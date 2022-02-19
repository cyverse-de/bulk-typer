(ns bulk-typer.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clj-icat-direct.icat :as icat]
            [common-cli.core :as ccli]
            [clojure.tools.logging :as log]
            [bulk-typer.actions :as actions]
            [bulk-typer.amqp :as amqp]
            [bulk-typer.config :as cfg]
            [service-logging.thread-context :as tc]))

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
   ["-p" "--prefix PREFIX" "UUID prefix to process"]
   ["-x" "--full" "Process the whole data store once, then exit"]
   ["-y" "--periodic" "Listen for amqp messages, index.all or index.info-types, and run a full reindex when one is received."]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn- amqp-config
  []
  {:uri                   (cfg/amqp-uri)
   :exchange              (cfg/amqp-exchange)
   :exchange-type         (cfg/amqp-exchange-type)
   :exchange-durable?     (cfg/amqp-exchange-durable?)
   :exchange-auto-delete? (cfg/amqp-exchange-auto-delete?)
   :queue-name            (str "bulk-typer." (cfg/environment-name))
   :queue-durable?        true
   :queue-exclusive?      false
   :queue-auto-delete?    false
   :qos                   (cfg/amqp-qos)})

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 "The config file does not exist."))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (cfg/load-config-from-file (:config options))
      (when (:file options)
        (tc/with-logging-context {:mode "file" :file (:file options)}
          (actions/do-file (:file options))))
      (icat/setup-icat
        (assoc (icat/icat-db-spec (cfg/icat-host) (cfg/icat-user) (cfg/icat-password) :port (cfg/icat-port) :db (cfg/icat-db))
               :test-connection-on-checkout true))
      (when (:prefix options)
        (tc/with-logging-context {:mode "prefix"}
          (actions/do-prefix (:prefix options))))
      (when (:full options)
        (actions/do-all-prefixes))
      (if (:periodic options)
        (tc/with-logging-context {:mode "periodic"}
          (try
            (amqp/configure (partial amqp/handler actions/do-all-prefixes) (amqp-config) ["index.all" "index.info-types"])
            (catch Exception e
              (log/error "setting up AMQP" e))))
        (do
          (actions/shutdown)
          (shutdown-agents))))))
