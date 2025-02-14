(ns agglo.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [cognitect.transit :as transit]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [agglo.feed :as feed]
            [selmer.parser :as selmer])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(let [logs-dir (io/file "logs")]
  (when-not (.exists logs-dir)
    (.mkdirs logs-dir)))

(defn write-transit [data]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out)))

(defn read-transit [data]
  (let [in (ByteArrayInputStream. (.getBytes data))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn render-home-page [html-content feeds]
  (try
    (log/info "Rendering home page with feeds:" feeds)
    (let [updated-content
          (str/replace html-content
             "{{feeds}}"
             (apply str
                    (map (fn [{:keys [title link description]}]
                           (let [description-text (if (map? description)
                                                    (:value description)
                                                    description)]
                             (format "<div class='feed'>
                                        <h2><a href='%s'>%s</a></h2>
                                        <div>%s</div>
                                      </div>"
                                     (or link "#") (or title "No title") (or description-text "No description"))))
                         feeds)))]
      updated-content)  ;; Retorna o conteúdo modificado dentro do `try`

    (catch Exception e
      (log/error e "Error rendering home page")
      "Internal server error")))

(defn render-feed [_]
  (log/info "Starting render-feed handler")
  (try
    (log/info "Fetching feeds...")
    (let [feeds (feed/fetch-feeds)]
      (log/info "Feeds fetched:" feeds)
      (log/info "Loading template...")
      (let [html-file-path "resources/public/index.html"
            html-file (io/file html-file-path)]
        (log/info "HTML file path:" html-file-path)
        (if (.exists html-file)
          (do
            (log/info "HTML file found, loading content")
            (let [content (slurp html-file)
                  rendered (selmer/render content {:feeds feeds})]
              (log/info "Template rendered successfully")
              {:status 200
               :headers {"Content-Type" "text/html; charset=utf-8"}
               :body rendered}))
          (do
            (log/error "HTML file not found at" html-file-path)
            {:status 500
             :headers {"Content-Type" "text/html"}
             :body "Template file not found"}))))
    (catch Exception e
      (log/error e "Error in render-feed:")
      {:status 500
       :headers {"Content-Type" "text/html"}
       :body (str "Internal server error: " (.getMessage e))})))

(defn feeds-handler [request]
  (log/info "Fetching feeds")
  (try
    (let [feeds-data (feed/fetch-feeds)]
      (log/info "Feeds fetched successfully" feeds-data)
      {:status 200
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (write-transit feeds-data)})
    (catch Exception e
      (log/error e "Error fetching feeds" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (write-transit {:error "Internal server error"})})))

(defn blog-links-handler [request]
  (log/info "Fetching blog links")
  (try
    (let [config (feed/load-config)]
      (log/info "Blog links fetched successfully" config)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (write-transit (:rss-urls config))})
    (catch Exception e
      (log/error e "Error fetching blog links" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (write-transit {:error "Internal server error"})})))

(def routes
  #{["/" :get render-feed :route-name :index]
    ["/feeds" :get feeds-handler :route-name :feeds]
    ["/blog-links" :get blog-links-handler :route-name :blog-links]})

(def service
  {::http/routes routes
   ::http/type :jetty
   ::http/port 8080})

(defn home-page-handler [request]
  (try
    (log/info "Serving home page")
    (let [html-file-path "resources/public/index.html"
          html-file (io/file html-file-path)
          feeds (feed/fetch-feeds)]  ;; Corrigido aqui
      (log/info "HTML file path:" html-file-path)
      (log/info "Feeds data:" feeds)
      (if (.exists html-file)
        (do
          (log/info "HTML file found, loading content")
          (let [content (slurp html-file)]
            (log/info "HTML file content loaded successfully")
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body (render-home-page content feeds)}))
        (do
          (log/error "HTML file not found at" html-file-path)
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (write-transit {:error "Internal server error: HTML file not found"})})))
    (catch Exception e
      (log/error e "Error serving home page" e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (write-transit {:error "Internal server error"})})))

;; Entry point
(defn -main []
  (http/start (http/create-server service)))