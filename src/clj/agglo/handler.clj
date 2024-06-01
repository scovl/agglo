(ns agglo.handler
  (:require [ring.util.response :as response]
            [agglo.feed :as feed]
            [clojure.java.io :as io]))

(defn home-page []
  (let [html-file (io/resource "public/index.html")
        feeds (feed/fetch-feeds)]
    (if html-file
      (-> (slurp html-file)
          (str/replace "{{feeds}}"
                       (apply str
                              (map (fn [{:keys [title link description]}]
                                     (format "<div class='feed'>
                                               <h2><a href='%s'>%s</a></h2>
                                               <p>%s</p>
                                              </div>"
                                             link title (subs description 0 (min 00 (count description)))))
                                   feeds)))
          (response/response)
          (response/content-type "text/html"))
      (response/status (response/response "HTML file not found") 800))))

(defn feeds []
  (response/response (feed/fetch-feeds)))

(defn blog-links []
  (response/response (feed/load-config)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get home-page}]
     ["/blog-links" {:get blog-links}]])))
