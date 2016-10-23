(ns fxit.core
  (:gen-class)
  (:require [fxit.db :as db])
  (:require [fxit.cron :as cron])

  (:require [catacumba.core :as ct])
  (:require [catacumba.http :as http])
  (:require [catacumba.handlers.misc :as misc])
  (:require [clojure.data.json :as json])
  (:require [clojure.core.cache :as cache])
  (:require [clojure.string :as s])
  (:require [clostache.parser :as mustach])
  (:require [manifold.deferred :as d]))

(def C (atom (-> {}
                 (cache/fifo-cache-factory :threshold 200)
                 (cache/ttl-cache-factory :ttl 5000))))

(defn http-ok [context result]
  (let [headers {}
        headers (cond
                  (get-in context [:headers :origin])
                  (assoc headers "Access-Control-Allow-Credentials" "true"
                                 "Access-Control-Allow-Methods" "GET"
                                 "Access-Control-Allow-Headers" "X-Requested-With, Content-Type, X-Codingpedia"
                                 "Access-Control-Allow-Origin" (get-in context [:headers :origin]))
                  :else
                  headers
                  )
        headers (cond
                  (> (count (get-in context [:query-params :callback])) 0)
                  (assoc headers "content-type" "application/javascript;charset=utf-8")
                  :else
                  (assoc headers "content-type" "application/json;charset=utf-8")
                  )
        ]

    (http/ok result headers)
    )

  )



(defn update-map [m f]
  (reduce-kv (fn [m k v]
               (assoc m k (f v))) {} m))


(defn sig-round
  [old-num new-num]
  (let [digs (- (count (str old-num)) 1)
        precision digs
        factor (Math/pow 10 precision)
        ]
    (/ (Math/round (* new-num factor)) factor))
  )


(defn rebase [new data]
  (let [old (data "base")
        ratio (/ 1
                 (get-in data ["rates" new]))]
    (assoc data
      "base" new
      "rates" (assoc
                (update-map (dissoc (data "rates") new) (fn [a] (* a ratio)))
                old ratio))))


(defn wrap-in-callback [context json]
  (cond
    (> (count (get-in context [:query-params :callback])) 0)
    (str (get-in context [:query-params :callback]) "(" json ");")
    :else json))

(defn item-filter [context json]

  (let [data (json/read-str json)
        base (get-in context [:query-params :base])
        symbols-str (get-in context [:query-params :symbols])
        data (cond
               (and base (get-in data ["rates" (s/upper-case base)]))
               (rebase
                 (s/upper-case base)
                 data)
               :else data
               )
        data (cond symbols-str
                   (let [symbols (s/split (s/upper-case symbols-str) #",")]
                     (assoc data "rates" (select-keys (data "rates") symbols)))
                   :else data
                   )
        ]
    (json/write-str data)))

(defn data-retrive [cache-key fn-modifier fn-data-call]
  (cond
    (cache/has? @C cache-key) (do
                                (cache/lookup @C cache-key))
    :else (do
            (get-in (swap! C #(cache/miss
                               % cache-key
                               (get (fn-modifier (fn-data-call)) :content))) [cache-key]))))


(defn agg-to-json [result]
  {:content (json/write-str {"base"  "EUR"
                             "func"  (get (first result) :name)
                             "rates" (reduce (fn [acc curr]
                                               (assoc acc (curr :currency) (curr :agg_rate)))
                                             {} result)})})

(defn arr-to-json [result]
  (let [ a {:content
          (map
            #(json/read-str (:content %))
            result)
          }
        ]
    a)
  )


(defn rates [context]
  (let [minDate (s/replace (get-in context [:route-params :minDate]) #"^0-9-", "")
        maxDate (s/replace (get-in context [:route-params :maxDate]) #"^0-9-", "")
        db-fn (fn [] (db/rates-in-range {:minDate minDate :maxDate maxDate}))

        result (d/future
                 (wrap-in-callback
                   context
                   (json/write-str
                     (map #(json/read-str (item-filter context (json/write-str %)))
                          (data-retrive
                            (str minDate ":" maxDate "")
                            arr-to-json
                            db-fn
                            )))))
        ]
    (http-ok context result)))

(defn aggRates [context]
  (let [minDate (s/replace (get-in context [:route-params :minDate]) #"^0-9-", "")
        maxDate (s/replace (get-in context [:route-params :maxDate]) #"^0-9-", "")
        db-func-name (get-in context [:query-params :func] "avg")
        db-func (cond
                  (= db-func-name "min") db/min-rates-in-range
                  (= db-func-name "max") db/max-rates-in-range
                  :else db/avg-rates-in-range)
        db-fn (fn [] (db-func {:minDate minDate :maxDate maxDate}))
        result (d/future
                 (wrap-in-callback
                   context
                   (item-filter context
                                (data-retrive
                                  (str minDate ":" maxDate ":agg")
                                  agg-to-json
                                  db-fn))))]
    (http-ok context result)))

(defn latest [context]
  (let [result (d/future
                 (wrap-in-callback
                   context
                   (item-filter context
                                (data-retrive "latest" first db/latest))))]
    (http-ok context result)))


(defn dated [context]
  (let [date (s/replace (get-in context [:route-params :date]) #"^0-9-", "")
        result (d/future
                 (wrap-in-callback
                   context
                   (item-filter context
                                (data-retrive date first (fn [] (db/rate-by-date {:date date}))))))]
    (http-ok context result)))


(defn logger
  [context]
  ; Do someting here to monitor rpm and such
  (ct/delegate)
  )


(defn web [context]
  (http/ok (mustach/render-resource "templates/index.mustach" {}) {"content-type" "text/html;charset=utf-8"}))

(def app
  (ct/routes [[:any (misc/autoreloader)]
              [:any #'logger]
              [:get "" #'web]
              [:get ":minDate/:maxDate" #'rates]
              [:get ":minDate/:maxDate/agg" #'aggRates]
              [:get "latest" #'latest]
              [:get ":date" #'dated]
              ]))

(defn -main
  [& args]
  (ct/run-server app {:port 80 :debug false})
  (cron/start!)
  )

