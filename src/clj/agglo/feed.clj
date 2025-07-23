(ns agglo.feed
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [buran.core :as buran]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml])
  (:import (org.jdom2 Element)
           (java.io ByteArrayInputStream)
           (java.text SimpleDateFormat)
           (java.util Date Locale)))

;; Carrega as configurações do arquivo config.edn
(defn load-config []
  (try
    (let [config-file "resources/config.edn"]
      (log/info "Loading config from" config-file)
      (if (.exists (io/file config-file))
        (do
          (let [config (edn/read-string (slurp config-file))]
            (log/info "Config loaded successfully:" config)
            config))
        (do
          (log/warn "Config file not found, using default configuration")
          {:rss-urls ["https://hnrss.org/frontpage"]})))
    (catch Exception e
      (log/error e "Error loading config, using default configuration")
      {:rss-urls ["https://hnrss.org/frontpage"]})))

;; Remove elementos JDOM do feed processado
(defn remove-jdom-elements [data]
  (postwalk (fn [x]
              (cond
                (instance? Element x) nil
                (instance? java.util.Map x) (into {} (remove (fn [[k v]] (instance? Element v)) x))
                (instance? java.util.List x) (vec (remove #(instance? Element %) x))
                :else x))
            data))

;; Sanitiza os dados do feed para remover elementos indesejados
(defn sanitize-feed [feed]
  (-> feed
      remove-jdom-elements
      (update :entries
              (fn [entries]
                (map (fn [entry]
                       (-> entry
                           (dissoc :foreign-markup)
                           (update :description
                                   (fn [desc]
                                     (cond
                                       (string? desc) desc
                                       (map? desc) (get desc :value "No description")
                                       :else "No description")))))
                     (or entries [])))))) ;; Garante que entries nunca seja nil

;; Helper function to truncate text
(defn truncate-text [text max-length]
  (if (and text (> (count text) max-length))
    (str (subs text 0 max-length) "...")
    text))

;; Parse dates from different RSS formats
(defn parse-date [date-str]
  (try
    (when date-str
      (let [formats ["EEE, dd MMM yyyy HH:mm:ss Z"
                     "yyyy-MM-dd'T'HH:mm:ss'Z'"
                     "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                     "EEE MMM dd HH:mm:ss zzz yyyy"
                     "yyyy-MM-dd HH:mm:ss"
                     "dd MMM yyyy HH:mm:ss Z"]]
        (some (fn [fmt]
                (try
                  (let [sdf (SimpleDateFormat. fmt Locale/US)]
                    (.parse sdf date-str))
                  (catch Exception _ nil)))
              formats)))
    (catch Exception e
      (log/warn "Failed to parse date:" date-str e)
      nil)))

;; Sort posts by publication date (most recent first)
(defn sort-entries-by-date [entries]
  (->> entries
       (sort-by (fn [entry]
                  (if-let [date (parse-date (:pubDate entry))]
                    (- (.getTime date))
                    -1))
                >)))

(defn xml->map [xml-str]
  (try
    (-> xml-str
        .getBytes
        java.io.ByteArrayInputStream.
        xml/parse
        zip/xml-zip)
    (catch Exception e
      (log/error e "Failed to parse XML")
      nil)))

(defn extract-text [content]
  (if (string? content)
    content
    (str/join " " (map extract-text (:content content)))))

(defn parse-rss-item [item-loc]
  {:title (zip-xml/xml1-> item-loc :title zip-xml/text)
   :link (zip-xml/xml1-> item-loc :link zip-xml/text)
   :description (zip-xml/xml1-> item-loc :description zip-xml/text)
   :pubDate (zip-xml/xml1-> item-loc :pubDate zip-xml/text)
   :categories (zip-xml/xml-> item-loc :category zip-xml/text)})

(defn parse-rss [xml-str]
  (when-let [xml-zip (xml->map xml-str)]
    (let [channel (zip-xml/xml1-> xml-zip :channel)]
      {:title (zip-xml/xml1-> channel :title zip-xml/text)
       :entries (vec (map parse-rss-item
                         (zip-xml/xml-> channel :item)))})))

(defn fetch-url [url]
  (try
    (log/info "Fetching URL:" url)
    (let [response (http/get url {:as :string
                                 :socket-timeout 10000
                                 :connection-timeout 10000})]
      (log/info "Response status:" (:status response))
      (when (= 200 (:status response))
        (:body response)))
    (catch Exception e
      (log/error e "Failed to fetch URL:" url)
      nil)))

(defn parse-xml [xml-str]
  (try
    (log/info "Parsing XML string of length:" (count xml-str))
    (-> xml-str
        (.getBytes "UTF-8")
        ByteArrayInputStream.
        xml/parse)
    (catch Exception e
      (log/error e "Failed to parse XML")
      nil)))

(defn extract-feed-data [xml-data]
  (try
    (when xml-data
      (let [channel (first (filter #(= :channel (:tag %)) 
                                 (-> xml-data :content)))
            items (filter #(= :item (:tag %)) 
                         (:content channel))
            get-content (fn [item tag]
                         (some->> item
                                :content
                                (filter #(= tag (:tag %)))
                                first
                                :content
                                first))]
        
        (log/info "Found" (count items) "items in feed")
        
        {:title (get-content channel :title)
         :entries (vec (for [item items]
                        {:title (get-content item :title)
                         :link (get-content item :link)
                         :description (get-content item :description)
                         :pubDate (get-content item :pubDate)}))}))
    (catch Exception e
      (log/error e "Failed to extract feed data")
      {:title "Error parsing feed" :entries []})))

(defn fetch-feed [url]
  (log/info "Processing feed from URL:" url)
  (try
    (let [raw-feed (buran/consume-http url)
          feed     (-> raw-feed buran/shrink sanitize-feed)
          title    (get-in feed [:info :title])
          entries  (:entries feed)
          sorted-entries (sort-entries-by-date entries)]
      (log/info "Successfully extracted feed data:" feed)
      {:title (or title "")
       :entries (vec sorted-entries)})
    (catch Exception e
      (log/error e "Failed to fetch or parse feed" url)
      {:title "Error fetching feed" :entries []})))

(defn fetch-feeds []
  (let [config (load-config)
        urls (:rss-urls config)]
    (log/info "Fetching feeds from URLs:" urls)
    (vec (map fetch-feed urls))))

;; Função principal para iniciar o processamento dos feeds
(defn -main []
  (log/info "Starting feed processing")
  (let [feeds (fetch-feeds)]
    (log/info "Feeds fetched:" feeds)
    feeds)) ;; Retorna os feeds para que possam ser usados em outra parte do sistema

