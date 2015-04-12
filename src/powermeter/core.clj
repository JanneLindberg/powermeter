(ns powermeter.core
  (:gen-class)
  (:use org.httpkit.server)
  (:use ring.util.response)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as resp]
            [ring.middleware.json :as middleware]
            [ring.adapter.jetty :as ring]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.local :as l]
            ))


(def ^{:private true} default-port 8093)

(def ^{:private true} max-data-length (* 13 60))
(def ^{:private true} pwr-data (atom []))

(def ^{:private true} prev-data (atom 0))

(defonce server (atom nil))


(def time_zone (t/time-zone-for-offset 2))

(defn- time-string [millisec]
  (str (l/format-local-time
      (t/to-time-zone  (c/from-long millisec) time_zone) :date-time)))


(defn- ms-time [ts]
  (* 1000 (read-string (first (clojure.string/split ts #"\."))))
)


(defn- add-pwr-data
  "Add to the tail of the pwr-data array, drop the first entry when max size is exceeded"
  [timestamp watt kwh]

  (let [num (read-string kwh)]
;    (println "kwh " kwh "   num:" num  "   time:" (time-string timestamp))
;    (println "diff:" (- num @prev-data))
    (reset! prev-data num))
  (let [data (conj (vec @pwr-data) {:watt watt :kwh kwh :timestamp timestamp})]
    (if (> (count data) max-data-length)
      (reset! pwr-data (rest data))
      (reset! pwr-data data)
      ))
  nil)


(defn- handle-pwr-data-post [msg]
  (doseq [body msg]
    (add-pwr-data (read-string (:timestamp body)) (:watt body) (:kwh body)))
  (response nil))


(defn- get-power-data
  "Return the array of power measurement values"
  []
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str (json/write-str @pwr-data ))
   })


(defroutes my-routes
  (GET "/powerdata/" [] (get-power-data))
  (POST "/data" {body :body} (handle-pwr-data-post body))
  (route/resources "/")
  (GET "/" [] (resp/redirect "/powermeter.html"))
  (route/not-found "Not Found 404")
)


(def handler
  (-> (handler/api my-routes)
      (middleware/wrap-json-body  {:keywords? true :bigdecimals? true })
      ))


(defn start-server [port-no]
  (reset! server (run-server #'handler {:port port-no})))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))


(defn -main [& args]
  (let [port (Integer. (or (System/getenv "PORT") default-port))]
    (println "Using port:" port)
    (start-server port)))
