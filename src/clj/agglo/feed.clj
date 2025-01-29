(ns agglo.feed
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clj-http.client :as client]
            [buran.core :refer [consume consume-http shrink]]  ;; Uso correto do buran
            [clojure.walk :refer [postwalk]])
  (:import (org.jdom2 Element)))

;; Carrega as configurações do arquivo config.edn
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
          (log/error "Config file not found at" config-file-path)
          {})))
    (catch Exception e
      (log/error e "Error loading config" e)
      {})))

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

;; Busca um feed usando Buran e extrai os dados
(defn fetch-feed [url]
  (log/info "Fetching feed from URL:" url)
  (try
    (let [raw-feed (consume-http url)
          feed (shrink raw-feed)]

      ;; LOG para depuração
      (log/info "Raw feed data (before shrink):" raw-feed)
      (log/info "Feed data (after shrink):" feed)

      ;; Verifica se o feed tem título e entradas
      (let [info (get feed :info {})
            entries (or (get feed :entries) [])
            feed-title (or (:title info) "Untitled Feed")]

        {:title feed-title
         :entries (map (fn [entry]
        (let [description-text (or (get-in entry [:description :value]) ;; Atom
                                  (:description entry)                ;; RSS
                                  (get-in entry [:content :value])   ;; Atom (content HTML)
                                  (:summary entry)                    ;; Algumas variações de Atom
                                  (when (map? (:description entry))
                                    (pr-str (:description entry)))  ;; Se for um mapa, serializar
                                  "No description")]
                           {:title (or (:title entry) "No title")
                            :link (or (:link entry) "#")
                            :description description-text
                            :pubDate (or (:published-date entry) "No date")})) 
                       entries)}))

    (catch Exception e
      (log/error e "Error fetching feed from URL:" url)
      {:title "Error fetching feed"
       :entries []})))  ;; Retorna um feed vazio em caso de erro


;; Busca múltiplos feeds com base nas URLs do config.edn
(defn fetch-feeds []
  (let [config (load-config)
        urls (:rss-urls config)]
    (if (seq urls)
      (do
        (log/info "Fetching feeds for URLs:" urls)
        (map fetch-feed urls))
      (do
        (log/warn "No URLs found in config")
        [])))) ;; Retorna uma lista vazia caso não haja URLs

;; Função principal para iniciar o processamento dos feeds
(defn main []
  (log/info "Starting feed processing")
  (let [feeds (fetch-feeds)]
    (log/info "Feeds fetched:" feeds)
    feeds)) ;; Retorna os feeds para que possam ser usados em outra parte do sistema

(main)
