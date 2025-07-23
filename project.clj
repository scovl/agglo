(defproject agglo "0.1.0-SNAPSHOT"
  :description "Blog aggregator"
  :url "https://example.com"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60"]
                 [reagent "1.2.0"]
                 [io.pedestal/pedestal.service "0.6.0" :exclusions [org.slf4j/slf4j-api]]
                 [io.pedestal/pedestal.jetty "0.6.0" :exclusions [org.slf4j/slf4j-api]]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [cljs-ajax "0.8.4"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/tools.logging "1.2.4"]
                 [log4j/log4j "1.2.17"]
                 [buran "0.1.4"]
                 [selmer "1.12.58"]
                 [ring/ring-core "1.9.6"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [ch.qos.logback/logback-classic "1.2.11"]
                 [org.slf4j/slf4j-api "1.7.36"]]
  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds [{:id "main"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"
                                   :asset-path "js/out"
                                   :optimizations :advanced
                                   :main agglo.core
                                   :source-map "resources/public/js/main.js.map"}}]}
  :main agglo.core)
