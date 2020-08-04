(ns bulk-typer.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(cc/defprop-optstr environment-name
  "The short name of the environment bulk-typer is running in. Used for defining the name of the queue it listens on."
  [props config-valid configs]
  "bulk-typer.environment-name" "docker-compose")

(cc/defprop-optstr garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs]
  "bulk-typer.type-attribute" "ipc-filetype")

(cc/defprop-optstr unknown-type
  "The value that goes in the value column when a type could not be determined."
  [props config-valid configs]
  "bulk-typer.unknown-type" "unknown")

(cc/defprop-optlong filetype-read-amount
  "The size, in bytes as a long, of the sample read from iRODS"
  [props config-valid configs]
  "bulk-typer.filetype-read-amount" 1024)

(cc/defprop-optstr irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs]
  "bulk-typer.irods.host" "irods")

(cc/defprop-optstr irods-port
  "Returns the iRODS port."
  [props config-valid configs]
  "bulk-typer.irods.port" "1247")

(cc/defprop-optstr irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs]
  "bulk-typer.irods.user" "rods")

(cc/defprop-optstr irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs]
  "bulk-typer.irods.pass" "notprod")

(cc/defprop-optstr irods-zone
  "Returns the iRODS zone."
  [props config-valid configs]
  "bulk-typer.irods.zone" "iplant")

(cc/defprop-optstr irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs]
  "bulk-typer.irods.home" "/iplant/home")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs]
  "bulk-typer.irods.resc" "")

(cc/defprop-optint irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs]
  "bulk-typer.irods.max-retries" 10)

(cc/defprop-optint irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs]
  "bulk-typer.irods.retry-sleep" 1000)

(cc/defprop-optboolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs]
  "bulk-typer.irods.use-trash" true)

(cc/defprop-optstr icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs]
  "bulk-typer.icat.host" "irods")

(cc/defprop-optint icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs]
  "bulk-typer.icat.port" "5432")

(cc/defprop-optstr icat-user
  "The user for the ICAT database."
  [props config-valid configs]
  "bulk-typer.icat.user" "rods")

(cc/defprop-optstr icat-password
  "The password for the ICAT database."
  [props config-valid configs]
  "bulk-typer.icat.password" "notprod")

(cc/defprop-optstr icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs]
  "bulk-typer.icat.db" "ICAT")

(defn- exception-filters
  []
  (remove nil? [(icat-password) (icat-user) (irods-pass) (irods-user)]))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user"])
  (validate-config)
  (ce/register-filters (exception-filters)))
