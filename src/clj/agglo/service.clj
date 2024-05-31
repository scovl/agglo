(ns agglo.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [agglo.feed :as feed]
            [clojure.java.io :as io]))

(defn home-page [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "public/index.html"))})

(defn feeds [request]
  {:status 200
   :body (feed/fetch-feeds)})

(defn blog-links [request]
  {:status 200
   :body (feed/load-config)})

(def routes
  (route/expand-routes
   #{["/" :get home-page :route-name :home]
     ["/feeds" :get feeds :route-name :feeds]
     ["/blog-links" :get blog-links :route-name :blog-links]}))

(def service
  {:env :prod
   ::http/routes routes
   ::http/resource-path "/public"
   ::http/type :jetty
   ::http/port 3000})
