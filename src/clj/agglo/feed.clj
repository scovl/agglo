(ns agglo.feed
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zf]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config []
  (edn/read-string (slurp (io/resource "config.edn"))))

(defn fetch-feed [url]
  (let [response (client/get url)
        body (:body response)]
    (-> body
        (xml/parse)
        (zip/xml-zip))))

(defn parse-feed [feed]
  (let [entries (zf/xml1-> feed :channel :item)]
    (map (fn [entry]
           {:title (zf/xml1-> entry :title zf/text)
            :link  (zf/xml1-> entry :link zf/text)
            :description (zf/xml1-> entry :description zf/text)})
         entries)))

(defn fetch-feeds []
  (let [config (load-config)
        feed-urls (:rss-urls config)
        feeds (map fetch-feed feed-urls)]
    (flatten (map parse-feed feeds))))
