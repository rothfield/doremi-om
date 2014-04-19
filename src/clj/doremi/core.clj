(ns doremi.core
  (:gen-class)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as resp]
            ring.adapter.jetty
            [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(enlive/deftemplate page
  (io/resource "index.html")
  []
  [:body] (enlive/append
            (enlive/html [:script (browser-connected-repl-js)])))

(defonce repl-env (reset! cemerick.austin.repls/browser-repl-env (cemerick.austin/repl-env)))

(defn js-repl[]
  (cemerick.austin.repls/cljs-repl repl-env)
  )
(def comments (atom []))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn init
  []
  (reset! comments (-> (slurp "comments.json")
                       (json/parse-string true)
                       vec)))

(defn save-comment!
  [{:keys [body]}]
  (let [comment (-> body io/reader slurp (json/parse-string true))]
    (swap! comments conj comment)
    (json-response
      {:message "Saved comment!"})))

(defroutes app-routes
  (GET "/old" [] (resp/redirect "/index.html"))

  (GET "/comments" [] (json-response
                        {:message "Here's the comments!"
                         :comments @comments}))

  (POST "/comments" req (save-comment! req))

  (route/resources "/")
  (GET "/*" req (page))

  (route/not-found "Page not found"))

(def app
  (-> #'app-routes
      handler/api))

(defn run
  []
  (defonce ^:private server
    (ring.adapter.jetty/run-jetty #'app {:port 8080 :join? false}))
  server)

(defn -main[]
  (run)
  )


(println "loaded doremi.core")

