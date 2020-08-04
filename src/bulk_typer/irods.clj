(ns bulk-typer.irods
  (:use [clj-jargon.item-ops :only [input-stream]])
  (:require [heuristomancer.core :as hm]
            [debug-utils.log-time :refer [log-time]]
            [clojure.tools.logging :as log]
            [clj-jargon.metadata :as meta]
            [bulk-typer.config :as cfg]))

(defn get-data
  [cm path]
  (log-time (str "get-data " path)
    (hm/sip (input-stream cm path) (cfg/filetype-read-amount))))

(defn get-file-type
  "Uses heuristomancer to determine a the file type of a file."
  [data & info]
  (log/info "get-file-type data:" data " \\;")
  (log-time (str "get-file-type" info)
    (let [result (hm/identify-sample data)]
      (if (or (nil? result) (empty? (name result)))
        (cfg/unknown-type)
        (name result)))))

(defn add-type-if-unset
  [cm path ctype]
  (log-time (str "add-type-if-unset " path " " ctype)
    (when-not (log-time "attribute?" (meta/attribute? cm path (cfg/garnish-type-attribute)))
      (log-time "add-metadata" (meta/add-metadata cm path (cfg/garnish-type-attribute) ctype "ipc-bulk-typer")))))
