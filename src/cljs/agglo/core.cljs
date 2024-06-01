(ns agglo.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET]]
            [cljs.core.async :refer [chan put! <! go]]
            [agglo.views :refer [example-view]]))

;; Private function to handle the fetch-feeds response
(defn- handle-fetch-feeds-response [ch feeds response]
  (put! ch response)
  (js/console.log "Feeds fetched successfully" response))

;; Private function to handle fetch-feeds errors
(defn- handle-fetch-feeds-error [error]
  (js/console.error "Failed to fetch feeds" error))

;; Private function to handle the fetch-blog-links response
(defn- handle-fetch-blog-links-response [ch blogs response]
  (put! ch (:rss-urls response))
  (js/console.log "Blog links fetched successfully" response))

;; Private function to handle fetch-blog-links errors
(defn- handle-fetch-blog-links-error [error]
  (js/console.error "Failed to fetch blog links" error))

(defn fetch-feeds []
  (let [feeds (r/atom [])
        ch (chan)]
    (GET "/feeds"
      {:handler (partial handle-fetch-feeds-response ch feeds)
       :error-handler handle-fetch-feeds-error})
    (go (reset! feeds (<! ch)))
    feeds))

(defn fetch-blog-links []
  (let [blogs (r/atom [])
        ch (chan)]
    (GET "/blog-links"
      {:handler (partial handle-fetch-blog-links-response ch blogs)
       :error-handler handle-fetch-blog-links-error})
    (go (reset! blogs (<! ch)))
    blogs))

(defn home-page []
  (let [feeds (fetch-feeds)
        blogs (fetch-blog-links)]
    (fn []
      [:div#app
       [:div#content
        [:h1 "RSS Feeds"]
        (for [{:keys [title link description]} @feeds]
          [:div.feed
           [:h2 [:a {:href link} title]]
           [:p (first (clojure.string/split description #"\n"))]])] ; Show first paragraph
       [:div#sidebar
        [:h1 "Blog Links"]
        [:ul
         (for [blog @blogs]
           [:li [:a {:href blog} blog]])]]
       [example-view]])))

(defn init []
  (dom/render [home-page] (.getElementById js/document "app")))

(init)
