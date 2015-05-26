(ns com.endlessparentheses.droidmage.toast
  (:require [clojure.string :as s])
  (:use [neko.threading :only [on-ui]]
        [neko.notify    :only [toast]]
        [neko.debug     :only [*a]])
  (:import (android.app Activity)))

(defn to [^Activity a & info]
  (on-ui
   (if (and (seq info) (#{:long :short} (last info)))
     (toast a (clojure.string/join " " (butlast info)) (last info))
     (toast a (clojure.string/join " " info) :long))))

(defmacro t [& info]
  `(to (*a) ~@info))
