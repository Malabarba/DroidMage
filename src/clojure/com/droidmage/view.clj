(ns com.droidmage.view
  (:require [clojure.string :as s]
            [neko.listeners.text-view :as tl]
            [neko.listeners.dialog :as dl])
  (:use [neko.threading :only [on-ui]]
        [neko.ui        :only [config make-ui]]
        [neko.find-view :only [find-view]])
  (:import android.widget.TextView
           (android.app Activity)
           (android.app DialogFragment)
           (android.app AlertDialog AlertDialog$Builder)))

(defn make-text-input [callback attr-map]
  [:edit-text 
   (into {:input-type  android.view.inputmethod.EditorInfo/TYPE_CLASS_TEXT
          :ime-options android.view.inputmethod.EditorInfo/IME_ACTION_GO
          :on-editor-action-listener (tl/on-editor-action-call callback)}
         attr-map)])

(defn set-editor-action!
  ([^Activity a view callback]
   (set-editor-action! (find-view a view) callback))
  ([^TextView view callback]
   (.setOnEditorActionListener
    view (tl/on-editor-action-call callback))
   view))

(defn get-text
  ([^TextView view]
   (str (.getText view)))
  ([^Activity a view]
   (str (.getText ^TextView (find-view a view)))))

(defn set-text [a view s]
  (on-ui (config (find-view a view) :text s)))

(defn list-picker [^Activity a items callback]
  "items is a list of strings.
  callback is a function that will be called with the postion of the
  selected item."
  (proxy [DialogFragment] []
    (onCreateDialog [savedInstanceState]
      (-> (AlertDialog$Builder. a)
          (.setItems
           (into-array java.lang.CharSequence items)
           (dl/on-click-call (fn [dialog pos] (callback pos))))
          (.create)))))

(defn show-picker
  ([^Activity a ^DialogFragment picker]
   (show-picker a picker (java.util.UUID/randomUUID)))
  ([^Activity a ^DialogFragment picker tag]
   (.show picker (.getFragmentManager a) (str tag))))

(defn show-list-picker
  ([^Activity a items callback]
   (show-picker a (list-picker a items callback))))

(defmacro set-layout! [a layout & args]
  "Call set-content-view! on activity a with (layout a).
Extra args are passed to layout along with a."
  `(neko.threading/on-ui
    (neko.activity/set-content-view! ~a (~layout ~a ~@args))))
