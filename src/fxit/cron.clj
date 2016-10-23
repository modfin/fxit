(ns fxit.cron
  (:require [fxit.db :as db])

  (:require [clojure.xml :as x])
  (:require [cronj.core :as cj])
  (:require [org.httpkit.client :as client])
  (:require [clojure.data.json :as json])
  (:import (java.io ByteArrayInputStream)))

(defn ^:private parse-xml [s]
  (x/parse
    (ByteArrayInputStream. (.getBytes s))))


(defn ^:private grab-ecb-data [t opts]

  (def url "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml")

  (client/get url {:timeout 1000 :user-agent "User-Agent-string"}
              (fn [{:keys [status headers body error]}]
                (println "Grabbing")
                (if error
                  (println "Failed, exception is " error)
                  (do
                    (def xml (parse-xml body))
                    (def base "EUR")
                    (def date (get-in xml [:content 2 :content 0 :attrs :time]))
                    (def rates (reduce
                                 (fn [acc rate]
                                   (assoc acc (get-in rate [:attrs :currency])
                                              (read-string (get-in rate [:attrs :rate]))))
                                 {}
                                 (get-in xml [:content 2 :content 0 :content])))

                    (def latest {"base" base "date" date "rates" rates})

                    (db/upsert-rate! {:date date :content (json/write-str latest)})
                    (doseq [kv rates] (db/upsert-euro-to-currency! {:currency (key kv) :date date :rate (val kv)}))
                    )))))

(def ^:private cnj
  (cj/cronj :entries
            [{:id       "print-task"
              :handler  grab-ecb-data
              :schedule "/20 * * * * * *"
              }]))

(defn start! []
  (cj/start! cnj)
  )

(defn stop! []
  (cj/stop! cnj)
  )
