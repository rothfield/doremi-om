(ns doremi.core
  (:import jline.Terminal)
  (:gen-class)
  (:require [compojure.handler :as handler]
            [jansi-clj.core :refer :all] ;; for game of life
            [jansi-clj.auto]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as resp]
            ring.adapter.jetty
            [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [cheshire.core :as json]
            [clojure.java.io :as io]))
(comment
  (use 'doremi.core :reload) (ns doremi.core) 
  (use 'clojure.stacktrace) 
  (print-stack-trace *e)
)
(enlive/deftemplate page
  (io/resource "index.html")
  []
  [:body] (enlive/append
            (enlive/html [:script (browser-connected-repl-js)])))

;;(defonce repl-env (reset! cemerick.austin.repls/browser-repl-env (cemerick.austin/repl-env)))
;;(defn js-repl[]
 ;; (cemerick.austin.repls/cljs-repl repl-env)
  ;;)
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



(println "loaded doremi.core")

(println "game of life")

(defn nodes[size]
  (for [x (range size) y (range size)] [x y]))

(defn show-grid[grid]
  (doseq [row grid]
    (println row) 
    ))

(def debug false)

(defn neighbors[[row column] size]
  (remove (fn [[a b]]
            (cond 
              (or (< a 0) (>= a size))
              true
              (or (< b 0) (>= b size))
              true
              true
              false))
          [
           [(dec row) (dec column)]
           [ (dec row) column] 
           [ (dec row) (inc column)] 
           [row (dec column)]
           [ row (inc column)] 
           [(inc row) (dec column)]
           [ (inc row) column] 
           [ (inc row) (inc column)] 
           ]
          ))



(defn life-function[node neighbors-list grid]
  {
   :post [(if debug (do (println "life-function returns" %) true)
            true)]}
    (if debug (do
                (println)
                (println "Entering life function")
                (println "life-function, node is" node)
                (println "life-function, grid is")
                (println grid)
                (println "life-function, neighbors-list is" neighbors-list)
                ))

  (let [current (get-in grid node)
        live-cell (= current 1)
        dead-cell (not live-cell)
        neighbor-values (map #(get-in grid %) neighbors-list)
        ;;_ (println neighbor-values)
        neighbors (apply + neighbor-values)
        dies 0
        lives 1]
    (if debug (do
                (println)
                (println "Entering life function")
                (println "life-function, node is" node)
                (println "life-function, grid is")
                (println grid)
                (println "life-function, neighbors-list is" neighbors-list)
                (println "neighbor-values=" neighbor-values)
                (println "current is:" current)
                (println "neighbors=" neighbors)
                (println "live-cell" live-cell)
                (println "dead-cell" dead-cell)
                ))
    (cond 
      (and live-cell (<  neighbors 2)) ;;Any live cell with fewer than two live neighbours dies, as if caused by under-population.
      dies 
      (and live-cell (>  neighbors 3)) ;;Any live cell with more than three live neighbours dies, as if by overcrowding.
      dies 
      live-cell ;;Any live cell with two or three live neighbours lives on to the next generation.
      lives
      (and dead-cell (= 3 neighbors)) ;;Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.
      lives 
      true
      current)))


(defn next-generation[[grid node-neighbors-map] nodes]
[
   (reduce (fn [accum node]
             (assoc-in accum node 
                       (life-function node
                                      (node-neighbors-map node)
                                      grid)))
              grid nodes)
   node-neighbors-map]
  )


(defn node-neighbors-map[size]
  (reduce 
    (fn [accum val] 
      (assoc accum val 
             (neighbors val size) 
             )) {} (nodes size)))
;;(pprint (node-neighbors-map 3))

(def black-square
  "\u25A0"
  )

(defn display-row-old[row]
  (apply str (map 
               (fn[num] (get {1 black-square 0 " "} num))
                  row)
  ))

(defn display-row[row]

  (println (apply str (map 
               (fn[num] (get {1 (black-bg " ")  0 (white-bg " ")} num))
                  row)
  )))

;;(println (display-row [0 1 1 1]))

(defn display-grid[[grid _]]
  (println)
  (doseq [ row grid]
    (display-row row)
    )
  (println))
(defn new-world[grid]
  [grid (node-neighbors-map (count grid))]
  )

(defn trim-grid[grid]
  (let [first-non-empty-row 0]
    grid
  ))
(defn my-hash[world]
  (apply str (apply str world)))

(defn live-the-game-of-life[seed iterations term]
;;  (pprint (new-world seed))
  (let [my-nodes (nodes (count seed)) 
        ]

  (loop [ctr 0 
         world (new-world seed)
         world-set #{(my-hash world)}]
    (let [the-next-generation (next-generation world my-nodes)]
    (println "Iteration" (inc ctr))
    (if true ;;term
  (display-grid world)
      )
      
   ;;   (if term
    ;;  (.readCharacter term System/in))
     ;; (when false ;;(not term)
      ;;  (println "sleeping")
       ;; (Thread/sleep 5000)
       ;; )
    (cond  
     (= ctr iterations)
      iterations
      (world-set (my-hash the-next-generation))
      (do
      (println "Stable world after " ctr " iterations")
      (display-grid world))
      true
      (recur (inc ctr)
             the-next-generation
             (conj world-set (my-hash the-next-generation))))))))


(comment
  (println "\n" (display-grid
                  (next-generation 
                    (new-world [
                                [0 1 0] 
                                [1 1 1]
                                [0 0 0]
                                ]
                               )
                    ))))

(def r-pentamino
  [ [ 0 0 0 0 0]
   [  0 0 1 1 0]
   [  0 1 1 0 0]
   [  0 0 1 0 0]
   [  0 0 0 0 0]
   ]
  )
(def blinker
  [ [ 0 0 0 0 0]
   [  0 0 1 0 0]
   [  0 0 1 0 0]
   [  0 0 1 0 0]
   [  0 0 0 0 0]
   ]
  )

(defn pad[base width]
  "Width will be total length of grid"
  (let 
    [
    blank-line (vec (repeat width 0))
     num-before-lines (int (/ (- width (count base)) 2))
     _ (println num-before-lines)
    blanks-before (vec (repeat num-before-lines blank-line))
        blanks-after (vec (repeat (- width (+ (count blanks-before)
                                       (count base))) blank-line))
       x (vec (repeat (- (/ width 2) (count base)) 0))
      y (vec (repeat (- width (count x) (count base)) 0))
      base2  (vec (map (fn[ary] (vec (concat x ary y))) base))
        ]
    (vec (concat blanks-before base2 blanks-after))
    ))

(comment
(def z (pad r-pentamino 30))
(println (map count z))
(println (count z))
(println (count z) (count (nth z 1)))
(pprint z)
)
;;(println r-pentamino)
;;(live-the-game-of-life (pad r-pentamino 100) 2 nil)
(defn -main[]
  (let [term (Terminal/getTerminal)]
    (live-the-game-of-life (pad r-pentamino 200) 1200 term)

;;  (run)
  ))

