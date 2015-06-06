(ns clojure.com.droidmage.game-activity
  (:use [neko.activity :only [defactivity simple-fragment]])
  (:import (android.app Activity)))

(defactivity com.droidmage.GameActivity
  :key :game
  :features [:no-title]
  
  :on-create
  (fn [^Activity this bundle]))

