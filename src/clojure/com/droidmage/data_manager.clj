(ns com.droidmage.data-manager
  (:use com.droidmage.toast)
  (:import android.app.Activity
           java.io.File
           java.lang.Exception
           java.lang.Integer
           java.util.ArrayList
           java.util.Collection
           java.util.HashMap
           mage.util.ClassScanner)
  (:require [clojure.string :as s]))

(def databases-ready (atom false))

(defn extract-file!
  "Extract a file from the assets directory to the external data
  directory. Parent directories are created as needed."
  ([^Activity a filename] (extract-file! a filename true))
  ([^Activity a filename overwrite]
   (let [out-file (File. (str (.getFilesDir a) "/" filename))
         parent-path (.getParent out-file)
         parent (when parent-path (File. parent-path))]
     (when (or (not (.exists out-file))
               overwrite)
       (when parent
         (.mkdirs parent))
       (spit out-file (slurp (.open (.getAssets a) filename)))))))

(def db-version 1)

(defn extract-databases! [^Activity a & [force]]
  (let [db-version-file (str (.getFilesDir a) "/db-version")
        version (try (Integer/parseInt (slurp db-version-file))
                     (catch Exception e))]
    (if (or force
            (not version)
            (< version db-version))
      (try (ld "Extracting database files.")
           (extract-file! a "db/cards.h2.mv.db")
           (extract-file! a "db/cards.h2.trace.db")
           (spit db-version-file (str db-version))
           (catch Exception e
             (le "EXCEPTION!!! " :exception e)
             (to a e)))
      (li "Database files already up-to-date, version:" version))
    (.initialize mage.cards.repository.CardRepository/instance false)
    (.initialize mage.cards.repository.ExpansionRepository/instance false)))

(defn populate-class-scanner-package-map! [^Activity a]
  (try
    (let [^HashMap hm ClassScanner/packageMap]
      (if (> (.size hm) 3)
        (ld "ClassScanner already populated.")
        (do (doseq [line (s/split-lines (slurp (.open (.getAssets a) "data/packagemap")))]
              (let [[package & ^Collection classes]
                    (s/split line #" ")]
                (.put hm package (ArrayList. classes))))
            (ld "Done with populating ClassScanner"))))
    (catch Exception e
      (le "Can't populate sets database" :exception e))))
