(ns com.droidmage.shared-preferences
  (:require [neko.data :as data])
  (:use [com.droidmage.toast])
  (:import (android.content SharedPreferences SharedPreferences$Editor Context)
           neko.App))

(defprotocol GenericExtrasKey
  "If given a string returns itself, otherwise transforms a argument
  into a string."
  (generic-key [key]))

(extend-protocol GenericExtrasKey
  String
  (generic-key [s] s)

  clojure.lang.Keyword
  (generic-key [k] (.getName k)))

(def ^:private sp-access-modes {:private Context/MODE_PRIVATE
                                :world-readable Context/MODE_WORLD_READABLE
                                :world-writeable Context/MODE_WORLD_WRITEABLE})

(defn get-shared-preferences
  "Returns the SharedPreferences object for the given name. Possible modes:
  `:private`, `:world-readable`, `:world-writeable`."
  ([name mode]
   (get-shared-preferences App/instance name mode))
  ([^Context context, name, mode]
   {:pre [(or (number? mode) (contains? sp-access-modes mode))]}
   (let [mode (if (number? mode)
                mode (sp-access-modes mode))]
     (.getSharedPreferences context name mode))))

(defn ^SharedPreferences$Editor assoc-arbitrary!
  "Puts `value` of an arbitrary Clojure data type into given
  SharedPreferences editor instance. Data is printed into a string and
  stored as a string value."
  [^SharedPreferences$Editor sp-editor key value]
  (let [key (generic-key key)]
    (.putString sp-editor key (pr-str value))))

(defn get-arbitrary
  "Gets a string by given key from a SharedPreferences
  HashMap (wrapped with `like-map`) and transforms it into a data
  value using Clojure reader."
  [sp-map key]
  (when-let [val (get sp-map key)]
    (read-string val)))


(def sp "SharedPreferences manager for the application." (atom nil))
(def preferences "Set of atoms to keep track of." (atom #{}))

(defn- watch
  "Watch function for saving preferences whenever they are edited."
  [^SharedPreferences sp _key ref old new]
  (when-not (= old new)
    (when-not sp
      (throw (java.lang.Exception
              (str "shared-preferences not initialized: " sp))))
    (locking sp
      (-> (.edit sp)
          (assoc-arbitrary! (:sp-key (meta ref)) new)
          .commit))))

(defn track-and-set!
  "Set the value of atom `a` according to the value stored in
  `shared-prefs` (if any), then add a watch to it so any changes in
  its value are updated in `shared-prefs`.
  shared-prefs defaults to `sp`.
  The atom must have an `:sp-key` meta property holding a string or a
  keyword."
  ([a]
   (if @sp
     (track-and-set! a @sp)
     (throw (java.lang.Exception "shared-preferences not initialized"))))
  ([a ^SharedPreferences sp]
   {:pre [(instance? clojure.lang.Atom a)
          (:sp-key (meta a))]}
   (let [key (:sp-key (meta a))]
     (locking sp
       (try (let [msp (data/like-map sp)]
              (when (.containsKey msp key)
                (reset! a (get-arbitrary msp key))))
            (catch java.lang.Exception e
              (le "Preference" key "couldn't be read, disregarding"))))
     (add-watch a :shared-preferences-save-tracker
                (partial watch sp)))))

(defn initialize-preferences
  "Restore the recorded value of all preferences, and configure them
  to be saved when changed. Call this only once per application
  lifetime.

  This function can be invoked with either a SharedPreferences object,
  or a name and a Context (as per `get-shared-preferences`). In the
  latter case, this function will have no effect if preferences have
  already been initialized in this session, unless a third truthy
  argument is provided."
  ([^Context context name]
   (initialize-preferences context name false))
  ([^Context context name force]
   (when (or force (not @sp))
     (initialize-preferences
      (get-shared-preferences context name :private))))
  ([^SharedPreferences shared-prefs]
   (reset! sp shared-prefs)
   (doseq [p @preferences]
     (track-and-set! p shared-prefs))))

(defmacro defpreference
  "Define a preference, i.e., an atom whose value is saved between
  sessions. Defines a var with `name`, whose value is an atom
  containing `value`, optionally including a docstring. The actual
  saving and restoring only takes place once the function
  initialize-preferences is called, which should happen only once per
  application lifetime, so call it in the onCreate of your main
  activity.

  Once the function is called, any previously saved value will
  override `value`, and a watcher will be used to always save the
  value again when the atom is changed.

  Additional keyword arguments accepted are:
  :version -- Change this number to ignore previously saved values.
  :key -- The key to use in `sp`, defaults to \"sp/namespace/name/version\".
  Setting this invalidates the version argument.
  :meta and :validator -- Passed to the atom."
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
