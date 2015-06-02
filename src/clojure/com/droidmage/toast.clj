(ns com.droidmage.toast
  (:require [clojure.string :as s]
            [neko.log :as l])
  (:use [neko.threading :only [on-ui]]
        [neko.notify    :only [toast]]
        [neko.debug     :only [*a]])
  (:import (android.app Activity)))

(defn to [^Activity a & info]
  (l/i info)
  (on-ui
   (if (and (seq info) (#{:long :short} (last info)))
     (toast a (clojure.string/join " " (butlast info)) (last info))
     (toast a (clojure.string/join " " info) :long))))

(defmacro t [& info]
  `(to (*a) ~@info))

(def tag "MyApp")

(defmacro le [& args]
  `(neko.log/e ~@args :tag ~tag))

(defmacro ld [& args]
  `(neko.log/d ~@args :tag ~tag))

(defmacro li [& args]
  `(neko.log/i ~@args :tag ~tag))

(defmacro lv [& args]
  `(neko.log/v ~@args :tag ~tag))

(defmacro lw [& args]
  `(neko.log/w ~@args :tag ~tag))
