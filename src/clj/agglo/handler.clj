(ns agglo.handler
  (:require [ring.util.response :as response]
            [agglo.feed :as feed]
            [clojure.java.io :as io]))

(defn home-page []
  (response/content-type
   (response/resource-response "public/index.html") "text/html"))

(defn feeds []
  (response/response (feed/fetch-feeds)))

(defn blog-links []
  (response/response (feed/load-config)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get home-page}]
     ["/feeds" {:get feeds}]
     ["/blog-links" {:get blog-links}]])))
