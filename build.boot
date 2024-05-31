(set-env!
 :source-paths #{"src/clj" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.10.844"]
                 [reagent "1.1.0"]
                 [io.pedestal/pedestal.service "0.5.9"]
                 [io.pedestal/pedestal.jetty "0.5.9"]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [adzerk/boot-cljs "2.1.5"]
                 [adzerk/boot-reload "0.6.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.30"]
                 [cljs-ajax "0.8.4"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]])

(require '[boot.core :refer :all]
         '[boot.task.built-in :refer :all]
         '[adzerk.boot-cljs :refer [cljs]]
         '[io.pedestal.http :as http]
         '[agglo.service :refer [service]])

(deftask build-cljs
  "Build ClojureScript"
  []
  (comp
   (cljs :optimizations :advanced :source-map true)
   (target :dir #{"resources/public/js"})))

(deftask run
  "Run the web server"
  []
  (comp
   (build-cljs)
   (with-pass-thru _
     (future (http/start (http/create-server service)))
     (println "Server is running at http://localhost:3000")
     @(promise))))

(deftask build
  "Build the entire project"
  []
  (comp (build-cljs)
        (target)))
