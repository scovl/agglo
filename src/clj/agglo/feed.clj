(ns agglo.feed
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clj-http.client :as client]
            [buran.core :refer [consume shrink]]
            [clojure.walk :refer [postwalk]])
  (:import (org.jdom2 Element)))

(defn load-config []
  "Loads the configuration from resources/config.edn."
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
          (log/error "Config file not found at" config-file-path)
          {})))
    (catch Exception e
      (log/error e "Error loading config" e)
      {})))

(defn remove-jdom-elements [data]
  "Remove JDOM elements from the data recursively."
  (postwalk (fn [x]
              (cond
                (instance? Element x) nil
                (instance? java.util.Map x) (into {} (remove (fn [[k v]] (instance? Element v)) x))
                (instance? java.util.List x) (vec (remove #(instance? Element %) x))
                :else x))
            data))

(defn sanitize-feed [feed]
  "Sanitizes the feed data by removing JDOM elements and foreign markup."
  (-> feed
      remove-jdom-elements
      (update :entries
              (fn [entries]
                (map (fn [entry]
                       (-> entry
                           (dissoc :foreign-markup)
                           (update :description
                                   (fn [desc]
                                     (if (map? desc)
                                       (get desc :value desc)
                                       desc)))))
                     entries)))))

(defn fetch-feed [url]
  "Fetches a feed from the given URL and returns it as a Clojure data structure."
  (log/info "Fetching feed from URL:" url)
  (try
    (let [response (client/get url {:as :string})
          body (:body response)]
      (log/info "Fetched feed body from URL:" url "\n" body)
      (let [feed (shrink (consume body))
            channel-title (get-in feed [:rss :channel :title 0])
            entries (get-in feed [:rss :channel :item])]
        (log/info "Channel title:" channel-title)
        (log/info "Parsed feed entries:" entries)
        {:title channel-title
         :entries (map (fn [entry]
                         {:title (get-in entry ["title" 0])
                          :link (get-in entry ["link" 0])
                          :description (get-in entry ["description" 0])
                          :pubDate (get-in entry ["pubDate" 0])
                          :guid (get-in entry ["guid" 0])
                          :categories (mapv #(get % :content) (get entry "category"))})
                       entries)}))
    (catch Exception e
      (log/error e "Error fetching feed from URL:" url)
      nil)))

(defn fetch-feeds []
  "Fetches multiple feeds based on URLs from the config file."
  (let [config (load-config)
        urls (:rss-urls config)
        feeds (map fetch-feed urls)]
    (log/info "Fetched feeds data:" feeds)
    feeds))

(defn main []
  (log/info "Starting feed processing")
  (let [feeds (fetch-feeds)]
    (log/info "Feeds fetched:" feeds)
    ;; Aqui você pode adicionar lógica para manipular os feeds como necessário
    ))

(main)