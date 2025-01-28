(ns agglo.handler
  (:require [ring.util.response :as response]
            [agglo.feed :as feed]
            [agglo.service :refer [render-home-page]]
            [clojure.java.io :as io]))

(defn home-page []
  (let [html-file (io/resource "public/index.html")
        feeds (feed/fetch-feeds)]  ;; Corrigido aqui
    (if html-file
      (-> (slurp html-file)
          (render-home-page feeds)
          (response/response)
          (response/content-type "text/html"))
      (response/status (response/response "HTML file not found") 500))))

(defn feeds []
  (response/response (feed/fetch-feeds)))  ;; Corrigido aqui

(defn blog-links []
  (response/response (feed/load-config)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get home-page}]
     ["/blog-links" {:get blog-links}]])))