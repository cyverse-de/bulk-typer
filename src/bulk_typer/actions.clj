(ns bulk-typer.actions
  (:require [clojure.math.numeric-tower :as math]
            [clojure-commons.file-utils :as ft]
            [clj-jargon.init :as init]
            [clj-icat-direct.icat :as icat]
            [debug-utils.log-time :refer [log-time]]
            [clojure.string :as string]
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
  (log-time "do-files with-jargon"
    (init/with-jargon (mk-jargon-cfg) [cm]
      (let [;; create an agent, queue loading data, and queue getting the file type from that data
            agents (mapv (fn [f]
                           (as-> (agent f) a
                                 (send-via irods-pool a (fn [f] [f (irods/get-data cm f)]))
                                 (send a (fn [[f d]] [f (irods/get-file-type d f)]))
                                 (send-via icat-pool a (fn [[f t]] [f t (irods/add-type-if-unset cm f t)])))) files)]
        ;; wait for agents to finish everything we've tasked them with before deref
        (log-time "await" (apply await agents))
        (mapv deref agents)))))

(defn do-prefix
  [prefix]
  (log-time (str "do-prefix " prefix)
    (let [files (log-time "icat" (icat/prefixed-files-without-attr prefix "ipc-filetype"))]
      (do-files files))))

(defn do-file
  [file]
  (when (ft/exists? file)
    (let [files (remove string/blank? (string/split (slurp file) (re-pattern "\n")))]
      (do-files files))))

(defn do-all-prefixes
  []
  (log-time "do-all-prefixes"
  (let [prefixes (make-prefixes)]
    (doseq [prefix prefixes]
      (do-prefix prefix)))))

(defn shutdown
  []
  (.shutdown irods-pool)
  (.shutdown icat-pool))
