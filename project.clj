(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/bulk-typer "0.1.1-SNAPSHOT"
  :description "A service that processes many files to add info-types"
  :url "https://github.com/cyverse-de/bulk-typer"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "bulk-typer-standalone.jar"
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [cheshire "6.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [com.fasterxml.jackson.core/jackson-core "2.20.0"]
                 [com.fasterxml.jackson.core/jackson-databind "2.20.0"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.20.0"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.20.0"]
                 [com.novemberain/langohr "5.6.0" :exclusions [org.slf4j/slf4j-api]]
                 [slingshot "0.12.2"]
                 [org.clojure/math.numeric-tower "0.1.0"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/clj-icat-direct "2.9.7"]
                 [org.cyverse/clojure-commons "3.0.11" :exclusions [commons-logging]]
                 [org.cyverse/service-logging "2.8.5"]
                 [org.cyverse/clj-jargon "3.1.4"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.cyverse/heuristomancer "2.8.7"]]
  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :cljfmt {:indents {log-time [[:inner 0]]}}
  :main ^:skip-aot bulk-typer.core
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "0.7.0"]
            [test2junit "1.4.4"]]
  :uberjar-exclusions [#"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/bulk-typer-logging.xml"])
