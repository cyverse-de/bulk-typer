(ns bulk-typer.actions
  (:require [slingshot.slingshot :refer [try+]]
            [clojure.math.numeric-tower :as math]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [clj-jargon.init :as init]
            [clj-icat-direct.icat :as icat]
            [clojure.string :as string]
            [service-logging.thread-context :as tc]
            [bulk-typer.irods :as irods]
            [bulk-typer.config :as cfg])
  (:import [java.util.concurrent Executors]))

(def irods-pool (Executors/newFixedThreadPool 5))
(def icat-pool (Executors/newFixedThreadPool 5))

(defn- mk-jargon-cfg
  []
  (init/init (cfg/irods-host)
             (cfg/irods-port)
             (cfg/irods-user)
             (cfg/irods-pass)
             (cfg/irods-home)
             (cfg/irods-zone)
             (cfg/irods-resc)
             :max-retries (cfg/irods-max-retries)
             :retry-sleep (cfg/irods-retry-sleep)
             :use-trash   (cfg/irods-use-trash)))

(defn- make-prefixes
  ([]
   (make-prefixes (cfg/base-prefix-length)))
  ([length]
   (let [format-str (format "%%0%dx" length)]
   (map (fn [x] (format format-str x)) (shuffle (range 0 (math/expt 16 length)))))))

(defn- do-or-error
  "Takes an action, agent state, and any other args. If it throws an error,
  returns an object with the error at a known key (which should continue as
  agent state). If passed an error, simply returns it without doing further
  computation."
  [action agent-state & args]
  (tc/with-logging-context (select-keys agent-state [:filename :type :set-result])
    (if (contains? agent-state ::error)
      agent-state
      (try+
        (apply action agent-state args)
        (catch Object o
          (log/error o)
          (assoc agent-state ::error o))))))

(defn- do-files
  [files]
  (init/with-jargon (mk-jargon-cfg) [cm]
    (let [;; create an agent, queue loading data, and queue getting the file type from that data
          agents (mapv (fn [f]
                         (as-> (agent {:filename f}) a
                               (send-via irods-pool a
                                         (partial do-or-error (fn [d] (assoc d :data (irods/get-data cm (:filename d))))))
                               (send     a
                                         (partial do-or-error (fn [d] (assoc d :type (irods/get-file-type (:data d) (:filename d))))))
                               (send-via icat-pool a
                                         (partial do-or-error (fn [d] (assoc d :set-result (irods/add-type-if-unset cm (:filename d) (:type d))))))))
                       files)]
      ;; wait for agents to finish everything we've tasked them with before deref, hopefully
      (apply await-for 300000 agents) ;; arbitrary timeout of 5 minutes, just for safety really
      (mapv deref agents))))

(defn do-prefix
  [prefix]
  (tc/with-logging-context {:prefix prefix}
    (log/info "Processing prefix " prefix)
    (let [files (distinct (icat/prefixed-files-without-attr prefix "ipc-filetype"))]
      (do-files files))
    (log/info "Done processing prefix " prefix)))

(defn do-file
  [file]
  (when (ft/exists? file)
    (let [files (remove string/blank? (string/split (slurp file) (re-pattern "\n")))]
      (do-files files))))

(defn do-all-prefixes
  []
  (let [prefixes (make-prefixes)]
    (doseq [prefix prefixes]
      (do-prefix prefix))))

(defn shutdown
  []
  (.shutdown irods-pool)
  (.shutdown icat-pool))
