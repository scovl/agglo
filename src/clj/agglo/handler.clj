(ns agglo.handler
  (:require [ring.util.response :as response]
            [agglo.feed :as feed]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

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

(defn home-page []
  (let [html-file-path "resources/public/index.html"
        html-file (io/file html-file-path)
        feeds (feed/fetch-feeds)]
    (if (.exists html-file)
      (-> (render-home-page (slurp html-file) feeds)
          (response/response)
          (response/content-type "text/html; charset=utf-8"))
      (do
        (log/error "HTML file not found at" html-file-path)
        (response/status (response/response "HTML file not found") 500)))))

(defn feeds []
  (response/response (feed/fetch-feeds)))

(defn blog-links []
  (response/response (feed/load-config)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get home-page}]
     ["/blog-links" {:get blog-links}]])))
