(ns bulk-typer.irods
  (:use [clj-jargon.item-ops :only [input-stream]])
  (:require [heuristomancer.core :as hm]
            [clojure.tools.logging :as log]
            [clj-jargon.metadata :as meta]
            [service-logging.thread-context :as tc]
            [bulk-typer.config :as cfg]))

(defn get-data
  "Fetches bulk-typer.config/filetype-read-amount bytes of the file at path"
  [cm path]
  (tc/with-logging-context {:path path}
    (hm/sip (input-stream cm path) (cfg/filetype-read-amount))))

(defn get-file-type
  "Uses heuristomancer to determine a the file type of a file."
  [data & info]
  (let [result (hm/identify-sample data)]
    (if (or (nil? result) (empty? (name result)))
      (cfg/unknown-type)
      (name result))))

(defn add-type-if-unset
  "Checks if path has an info-type set (in case it was added while we were
  calculating), then adds the provided type if not."
  [cm path ctype]
  (tc/with-logging-context {:path path :info-type ctype}
    (when-not (meta/attribute? cm path (cfg/garnish-type-attribute))
      (log/info "Adding type " ctype " to path " path)
      (meta/add-metadata cm path (cfg/garnish-type-attribute) ctype "ipc-bulk-typer"))))
