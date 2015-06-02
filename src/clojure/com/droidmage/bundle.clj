(ns clojure.com.droidmage.bundle
  (:import android.content.Intent
           android.os.Bundle))

(defn add-to-bundle [b key v]
  (let [k (if (keyword? key) (name key) (str key))]
    (cond
      (instance? Intent b) (.putExtra b k v)
      (string? v) (.putCharSequence b k v)
      (integer? v) (.putShort b k v)))
  b)

(defn into-bundle
  ([m] (into-bundle (Bundle.) m))
  ([bun m]
   (reduce (fn [b [k v]] (add-to-bundle b (name k) v))
           bun m)))
