(ns bulk-typer.irods
  (:use [clj-jargon.item-ops :only [input-stream]])
  (:require [heuristomancer.core :as hm]
            [debug-utils.log-time :refer [log-time]]
            [clojure.tools.logging :as log]
            [bulk-typer.config :as cfg]))

(defn get-data
  [cm path]
  (log-time (str "get-data " path)
            (hm/sip (input-stream cm path) (cfg/filetype-read-amount))))

(defn get-file-type
  "Uses heuristomancer to determine a the file type of a file."
  [data & info]
  (log-time (str "get-file-type" info)
            (let [result (hm/identify-sample data)]
              (if (or (nil? result) (empty? (name result)))
                ""
                (name result)))))

(defn content-type
  "Determines the filetype of path. Reads in a chunk, passes it to heuristomancer"
  [cm path]
  (log-time "content-type"
            (get-file-type (get-data cm path))))
