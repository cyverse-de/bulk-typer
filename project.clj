(defproject org.cyverse/bulk-typer "0.1.0-SNAPSHOT"
  :description "A service that processes many files to add info-types"
  :url "https://github.com/cyverse-de/bulk-typer"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :uberjar-name "bulk-typer-standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [com.novemberain/langohr "3.6.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/clj-icat-direct "2.8.7-SNAPSHOT"]
                 [org.cyverse/clojure-commons "2.8.0" :exclusions [commons-logging]]
                 [org.cyverse/clj-jargon "2.8.9"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.cyverse/heuristomancer "2.8.6"]]
  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :cljfmt {:indents {log-time [[:inner 0]]}}
  :main ^:skip-aot bulk-typer.core
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :plugins [[jonase/eastwood "0.2.3"]
            [test2junit "1.1.3"]]
  :uberjar-exclusions [#"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/bulk-typer-logging.xml"])
