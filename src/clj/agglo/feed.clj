(ns agglo.feed
  (:import [com.sun.syndication.io SyndFeedInput XmlReader]
           [java.net URL]
           [java.io ByteArrayInputStream])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clj-http.client :as client]))

(defrecord feed [authors author categories contributors copyright description
                 encoding entries feed-type image language link entry-links
                 published-date title uri])

(defrecord entry [authors author categories contents contributors description
                  enclosures link published-date title updated-date url])

(defrecord enclosure [length type uri])
(defrecord person    [email name uri])
(defrecord category  [name taxonomy-uri])
(defrecord content   [type value])
(defrecord image     [description link title url])
(defrecord link      [href hreflang length rel title type])

(defn- obj->enclosure [e]
  (map->enclosure {:length (.getLength e)
                   :type   (.getType e)
                   :url    (.getUrl e)}))

(defn- obj->content [c]
  (map->content {:type  (.getType c)
                 :value (.getValue c)}))

(defn- obj->link [l]
  (map->link {:href     (.getHref l)
              :hreflang (.getHreflang l)
              :length   (.getLength l)
              :rel      (.getRel l)
              :title    (.getTitle l)
              :type     (.getType l)}))

(defn- obj->category [c]
  (map->category {:name         (.getName c)
                  :taxonomy-uri (.getTaxonomyUri c)}))

(defn- obj->person [sp]
  (map->person {:email (.getEmail sp)
                :name  (.getName sp)
                :uri   (.getUri sp)}))

(defn- obj->image [i]
  (map->image {:description (.getDescription i)
               :link        (.getLink i)
               :title       (.getTitle i)
               :url         (.getUrl i)}))

(defn- obj->entry [e]
  (map->entry {:authors        (map obj->person    (seq (.getAuthors e)))
               :categories     (map obj->category  (seq (.getCategories e)))
               :contents       (map obj->content   (seq (.getContents e)))
               :contributors   (map obj->person    (seq (.getContributors e)))
               :enclosures     (map obj->enclosure (seq (.getEnclosures e)))
               :description    (or (if-let [d (.getDescription e)] (obj->content d))
                                   (first (map obj->content (seq (.getContents e)))))
               :author         (.getAuthor e)
               :link           (.getLink e)
               :published-date (.getPublishedDate e)
               :title          (.getTitle e)
               :updated-date   (.getUpdatedDate e)
               :uri            (.getUri e)}))

(defn- obj->feed [f]
  (map->feed  {:authors        (map obj->person   (seq (.getAuthors f)))
               :categories     (map obj->category (seq (.getCategories f)))
               :contributors   (map obj->person   (seq (.getContributors f)))
               :entries        (map obj->entry    (seq (.getEntries f)))
               :entry-links    (map obj->link     (seq (.getLinks f)))
               :image          (if-let [i (.getImage f)] (obj->image i))
               :author         (.getAuthor f)
               :copyright      (.getCopyright f)
               :description    (.getDescription f)
               :encoding       (.getEncoding f)
               :feed-type      (.getFeedType f)
               :language       (.getLanguage f)
               :link           (.getLink f)
               :published-date (.getPublishedDate f)
               :title          (.getTitle f)
               :uri            (.getUri f)}))

(defn- parse-internal [xmlreader]
  (let [feedinput (SyndFeedInput.)
        syndfeed  (.build feedinput xmlreader)]
    (obj->feed syndfeed)))

(defn ->url [s]
  (if (string? s) (URL. s) s))

(defn parse-feed
  ([feedsource]
   (parse-internal (XmlReader. (->url feedsource))))
  ([feedsource content-type]
   (parse-internal (XmlReader. (->url feedsource) content-type)))
  ([feedsource content-type lenient]
   (parse-internal (XmlReader. (->url feedsource) content-type lenient)))
  ([feedsource content-type lenient default-encoding]
   (parse-internal (XmlReader. (->url feedsource) content-type lenient default-encoding))))

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
        (parse-internal (XmlReader. stream))))
    (catch Exception e
      (log/error e "Error fetching feed from URL" url e)
      nil)))

(defn fetch-feeds []
  (try
    (let [config (load-config)
          feed-urls (:rss-urls config)]
      (log/info "Feed URLs to fetch:" feed-urls)
      (let [feeds (map fetch-feed feed-urls)]
        (log/info "Feeds fetched, now parsing")
        (let [parsed-feeds (filter some? (mapcat #(take 1 (:entries %)) feeds))]
          (log/info "Parsed feeds:" parsed-feeds)
          parsed-feeds)))
    (catch Exception e
      (log/error e "Error fetching or parsing feeds")
      [])))
