(ns agglo.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET]]
            [agglo.views :refer [example-view]]))

(defn fetch-feeds []
  (let [feeds (r/atom [])]
    (GET "/feeds"
      {:handler (fn [response]
                  (js/console.log "Feeds fetched successfully" response)
                  (reset! feeds response))
       :error-handler (fn [error]
                        (js/console.error "Failed to fetch feeds" error))})
    feeds))

(defn fetch-blog-links []
  (let [blogs (r/atom [])]
    (GET "/blog-links"
      {:handler (fn [response]
                  (js/console.log "Blog links fetched successfully" response)
                  (reset! blogs (:rss-urls response)))
       :error-handler (fn [error]
                        (js/console.error "Failed to fetch blog links" error))})
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
           [:p description]])]
       [:div#sidebar
        [:h1 "Blog Links"]
        [:ul
         (for [blog @blogs]
           [:li [:a {:href blog} blog]])]]
       [example-view]])))

(defn init []
  (dom/render [home-page] (.getElementById js/document "app")))

(init)
