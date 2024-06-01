(ns agglo.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [agglo.feed :as feed])
  (:import (org.apache.log4j PropertyConfigurator)))

; Inicializar Log4j
(PropertyConfigurator/configure (io/resource "log4j.properties"))

(defn home-page [request]
  (try
    (log/info "Serving home page")
    (let [html-file (io/file "resources/public/index.html")
          feeds (feed/fetch-feeds)]
      (log/info "HTML file path:" html-file)
      (if (.exists html-file)
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (-> (slurp html-file)
                   (clojure.string/replace "{{feeds}}"
                                           (apply str
                                                  (map (fn [{:keys [title link description]}]
                                                         (format "<div class='feed'>
                                                                   <h2><a href='%s'>%s</a></h2>
                                                                   <p>%s</p>
                                                                  </div>"
                                                                 link title (subs description 0 (min 800 (count description)))))
                                                       feeds))))}
        (do
          (log/error "HTML file not found")
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Internal server error: HTML file not found"})})))
    (catch Exception e
      (log/error e "Error serving home page" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:error "Internal server error"})})))

(defn feeds [request]
  (log/info "Fetching feeds")
  (try
    (let [feeds-data (feed/fetch-feeds)]
      (log/info "Feeds fetched successfully" feeds-data)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string feeds-data)})
    (catch Exception e
      (log/error e "Error fetching feeds" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:error "Internal server error"})})))

(defn blog-links [request]
  (log/info "Fetching blog links")
  (try
    (let [config (feed/load-config)]
      (log/info "Blog links fetched successfully" config)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string (:rss-urls config))})
    (catch Exception e
      (log/error e "Error fetching blog links" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:error "Internal server error"})})))

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