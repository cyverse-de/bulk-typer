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
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.12.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [me.raynes/fs "1.4.6"]
                 [com.novemberain/langohr "3.6.1"]
                 [slingshot "0.12.2"]
                 [org.clojure/math.numeric-tower "0.1.0"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/clj-icat-direct "2.9.4"]
                 [org.cyverse/clojure-commons "3.0.7" :exclusions [commons-logging]]
                 [org.cyverse/service-logging "2.8.3" :exclusions [ch.qos.logback/logback-classic]]
                 [org.cyverse/otel "0.2.5"]
                 [net.logstash.logback/logstash-logback-encoder "7.4"]
                 [org.cyverse/clj-jargon "3.1.0"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.cyverse/heuristomancer "2.8.6"]]
  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :cljfmt {:indents {log-time [[:inner 0]]}}
  :main ^:skip-aot bulk-typer.core
  :profiles {:dev     {:resource-paths ["conf/test"]
                       :jvm-opts       ["-Dotel.javaagent.enabled=false"]}
             :uberjar {:aot :all}}
  :plugins [[jonase/eastwood "1.4.2"]
            [lein-ancient "0.7.0"]
            [test2junit "1.1.3"]]
  :uberjar-exclusions [#"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/bulk-typer-logging.xml" "-javaagent:./opentelemetry-javaagent.jar" "-Dotel.resource.attributes=service.name=bulk-typer"])
