(ns agglo.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [agglo.feed :as feed]
            [ring.util.response :as response])
  (:import (org.apache.log4j PropertyConfigurator)))

; Inicializar Log4j
(PropertyConfigurator/configure (io/resource "log4j.properties"))

(defn load-html-file [resource-path]
  (let [resource (io/file resource-path)]
    (if (.exists resource)
      (slurp resource)
      (do
        (log/error "HTML file not found at" resource-path)
        nil))))

(defn replace-placeholder [html placeholder content]
  (clojure.string/replace html placeholder content))

(defn render-home-page [feeds]
  (let [html-content (load-html-file "resources/public/index.html")]
    (if html-content
      (replace-placeholder html-content
                           "{{feeds}}"
                           (apply str
                                  (map (fn [{:keys [title link description]}]
                                         (format "<div class='feed'>
                                                   <h2><a href='%s'>%s</a></h2>
                                                   <p>%s</p>
                                                  </div>"
                                                 link title (first (clojure.string/split description #"\n\n"))))
                                       feeds)))
      "HTML file not found")))

(defn home-page-handler [request]
  (try
    (log/info "Serving home page")
    (let [feeds (feed/fetch-feeds)
          rendered-content (render-home-page feeds)]
      (if (= rendered-content "HTML file not found")
        (-> (response/response rendered-content)
            (response/status 500)
            (response/content-type "application/json"))
        (-> (response/response rendered-content)
            (response/content-type "text/html; charset=utf-8"))))
    (catch Exception e
      (log/error e "Error serving home page" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:error "Internal server error"})})))

(defn feeds-handler [request]
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

(defn blog-links-handler [request]
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
   #{["/" :get home-page-handler :route-name :home]
     ["/feeds" :get feeds-handler :route-name :feeds]
     ["/blog-links" :get blog-links-handler :route-name :blog-links]}))

(def service
  {:env :prod
   ::http/routes routes
   ::http/resource-path "/public"
   ::http/type :jetty
   ::http/port 3000})
