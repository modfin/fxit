(ns fxit.db
  (:require [yesql.core :as y]))

(def ^:private db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "db/database.db"
   })


(y/defquery latest "lib.sql"
            {:connection db})
; (get (first (latest)) :content)

(y/defquery rate-by-date "lib.sql"
            {:connection db})
; (get (first (rate-by-date {:date "2015-07-21"})) :content)

(y/defquery rates-in-range "lib.sql"
            {:connection db})

(y/defquery upsert-rate! "lib.sql"
            {:connection db})
; (upsert-rate! {:date "2015-07-21" :content "the content"})

(y/defquery upsert-euro-to-currency! "lib.sql"
            {:connection db})
; (upsert-rate! {:date "2015-07-21" :content "the content"})

(y/defquery avg-rates-in-range "lib.sql"
            {:connection db})

(y/defquery min-rates-in-range "lib.sql"
            {:connection db})

(y/defquery max-rates-in-range "lib.sql"
            {:connection db})
