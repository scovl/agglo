(ns agglo.feed
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clj-http.client :as client]
            [buran.core :refer [consume shrink]]  ;; Uso correto do buran
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
      channel-title (:title (:channel (:rss feed))) ;; Obtendo o título do canal corretamente
      entries (:item (:channel (:rss feed)))]
  (log/info "Channel title:" channel-title)
  (log/info "Parsed feed entries:" entries)
  {:title (or channel-title "Untitled Feed")
   :entries (map (fn [entry]
                   {:title (or (:title entry) "No title")
                    :link (or (:link entry) "#")
                    :description (or (:description entry) "No description")
                    :pubDate (or (:pubDate entry) "No date")
                    :guid (or (:guid entry) "No GUID")
                    :categories (mapv #(get % :content) (:category entry))})
                 entries)})
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