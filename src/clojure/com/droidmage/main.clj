(ns com.droidmage.main
  (:require [clojure.string :as s]
            [com.droidmage.card :as card]
            [com.droidmage.chat :as chat]
            [com.droidmage.data-manager :as dataman]
            [com.droidmage.server :as server]
            [com.droidmage.server-list :as sl]
            [com.droidmage.view :as v]
            [neko.action-bar :as abar]
            [neko.context :as context]
            [neko.log :as l]
            [neko.ui :as ui]
            [wall.hack :as hack])
  (:use [com.droidmage.toast]
        [com.droidmage.shared-preferences :only [defpreference initialize-preferences]]
        [neko.activity  :only [defactivity simple-fragment]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]]
        [neko.ui.mapping  :only [defelement]])
  (:import (android.app Activity ProgressDialog)
           ;; android.support.v4.widget.DrawerLayout
           (android.widget TextView FrameLayout)
           ;; com.jeremyfeinstein.slidingmenu.lib.CustomViewAbove
           com.jeremyfeinstein.slidingmenu.lib.SlidingMenu
           java.sql.Driver
           java.sql.DriverManager))

(def is-connecting (atom false))
(defpreference last-username nil)
(declare attempt-connection)
(declare add-server-dialog)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Layout
(defn main-layout [^Activity act]
  (let [input-view
        (ui/make-ui
         act (v/make-text-input (fn [view _action _event]
                                  (attempt-connection act (v/get-text view)))
                                {:hint "Username",  :id ::username,
                                 :gravity :center, :layout-width :fill,
                                 :text (or @last-username ""),
                                 :enabled (not @is-connecting)
                                 :single-line true}))]
    [:linear-layout {:orientation :vertical
                     :padding-top 30
                     :padding-bottom 30
                     :id-holder true
                     :layout-width :match-parent
                     :gravity :center-horizontal}

     [:linear-layout {:orientation :horizontal
                      :padding 10, :padding-top 20
                      :id-holder true
                      :layout-width :match-parent
                      :gravity :center}
      [:text-view {:layout-weight 25}]
      [:linear-layout {:layout-weight 50} input-view]
      [:text-view {:layout-weight 25}]]

     [:linear-layout {:padding 10,
                      :orientation :horizontal}
      [:text-view {:text "Server: "}]
      [:button {:text (:name @sl/current-server),
                :id ::server-button,
                :enabled (not @is-connecting)
                :on-click sl/show-server-picker}]]

     (v/make-button
      "Connect" 30 (not @is-connecting)
      (attempt-connection act (v/get-text input-view)))

     (v/make-button "Add Server" nil (not @is-connecting)
                    add-server-dialog)

     [:progress-bar {:visibility (if @is-connecting :visible :invisible)
                     :padding 30}]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Connection
(defn add-server-dialog [^android.view.View button]
  "items is a list of strings.
  callback is a function that will be called with the postion of the
  selected item."
  (let [a (.getContext button)]
    (v/prompt-for-inputs a "Add Server"
                         (fn [[n ad p]]
                           (try (let [server {:name n :desc "" :address ad
                                              :port (Integer/parseInt p)}]
                                  (swap! sl/known-servers #(vec (distinct (conj % server))))
                                  (reset! sl/current-server server)))
                           (on-ui (v/set-layout! a main-layout)))
                         [{:hint "Name"}
                          {:hint "Address" :input-type :number}
                          {:hint "Port" :input-type :number}])))

(defn set-connected-layout
  "Reset the layout of ServerActivity to the proper tabs."
  [a chat-id]
  (ld "Setting Layout: ")
  (v/set-layout! a (constantly [:relative-layout {}]))
  (on-ui (abar/setup-action-bar
          a {:navigation-mode :tabs
             :tabs [[:tab {:text "Tables"
                           :tab-listener (simple-fragment
                                          a [:text-view {:text "Not Implemented"}])}]
                    [:tab {:text "Chat"
                           :tab-listener (simple-fragment
                                          a (chat/chat-layout a chat-id))}]]})))

(defn post-connect [^Activity a state-atom exception]
  (reset! is-connecting false)
  (if state-atom
    ;; Succeeded.
    (let [{:keys [server chat-id]} @state-atom
          {:keys [name address]} server]
      (on-ui (abar/setup-action-bar
              a {:title name, :subtitle address
                 :display-options :show-title}))
      (set-connected-layout a chat-id))
    ;; Failed.
    (do (v/set-layout! a main-layout)
        (to a "Failed Connection: " (str exception)))))

(defn attempt-connection [^Activity a user]
  (reset! last-username user)
  (.hideSoftInputFromWindow
   ^android.view.inputmethod.InputMethodManager
   (context/get-service a :input-method)
   (.getWindowToken (.getDecorView (.getWindow a))) 0)
  (reset! is-connecting true)
  (v/set-layout! a main-layout)
  (server/connect user @sl/current-server (partial post-connect a)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Activity
(defactivity com.droidmage.MainActivity
  :key :main
  :on-create
  (fn [^Activity this bundle]
    (keep-screen-on this)
    (initialize-preferences this)
    ;; (reset! last-username "ok")
    (v/set-layout! this main-layout)
    (future (dataman/extract-databases! this)
            (dataman/populate-class-scanner-package-map! this)
            (reset! dataman/databases-ready true))
    (sl/update-server-list this)))

;; (defn MainActivity-onDestroy [^Activity this]
;;   (let [{:keys [^org.mage.network.Client client]}
;;         @(.state this)]
;;     (try (if (.isConnected client)
;;            (.disconnect client))
;;          (catch java.lang.Exception e))))

;; (def deck (.getCards  (card/import-deck (*a) "sample-decks/2011/Zen_M11_SoM/Boros.dck")))

;; (defn attach-sliding-menu [this]
;;   (-> (SlidingMenu. this)
;;       (.setMode SlidingMenu.LEFT)
;;       (.setTouchModeAbove SlidingMenu.TOUCHMODE_FULLSCREEN)
;;       ;; (.setShadowWidthRes R.dimen.shadow_width)
;;       ;; (.setShadowDrawable R.drawable.shadow)
;;       ;; (.setBehindOffsetRes R.dimen.slidingmenu_offset)
;;       (.setFadeDegree 0.35f)
;;       (.attachToActivity this SlidingMenu.SLIDING_CONTENT)
;;       (.setMenu R.layout.menu)))

;; (import 'com.jeremyfeinstein.slidingmenu.lib.SlidingMenu)
;; (import (resolve 'com.jeremyfeinstein.slidingmenu.lib.SlidingMenu))

;; (defelement :list-view-2
;;   :inherits :list-view
;;   :constructor-args [android.view.Gravity/LEFT])

;; (defn make-adapter [vecs]
;;   (neko.ui.adapters/ref-adapter
;;    (fn [_] [:linear-layout {:id-holder true}
;;            [:text-view {:id ::caption-tv
;;                         :text "okokokokokkk"}]])
;;    (fn [position view _ text] nil)
;;    (atom vecs)))

;; (def lview (ui/make-ui (*a) [:list-view-2 {:padding 10
;;                                            :adapter
;;                                            (make-adapter
;;                                             [[:text-view {:text "Server: "}]
;;                                              [:text-view {:text "Server: "}]
;;                                              [:text-view {:text "Server: "}]
;;                                              [:text-view {:text "pos: "}]])}]))
;; (def dl (DrawerLayout. (*a)))
;; (def fl (FrameLayout. (*a)))

;; ;; [:list-view {:adapter (make-chat-adapter id)
;; ;;              :background-color android.graphics.Color/BLACK}]
;; (.addView dl fl 1 0)
;; (ui-e)

;; (on-ui (.addView dl lview 0 0))
;; (on-ui (neko.activity/set-content-view! (*a) dl))
;; (.isDrawerOpen dl)
