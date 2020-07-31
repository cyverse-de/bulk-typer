(ns bulk-typer.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clj-jargon.init :as init]
            [debug-utils.log-time :refer [log-time]]
            [common-cli.core :as ccli]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [bulk-typer.irods :as irods]
            [bulk-typer.config :as cfg]
            [service-logging.thread-context :as tc])
  (:import [java.util.concurrent Executors]))

(def ^:private svc-info
  {:app-name "bulk-typer"
   :group-id "org.cyverse"
   :art-id   "bulk-typer"
   :service  "bulk-typer"})

(def irods-pool (Executors/newFixedThreadPool 5))
(def icat-pool (Executors/newFixedThreadPool 5))

(defn- cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/bulk-typer.properties"]
   ["-f" "--file PATH" "Path to a file of paths to process"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

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

(defn- do-files
  [files]
  (init/with-jargon (mk-jargon-cfg) [cm]
    (let [;; create an agent, queue loading data, and queue getting the file type from that data
          agents (mapv (fn [f]
                         (as-> (agent f) a
                               (send-via irods-pool a (fn [f] [f (irods/get-data cm f)]))
                               (send a (fn [[f d]] [f (irods/get-file-type d f)]))
                               (send-via icat-pool a (fn [[f t]] [f t (irods/add-type-if-unset cm f t)])))) files)]
        ;; wait for agents to finish everything we've tasked them with before deref
      (log-time "await" (apply await agents))
      (mapv deref agents))))

(defn- do-file
  [file]
  (when (ft/exists? file)
    (let [files (remove string/blank? (string/split (slurp file) (re-pattern "\n")))]
      (do-files files))))

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 "The config file does not exist."))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (cfg/load-config-from-file (:config options))
      (mapv (fn [x] (log/info x))
            (log-time "do-file"
              (do-file (:file options))))
      (.shutdown irods-pool)
      (.shutdown icat-pool)
      (shutdown-agents))))
