(defproject doremi "0.1.0-SNAPSHOT"
  :source-paths ["src/clj" "src/cljs"]
  :main doremi.core
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.8.3"]
                 ;; CLJ
                 [ring "1.2.1"]

                 [ring/ring-core "1.2.1"]
                 [compojure "1.1.6"]
                 [enlive "1.1.5"]
                 [cheshire "5.3.1"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [secretary "0.4.0"]
                 [cljs-http "0.1.2"]
                ;; [om "0.1.7"]
                 [om "0.6.0"]
                 [com.cemerick/austin "0.1.3"] 
                 ;; for game of life
                 [jansi-clj "0.1.0"] 
                 [jline "0.9.94"]
                 ]

  :plugins [[lein-cljsbuild "1.0.0"]
            [lein-ring "0.8.7"]]

  :ring {:handler doremi.core/app
         :init    doremi.core/init}

  :profiles {
             :dev {
                   ;; Specify the ns to start the REPL in
                   :repl-options {:init-ns doremi.core
                                  :init 
                                  (do
                                ;;    (println "starting web server on http://localhost:8080")
                                 ;;   (doremi.core/run) ;; start web server
                                    ) 
                                  }
                   :plugins [[
                              com.cemerick/austin "0.1.3" 
                              ]]}
             }
  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :externs ["om/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/app.js"
                                   :source-map "resources/public/js/app.js.map"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :output-wrapper false
                                   :preamble ["om/react.min.js"]
                                   :externs ["om/externs/react.js"]
                                   :closure-warnings
                                   {:non-standard-jsdoc :off}}}]})
