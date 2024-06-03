(ns agglo.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [agglo.feed :as feed])
  (:import (org.apache.log4j PropertyConfigurator)))

; Inicializar Log4j
(PropertyConfigurator/configure (io/resource "log4j.properties"))

(defn render-home-page [html-content feeds]
  (try
    (-> html-content
        (str/replace "{{feeds}}"
                     (apply str
                            (map (fn [{:keys [title link description]}]
                                   (let [description-text (if (map? description)
                                                            (:value description)
                                                            description)]
                                     (format "<div class='feed'>
                                               <h2><a href='%s'>%s</a></h2>
                                               <p>%s</p>
                                              </div>"
                                             link title (if (string? description-text)
                                                          (first (str/split description-text #"\n"))
                                                          description-text))))
                                 feeds))))
    (catch Exception e
      (log/error e "Error rendering home page")
      "Internal server error")))

(defn home-page-handler [request]
  (try
    (log/info "Serving home page")
    (let [html-file-path "resources/public/index.html"
          html-file (io/file html-file-path)
          feeds (feed/fetch-feeds)]
      (log/info "HTML file path:" html-file-path)
      (if (.exists html-file)
        {:status 200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (render-home-page (slurp html-file) feeds)}
        (do
          (log/error "HTML file not found at" html-file-path)
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Internal server error: HTML file not found"})})))
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
       :headers {"Content-Type" "application/json; charset=utf-8"}
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
