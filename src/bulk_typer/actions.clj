(ns bulk-typer.actions
  (:require [clojure.math.numeric-tower :as math]
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

(defn- do-files
  [files]
  (init/with-jargon (mk-jargon-cfg) [cm]
    (let [;; create an agent, queue loading data, and queue getting the file type from that data
          agents (mapv (fn [f]
                         (as-> (agent f) a
                               (send-via irods-pool a (fn [f] [f (irods/get-data cm f)]))
                               (send a (fn [[f d]] [f (irods/get-file-type d f)]))
                               (send-via icat-pool a (fn [[f t]] [f t (irods/add-type-if-unset cm f t)])))) files)]
      ;; wait for agents to finish everything we've tasked them with before deref, hopefully
      (apply await-for 120000 agents) ;; somewhat arbitrary timeout
      (mapv deref (remove #(agent-error %) agents)))))

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
