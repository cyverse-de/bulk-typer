(ns bulk-typer.irods
  (:use [clj-jargon.item-ops :only [input-stream]])
  (:require [heuristomancer.core :as hm]
            [clj-jargon.metadata :as meta]
            [bulk-typer.config :as cfg]))

(defn get-data
  [cm path]
  (hm/sip (input-stream cm path) (cfg/filetype-read-amount)))

(defn get-file-type
  "Uses heuristomancer to determine a the file type of a file."
  [data & info]
  (let [result (hm/identify-sample data)]
    (if (or (nil? result) (empty? (name result)))
      (cfg/unknown-type)
      (name result))))

(defn add-type-if-unset
  [cm path ctype]
    (when-not (meta/attribute? cm path (cfg/garnish-type-attribute))
      (meta/add-metadata cm path (cfg/garnish-type-attribute) ctype "ipc-bulk-typer")))
