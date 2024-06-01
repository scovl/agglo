(ns agglo.handler
  (:require [ring.util.response :as response]
            [agglo.feed :as feed]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn load-html-file [resource-path]
  (let [resource (io/resource resource-path)]
    (if resource
      (slurp resource)
      (do
        (println "HTML file not found at" resource-path)
        nil))))

(defn replace-placeholder [html placeholder content]
  (str/replace html placeholder content))

(defn render-home-page [feeds]
  (let [html-content (load-html-file "public/index.html")]
    (if html-content
      (replace-placeholder html-content
                           "{{feeds}}"
                           (apply str
                                  (map (fn [{:keys [title link description]}]
                                         (format "<div class='feed'>
                                                   <h2><a href='%s'>%s</a></h2>
                                                   <p>%s</p>
                                                  </div>"
                                                 link title (first (str/split description #"\n\n"))))
                                       feeds)))
      "HTML file not found")))

(defn home-page-handler [request]
  (let [feeds (feed/fetch-feeds)
        rendered-content (render-home-page feeds)]
    (if (= rendered-content "HTML file not found")
      (-> (response/response rendered-content)
          (response/status 500)
          (response/content-type "application/json"))
      (-> (response/response rendered-content)
          (response/content-type "text/html; charset=utf-8")))))

(defn feeds-handler [request]
  (response/response (feed/fetch-feeds)))

(defn blog-links-handler [request]
  (response/response (feed/load-config)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get home-page-handler}]
     ["/blog-links" {:get blog-links-handler}]])))
