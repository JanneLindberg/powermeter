(defproject powermeter "0.1.0-SNAPSHOT"
  :description "Simple powermeter server"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/data.json "0.2.5"]
                 [http-kit "2.1.16"]
                 [compojure "1.3.2"]
                 [clj-time "0.9.0"]
                 ]
  :main powermeter.core
  :uberjar-name "power-meter.jar"
  :profiles {:uberjar {:aot [powermeter.core]}}
)
