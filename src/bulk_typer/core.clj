(ns bulk-typer.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clj-jargon.init :as init]
            [common-cli.core :as ccli]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [bulk-typer.irods :as irods]
            [bulk-typer.config :as cfg]
            [service-logging.thread-context :as tc]
    ))

(def ^:private svc-info
  {:app-name "bulk-typer"
   :group-id "org.cyverse"
   :art-id   "bulk-typer"
   :service  "bulk-typer"})

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

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 "The config file does not exist."))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (cfg/load-config-from-file (:config options))
      (when (ft/exists? (:file options))
        (let [files (remove string/blank? (string/split (slurp (:file options)) (re-pattern "\n")))]
          (log/warn files)
        ))
      )))
