(ns doremi.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                  ;; [secretary.macros :refer [defroute]]
                   
                   )
  (:require [goog.events :as events]
            [clojure.browser.repl]
            [cljs.core.async :refer [put! <! >! chan timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
           ;; [secretary.core :as secretary]
            [cljs-http.client :as http]
            [doremi.utils :refer [guid]])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(defn hello
  []
  (js/alert "hello"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util

(defn- with-id
  [m]
  (assoc m :id (guid)))

(defn- fetch-comments
  [app {:keys [url]}]
  (go (let [{{cs :comments} :body} (<! (http/get url))]
        (om/update!
          app
          ;; dnolen says the app-state (comments) must satify map? or indexed?
          ;; Lists and lazy seqs (returned by map) don't do that, but mapv
          ;; returns a vector, which satisfies indexed? so that'll work.
          #(assoc % :comments (mapv with-id cs))))))

(defn- value-from-node
  [owner field]
  (let [n (om/get-node owner field)
        v (-> n .-value clojure.string/trim)]
    (when-not (empty? v)
      [v n])))

(defn- clear-nodes!
  [& nodes]
  (doseq [n nodes]
    (set! (.-value n) "")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Components

(def INITIAL [])

(def app-state
  (atom {:comments INITIAL
         :renderAs "sargam-composition"
         :src "drm"
         :attributes {
                        :kind "sargam-composition"
                      }
         :error nil
         :parsed
        [:composition
         [:attribute-section "kind" :sargam-composition]
         [:stave
           [:notes-line
              [:measure [:beat [:pitch "Ab"] [:pitch "Db"] [:pitch "F"]]]]]]

         
         }))

(defn comment
  [{:keys [author text] :as cursor} owner opts]
  ;;(.log js/console (str "comment cursor = " cursor))
  (om/component
    (let [raw-markup "zzz" ;;(md/mdToHtml (or text "blank comment!"))
          color "red"]
      (dom/div #js {:className "comment"}
               (dom/h2 #js {:className "commentAuthor"} author)
               (dom/span #js {:dangerouslySetInnerHTML #js {:__html raw-markup}} )))))

(defn comment-list
  [{:keys [comments] :as cursor} owner opts]
  ;; (.log js/console (str "comment-list cursor = " (into {} cursor)))
  ;; (.log js/console (str "owner = " (into {} owner)))
  (om/component
    (dom/div #js {:className "commentList"}
             (into-array
               (om/build-all comment comments
                             {:key :id
                              :fn identity
                              :opts opts})))))

(defn save-comment!
  [comment cursor {:keys [url]}]
  (do (om/update! cursor
                  (fn [comments] 
                    (conj comments (assoc comment :id (guid)))))
      (go (let [res (<! (http/post url {:json-params comment}))]
            (prn (:message res))))))

(defn handle-submit
  [e cursor owner opts]
  (let [[author author-node] (value-from-node owner "author")
        [text text-node]     (value-from-node owner "text")]
    ;;(.log js/console cursor)
    (when (and author text)
      (save-comment! {:author author :text text} cursor opts)
      (clear-nodes! author-node text-node))
    false))


(defn doremi-text-area[app owner opts]
  (reify
    om/IRender
    (render [this]
      (let [placeholder-text
            (str "Enter letter music notation using 1234567,CDEFGABC, DoReMi (using drmfslt or DRMFSLT), SRGmPDN, or devanagri: " 
                 "सर ग़म म'प धऩ" 
                 "   Example:  | 1 -2 3- -1 | 3 1 3 - | \n\n")

            ]
        (dom/textarea #js 
                      {
                       :id "the_area"
                       :name "src"
                       :rows "4"
                       :cols "50"
                       :className "entryArea"
                       :placeholder placeholder-text
                       ;; :onChange this.handleTextChange, TODO
                       :ref "src"
                       }))

      )
    )
  )

(defn doremi-text-area-box[app owner opts]

  (reify
    om/IRender
    (render [this]
      (let [placeholder-text
            (str "Enter letter music notation using 1234567,CDEFGABC, DoReMi (using drmfslt or DRMFSLT), SRGmPDN, or devanagri: " 
                 "सर ग़म म'प धऩ" 
                 "   Example:  | 1 -2 3- -1 | 3 1 3 - | \n\n")

            ]
        (dom/div #js {
                      :className "entryAreaBox doremiContent"
                      }
                 (om/build doremi-text-area app )
                 )
    ))))

(defn comment-form
  [app owner opts]
  (reify
    om/IDidMount
    (did-mount [this node]
      ;; $(this.getDOMNode()) .autosize();
      (.log js/console "omIDidMount" "node is" node)
      )
    om/IRender
    (render [this]
      (let [placeholder-text
            (str "Enter letter music notation using 1234567,CDEFGABC, DoReMi (using drmfslt or DRMFSLT), SRGmPDN, or devanagri: " 
                 "सर ग़म म'प धऩ" 
                 "   Example:  | 1 -2 3- -1 | 3 1 3 - | \n\n")

            ]
        (dom/form
          #js {:className "commentForm" :onSubmit #(handle-submit % app owner opts)}
          (dom/div #js {
                        :className "entryAreaBox doremiContent"
                        }
                   (om/build doremi-text-area app )

                   )
          (dom/input #js {:type "text" :placeholder "Say something..." :ref "text"})
          (dom/input #js {:type "submit" :value "Post"})))
      )
    ))

(defn comment-box 
  [cursor owner {:keys [poll-interval] :as opts}]
  (reify
    om/IInitState
    (init-state [this]
      (om/update! cursor #(assoc % :comments INITIAL)))
    om/IWillMount
    (will-mount [this]
      (go (while true
            (fetch-comments cursor opts)
            (<! (timeout poll-interval)))))
    om/IRender
    (render [this]
      (dom/div
        #js {:className "commentBox"}
        (dom/h1 nil "Comments")
        (om/build comment-list cursor)
        (om/build comment-form cursor {:opts opts})))))
(defn header[cursor owner]
  (reify
    om/IRender
    (render [this]
      (let [example 
            (str
              "Enter letter music notation using 1234567,CDEFGABC, DoReMi (using drmfslt or DRMFSLT), SRGmPDN, or devanagri: " 
              "सर ग़म म'प धऩ"  "\n\n"
              ) 
            ]
        (dom/h3 #js {:title "Example"}, example)
        ))
    ))


(defn select-kind-box[cursor owner]
  (reify
    om/IRender
    (render [this]
      (let [kinds ["", "abc-composition", "doremi-composition",
                   "hindi-composition",
                   "number-composition", "sargam-composition"
                   ]
            options (map-indexed (fn[idx z ] (dom/option #js{:value z :key idx} z)) kinds)
            ]

        (dom/div #js{:className "selectNotationBox"}
                 (dom/label #js{:htmlFor: "selectNotation"} 
                            "Enter Notation as: ")
                 (apply dom/select #js {:id "selectNotation"} options)

                 )))))
(defn render-as-box[cursor owner]
  (reify
    om/IRender
    (render [this]
      (let [kinds ["", "abc-composition", "doremi-composition",
                   "hindi-composition",
                   "number-composition", "sargam-composition"
                   ]
            options (map-indexed (fn[idx z ] (dom/option #js{:value z :key idx} z)) kinds)
            ]

        (dom/div #js{:className "renderAsBox"}
                 (dom/label #js{:htmlFor: "renderAs"} 
                            "Render as: ")
                 (apply dom/select #js {:id "renderAs"} options)

                 )))))

(defn generate-staff-notation-button[cursor owner]
  (reify
    om/IRender
    (render [this]
      (dom/button #js {:name "generateStaffNotation"
                       :title "Generates Staff Notation/ MIDI/ Lilypond"}
                  "Generate Staff Notation/ MIDI/ Lilypond")

      ))
  )

(defn composition[cursor owner]
  (reify
    om/IRender
    (render [this]
  (.log js/console "appstate = " (pr-str @app-state))
  (.log js/console "cursor = " (pr-str cursor))
  (.log js/console "owner = " (pr-str owner))
      (dom/div #js{:className "composition doremiContent"}
               (dom/h3 #js{} "Rendered composition here")
               )

      )))

(defn doremi-box [cursor owner]
  (reify
    om/IDisplayName
    (display-name [this]
      "doremi-box")
    om/IRender
    (render [this]
      (dom/div #js {:className "doremiBox"} 
               (om/build header cursor nil)
               (dom/div #js {:className "controls"}
                        (om/build select-kind-box cursor)
                        (om/build render-as-box cursor)
                        (om/build generate-staff-notation-button cursor)
                        )
               (om/build doremi-text-area-box cursor)
               (om/build composition cursor)
               
               ))))

(om/root app-state doremi-box (.getElementById js/document "content")
         )
;; #js 
;;{ :urlBase: "/" ;; URL to post to server
;;}
