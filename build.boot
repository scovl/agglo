(set-env!
 :source-paths #{"src/clj" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60"]
                 [reagent "1.2.0"]
                 [io.pedestal/pedestal.service "0.6.0" :exclusions [org.slf4j/slf4j-api]]
                 [io.pedestal/pedestal.jetty "0.6.0" :exclusions [org.slf4j/slf4j-api]]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [adzerk/boot-cljs "2.1.5"]
                 [adzerk/boot-reload "0.6.0"]
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
                 [org.slf4j/slf4j-api "1.7.36"]])

(require '[boot.core :refer :all]
         '[boot.task.built-in :refer :all]
         '[adzerk.boot-cljs :refer [cljs]]
         '[io.pedestal.http :as http]
         '[agglo.service :refer [service]]
         '[boot.task.built-in :refer [target]]
         '[clojure.tools.analyzer.utils :as analyzer-utils])

(deftask clean
  "Clean the target directories"
  []
  (with-pass-thru fs
    (doseq [dir ["target"]]
      (let [dir-file (java.io.File. dir)]
        (when (.exists dir-file)
          (doseq [file (.listFiles dir-file)]
            (.delete file))
          (.delete dir-file))))))

(deftask copy-resources
  "Copy static resources to target"
  []
  (comp
   (sift :include #{#"public/css/.*" #"public/index.html"})
   (target :dir #{"target/resources"})))

(deftask build-cljs
  "Build ClojureScript"
  []
  (comp
   (cljs :main 'agglo.core
         :output-to "target/resources/public/js/main.js"
         :output-dir "target/resources/public/js/out"
         :asset-path "js/out"
         :optimizations :advanced
         :source-map true)))

(deftask run
  "Run the web server"
  []
  (comp
   (clean)
   (copy-resources)
   (build-cljs)
   (with-pass-thru _
     (http/start (http/create-server service))
     (println "Server is running at http://localhost:3000")
     (Thread/sleep Long/MAX_VALUE))))  ;; Mantém o servidor em execução

(deftask build
  "Build the project."
  []
  (comp
   (cljs :optimizations :advanced
         :output-dir "target/public/js"
         :output-to "target/public/js/main.js"
         :asset-path "/js"
         :main 'agglo.core)
   (target :dir #{"target/public"})))