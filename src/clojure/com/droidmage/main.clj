(ns com.droidmage.main
  (:require [clojure.string :as s]
            [com.droidmage.card :as card]
            [com.droidmage.data-manager :as dataman]
            [com.droidmage.screens.home :as home]
            [com.droidmage.server-list :as sl]
            [com.droidmage.sliding-menu :as sm]
            [com.droidmage.view :as v]
            [neko.action-bar :as abar]
            [neko.context :as context]
            [neko.listeners.view :as vl]
            [neko.log :as l]
            ;; [droidmage.wall.hack :as hack]
            [neko.ui :as ui])
  (:use [com.droidmage.shared-preferences :only [defpreference initialize-preferences]]
        [com.droidmage.toast]
        [neko.activity  :only [defactivity simple-fragment]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]]
        [neko.ui.adapters :only [ref-adapter]]
        [neko.ui.mapping  :only [defelement]])
  (:import (android.app Activity)
           ;; android.support.v4.widget.DrawerLayout
           (android.widget TextView FrameLayout)
           (android.view MenuItem View)
           ;; com.jeremyfeinstein.slidingmenu.lib.CustomViewAbove
           ;; com.droidmage.SlidingActivity
           com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity
           com.jeremyfeinstein.slidingmenu.lib.SlidingMenu
           com.droidmage.R
           java.sql.Driver
           java.sql.DriverManager))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sections
(def ^:dynamic *sections*
  "Each section is a map with the following keys:
  name: Display name
  layout: A function taking at least one arg, the activity.
  layout-args: Additional arguments passed to the layout function."
  (atom [{:name "Home" :layout #'home/screen-layout}]))

(defn add-section [name layout & [largs]]
  (swap! *sections* conj {:name name :layout layout :layout-args largs}))

(defn hide-menu [^SlidingActivity a]
  (.showContent a))

(defn make-sections-adapter [^Activity a]
  (ref-adapter
   (fn [context]
     [:linear-layout {:id-holder true, :orientation :horizontal}
      [:image-view {:padding 10, :image-resource android.R$drawable/sym_def_app_icon}]
      [:text-view {:padding 10, :text-color android.graphics.Color/WHITE,
                   :gravity :center-vertical,
                   ;; :text-appearance android.R$style/TextAppearance
                   :id ::section-name, :text-size 20}]])
   (fn [position ^View parent _ {:keys [name layout layout-args]}]
     (v/set-text parent ::section-name name)
     (.setOnClickListener
      parent
      (vl/on-click (apply v/set-layout! a layout layout-args)
                   (hide-menu a))))
   *sections*))

(defn menu-layout [^Activity a]
  (neko.ui/make-ui a [:linear-layout {:orientation :vertical}
                      [:list-view {:adapter (make-sections-adapter a)}]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Activity
(defactivity com.droidmage.MainActivity
  :key :main
  ;; :extends com.droidmage.SlidingActivity
  :extends com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity
  :on-create
  (fn [^SlidingActivity this bundle]
    (.superOnCreate this bundle)
    (swap! (.state this) assoc :add-section-fn #'add-section)
    (keep-screen-on this)
    (initialize-preferences this)

    ;; (v/set-layout! (*a) menu-layout )
    (v/set-layout! this (:layout (first @*sections*)))
    
    (.setBehindContentView this (menu-layout this))
    (.setSlidingActionBarEnabled this false)
    (on-ui
     (let [menu (.getSlidingMenu this)]
       (swap! (.state this) assoc :menu menu)
       (.setMode menu SlidingMenu/LEFT)
       (.setTouchModeAbove menu SlidingMenu/TOUCHMODE_FULLSCREEN)
       ;; (.setShadowWidth menu 400)
       (.setBehindWidth menu 400)
       (.setFadeDegree menu 0.5)))
    ;; (.setShadowDrawable menu R.drawable.shadow)
    ;; (.setBehindOffsetRes menu R.dimen.slidingmenu_offset)
    ;; (.setMenu menu R.layout.menu)
    
    (future (dataman/extract-databases! this)
            (dataman/populate-class-scanner-package-map! this)
            (reset! dataman/databases-ready true))
    (future (when (sl/update-server-list this)
              (to this "Server list updated." :short)))))

;; (.getSlidingMenu (*a))

;; (defn MainActivity-onCreate [^SlidingActivity this bundle])
;; (defn MainActivity-onCreateOptionsMenu
;;   [^Activity this menu])
;; (defn MainActivity-onOptionsItemSelected
;;   [^Activity this ^MenuItem item])

;; (defn MainActivity-onDestroy [^Activity this]
;;   (let [{:keys [^org.mage.network.Client client]}
;;         @(.state this)]
;;     (try (if (.isConnected client)
;;            (.disconnect client))
;;          (catch java.lang.Exception e))))

;; (def deck (.getCards  (card/import-deck (*a) "sample-decks/2011/Zen_M11_SoM/Boros.dck")))
