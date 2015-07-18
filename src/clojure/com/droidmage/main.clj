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
  (atom [{:name "Home" :layout #'com.droidmage.screens.home/screen-layout}]))

(defn add-section [name layout & [largs]]
  (swap! *sections* conj {:name name :layout layout :layout-args largs}))

(defn hide-menu [^SlidingActivity a]
  (.showContent a))

(declare menu-layout)
(defn make-sections-adapter [^Activity a]
  (ref-adapter
   (fn [context]
     [:linear-layout {:id-holder true, :orientation :horizontal, :padding 16}
      [:image-view {:padding-right 10, :image-resource android.R$drawable/sym_def_app_icon}]
      [:text-view {:text-color android.graphics.Color/WHITE, ;; :padding 10,
                   :gravity :center-vertical,
                   ;; :text-appearance android.R$style/TextAppearance
                   :id ::section-name, :text-size 20}]])
   (fn [position ^View parent _ {:keys [name layout layout-args]}]
     (v/set-text parent ::section-name name)
     (.setOnClickListener
      parent
      (vl/on-click (apply v/set-layout! a @layout layout-args)
                   (.setBehindContentView ^SlidingActivity a (menu-layout a))
                   (hide-menu a))))
   *sections*))

(defn menu-layout [^Activity a]
  (neko.ui/make-ui a [:linear-layout {:orientation :vertical}
                      [:text-view {:layout-height 120, :text "Placeholder Space"}]
                      [:list-view {:adapter (make-sections-adapter a)}]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Activity
(defn- setup-layout [^SlidingActivity a]
  ;; (v/set-layout! (*a) menu-layout )
  (v/set-layout! a @(:layout (first @*sections*)))

  (.setBehindContentView a (menu-layout a))
  (.setSlidingActionBarEnabled a false)
  (on-ui
   (let [menu (.getSlidingMenu a)]
     (swap! (.state a) assoc :menu menu)
     (.setMode menu SlidingMenu/LEFT)
     (.setTouchModeAbove menu SlidingMenu/TOUCHMODE_FULLSCREEN)
     ;; (.setShadowWidth menu 400)
     (.setBehindWidth menu 400)
     (.setFadeDegree menu 0.5)))
  ;; (.setShadowDrawable menu R.drawable.shadow)
  ;; (.setBehindOffsetRes menu R.dimen.slidingmenu_offset)
  ;; (.setMenu menu R.layout.menu)
  )

(defactivity com.droidmage.MainActivity
  :key :main
  ;; :extends com.droidmage.SlidingActivity
  :extends com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (swap! (.state this) assoc :add-section-fn #'add-section)
    (keep-screen-on this)
    (initialize-preferences this "simple-preferences")

    (setup-layout this)

    (future (to this "Extracting sample decks" :short)
            (doseq [deckfile (.list (.getAssets this) "decks")]
              (dataman/extract-file! this deckfile false))
            (add-section "Decks" #'com.droidmage.screens.decks/screen-layout)
            (to this "Extracting databases" :short)
            (dataman/extract-databases! this)
            (to this "Populating the cards list" :short)
            (dataman/populate-class-scanner-package-map! this)
            (reset! dataman/databases-ready true)
            (to this "All done" :short)
            (when (sl/update-server-list this)
              (to this "Server list updated." :short)))))

;; (on-ui (v/set-layout! (*a) @(:layout (first @*sections*))))
;; (on-ui (.setBehindContentView ^SlidingActivity (*a) (menu-layout (*a))))
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
