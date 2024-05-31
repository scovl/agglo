(ns agglo.core
  (:require [io.pedestal.http :as http]
            [agglo.service :refer [service]]))

(defn -main []
  (http/start (http/create-server service)))
