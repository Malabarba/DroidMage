(ns com.droidmage.shared-preferences
  (:require [neko.data :as data])
  (:use [com.droidmage.toast])
  (:import (android.content SharedPreferences Context)))

(def sp "SharedPreferences manager for the application." (atom nil))
(def preferences "Set of atoms to keep track of." (atom #{}))

(defn- watch
  "Watch function for saving preferences whenever they are edited."
  [^SharedPreferences sp _key ref old new]
  (when-not (= old new)
    (ld "Preference " ref " updated from " old " to " new)
    (when-not sp
      (throw (java.lang.Exception
              (str "shared-preferences not initialized: " sp))))
    (locking sp
      (ld "Saving it in " sp)
      (-> (.edit sp)
          (data/assoc-arbitrary! (:sp-key (meta ref)) new)
          .commit))))

(defn track-and-set!
  "Set the value of atom `a` according to the value stored in
  `shared-prefs` (if any), then add a watch to it so any changes in
  its value are updated in `shared-prefs`.
  shared-prefs defaults to `sp`.
  The atom must have an `:sp-key` meta property holding a string or a
  keyword."
  ([a] (track-and-set! a @sp))
  ([a ^SharedPreferences sp]
   (let [key (:sp-key (meta a))]
     (when-not key
       (throw (java.lang.IllegalArgumentException.
               (str "This atom is not a preference: " a))))
     (when-not sp
       (throw (java.lang.Exception
               "shared-preferences not initialized")))
     (locking sp
       (try (let [msp (neko.data/like-map sp)]
              (when (contains? msp key)
                (reset! a (neko.data/get-arbitrary msp key))))
            (catch java.lang.Exception e
              (to a "Preference" key "couldn't be read, disregarding."))))
     (add-watch a :shared-preferences-save-tracker
                (partial watch sp)))))

(defn initialize-preferences [^Context c]
  (ld "Initializing get-shared-preferences...")
  (reset! sp (data/get-shared-preferences c "droidmage_global" :private))
  (ld "Adding " (count @preferences) " preferences")
  (doseq [p @preferences]
    (track-and-set! p)))

(defmacro defpreference
  "Define a shared preference, i.e., an atom whose value is saved
  between sessions. Defines a var with `name`, whose value is an atom
  containing `value`, optionally including a `doc`string. Then, if
  this variable is present in `sp`, set the atom's value to the value
  saved in `sp`.

  By default, whenever the value of this atom changes, the
  corresponding value saved in `sp` is also updated.
  Additional keyword arguments accepted are:

  :version -- Change this number to ignore previously saved values.
  :key     -- The key to use in `sp`, defaults to \"sp/namespace/name/version\".
  :meta and :validator -- Passed to the atom."
  ;; :shared-prefs -- A SharedPreferences to use instead of `sp`.
  ([name value]
   `(defpreference ~name nil ~value))
  ([name doc value & rest]
   (if (odd? (count rest))
     `(defpreference ~name nil ~doc ~value ~@rest)
     (let [[& {:keys [key version meta validator]}] rest
           sp-key (or key (str "sp/" *ns* "/" name "/" version))]
       `(do (def ~name ~@(if doc [doc])
              (atom ~value
                    :meta ~(into {:sp-key sp-key} meta)
                    :validator ~validator))
            (swap! preferences conj ~name))))))

;; (defprotocol MapLike
;;   (like-map [this]))

;; (extend-protocol MapLike
;;   java.util.Collection
;;   (like-map [c]
;;     (if (map? c)
;;       c
;;       (into {} c)))
  
;;   Intent
;;   (like-map [i]
;;     (if-let [bundle (.getExtras i)]
;;       (MapLikeBundle. bundle)
;;       {}))
  
;;   SharedPreferences
;;   (like-map [sp]
;;     (MapLikeHashMap. (.getAll sp)))
  
;;   nil
;;   (like-map [_] {}))
