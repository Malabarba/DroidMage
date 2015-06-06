(ns com.droidmage.sliding-menu
  (:require [clojure.string :as s]
            [com.droidmage.view :as v]
            [neko.listeners.text-view :as tl])
  (:use com.droidmage.iop
        com.droidmage.toast
        [neko.activity  :only [defactivity simple-fragment]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.ui.adapters :only [ref-adapter]])
  (:import android.app.Activity
           (android.view MenuItem View)
           com.jeremyfeinstein.slidingmenu.lib.SlidingMenu
           com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity))

(defn sliding-menu-layout [adapter]
  [:linear-layout {:orientation :vertical
                   :id-holder true
                   :layout-width :match-parent
                   :gravity :bottom}
   [:list-view {:adapter adapter
                :background-color android.graphics.Color/BLACK}]])


(defn attach-sliding-menu [this]
  (let [menu (SlidingMenu. this)]
    (.setMode menu SlidingMenu/LEFT)
    (.setTouchModeAbove menu SlidingMenu/TOUCHMODE_FULLSCREEN)
    ;; (.setShadowWidthRes menu R.dimen.shadow_width)
    ;; (.setShadowDrawable menu R.drawable.shadow)
    ;; (.setBehindOffsetRes menu R.dimen.slidingmenu_offset)
    (.setFadeDegree menu 0.35)
    (.attachToActivity menu this SlidingMenu/SLIDING_CONTENT)
    ;; (.setMenu menu R.layout.menu)
    ))

;; (def menu (SlidingMenu. (*a)))
;; (on-ui (attach-sliding-menu (*a)))

(defactivity com.droidmage.SlidingActivity
  :key :main
  :extends com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity
  :on-create
  (fn [^Activity this bundle]
    (keep-screen-on this)
    ())

  :on-create-options-menu
  (fn [^Activity this menu])

  :on-options-item-selected
  (fn [^Activity this ^MenuItem item]))
