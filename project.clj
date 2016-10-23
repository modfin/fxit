(defproject fxit "0.1.0-SNAPSHOT"
  :description "A small FX rate convertion service"
  :url "http://fx.modfin.se"
  :license {:name "The MIT License (MIT)"
            :url  "https://github.com/modfin/fxit/blob/master/LICENSE"}
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [funcool/catacumba "0.17.0"]
                 [org.clojure/core.async "0.2.385"]
                 [http-kit "2.1.18"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.6.5"]
                 [im.chit/cronj "1.4.4"]
                 [yesql "0.5.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-time "0.12.0"]
                 [funcool/promesa "1.4.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 ]
  :main fxit.core
  :aot [fxit.core])
