(ns agglo.feed
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zf]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayInputStream)))

(defn load-config []
  (try
    (let [config-file-path "resources/config.edn"
          config-file (io/file config-file-path)]
      (if (.exists config-file)
        (do
          (log/info "Loading config from" config-file-path)
          (let [config (edn/read-string (slurp config-file))]
            (log/info "Config loaded:" config)
            config))
        (do
          (log/error "Config file not found")
          {})))
    (catch Exception e
      (log/error e "Error loading config")
      {})))

(defn fetch-feed [url]
  (log/info "Fetching feed from URL:" url)
  (try
    (let [response (client/get url {:as :string :socket-timeout 5000 :conn-timeout 5000})
          body (:body response)]
      (log/info "Feed fetched successfully from" url)
      (with-open [stream (ByteArrayInputStream. (.getBytes body "UTF-8"))]
        (-> stream
            xml/parse
            zip/xml-zip)))
    (catch Exception e
      (log/error e "Error fetching feed from URL" url e)
      nil)))

(defn safe-get [ctx path-fn default]
  (try
    (path-fn ctx)
    (catch IllegalArgumentException _ default)))

(defn parse-feed [feed]
  (try
    (let [first-entry (zf/xml1-> feed :channel :item)]
      (log/info "Parsing first feed entry")
      (when first-entry
        (let [title (safe-get first-entry #(zf/xml1-> % :title zf/text) "")
              link (safe-get first-entry #(zf/xml1-> % :link zf/text) "")
              description (safe-get first-entry #(zf/xml1-> % :description zf/text) "")]
          {:title title
           :link link
           :description description})))
    (catch Exception e
      (log/error e "Error parsing feed entry" e)
      nil)))

(defn fetch-feeds []
  (try
    (let [config (load-config)
          feed-urls (:rss-urls config)]
      (log/info "Feed URLs to fetch:" feed-urls)
      (let [feeds (map fetch-feed feed-urls)]
        (log/info "Feeds fetched, now parsing")
        (let [parsed-feeds (filter some? (map parse-feed feeds))]
          (log/info "Parsed feeds:" parsed-feeds)
          parsed-feeds)))
    (catch Exception e
      (log/error e "Error fetching or parsing feeds")
      [])))
