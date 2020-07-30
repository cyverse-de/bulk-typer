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

(defn do-files
  [file]
  (when (ft/exists? file)
    (init/with-jargon (mk-jargon-cfg) [cm]
      (let [files (remove string/blank? (string/split (slurp file) (re-pattern "\n")))
            agents (mapv agent files)
            agents (mapv (fn [a] (send-via irods-pool a (fn [f] [f (irods/get-data cm f)]))) agents)
            agents (mapv (fn [a] (send-off a (fn [[f d]] [f (irods/get-file-type d f)]))) agents)]
        (log-time "await" (apply await agents))
        (mapv deref agents)))))

(defn do-files*
  [file]
  (when (ft/exists? file)
    (mapv (fn [x] (log/info x))
          (mapv deref
                (let [files (remove string/blank? (string/split (slurp file) (re-pattern "\n")))]
                  (log-time "with-jargon"
                            (init/with-jargon (mk-jargon-cfg) [cm]
                              (doall (pmap
                                      (fn [f] (let [d (irods/get-data cm f)] (future [f (irods/get-file-type d f)])))
                                      files)))))))))

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
            (log-time "do-files"
              (do-files (:file options))))
      (.shutdown irods-pool)
      (shutdown-agents))))
