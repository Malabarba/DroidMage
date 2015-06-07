(ns com.droidmage.view
  (:require [clojure.string :as s]
            [neko.context :as context]
            [neko.listeners.dialog :as dl]
            [neko.listeners.text-view :as tl])
  (:use [neko.activity  :only [set-content-view!]]
        [neko.find-view :only [find-view]]
        [neko.threading :only [on-ui]]
        [neko.ui.mapping :only [defelement]]
        [neko.ui        :only [config make-ui]])
  (:import (android.app Activity)
           (android.app AlertDialog AlertDialog$Builder)
           (android.app DialogFragment)
           ;; android.support.v4.widget.DrawerLayout
           android.view.ViewGroup
           android.view.inputmethod.EditorInfo
           (android.widget TextView FrameLayout)))

(defelement  :progress-bar
  :classname android.widget.ProgressBar
  :inherits  :view
  :constructor-args [nil android.R$attr/progressBarStyleLarge]
  :traits    []
  :values    {:visible android.view.View/VISIBLE
              :gone android.view.View/GONE
              :invisible android.view.View/INVISIBLE})

(defelement :input-text
  :inherits :edit-text
  :values {:number      EditorInfo/TYPE_CLASS_NUMBER
           :datetime    EditorInfo/TYPE_CLASS_DATETIME
           :text        EditorInfo/TYPE_CLASS_TEXT
           :phone       EditorInfo/TYPE_CLASS_PHONE
           :go          EditorInfo/IME_ACTION_GO
           :done        EditorInfo/IME_ACTION_DONE
           :unspecified EditorInfo/IME_ACTION_UNSPECIFIED
           :send        EditorInfo/IME_ACTION_SEND
           :search      EditorInfo/IME_ACTION_SEARCH
           :previous    EditorInfo/IME_ACTION_PREVIOUS
           :next        EditorInfo/IME_ACTION_NEXT}
  :attributes {:input-type EditorInfo/TYPE_CLASS_TEXT})

(defn make-text-input [callback attr-map]
  [:input-text
   (into (into {:ime-options (if callback :go :next)}
               (if callback
                 {:on-editor-action-listener
                  (tl/on-editor-action-call callback)}))
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

(defn set-text
  ([view s]
   (on-ui
    (if (instance? android.widget.TextView view)
      (.setText ^android.widget.TextView view
                ^java.lang.String s)
      (config view :text s))))
  ([a view-name s]
   (set-text (find-view a view-name) s)))


(defn get-view-children
  "Get all children of this view."
  [^ViewGroup view]
  (map #(.getChildAt view %) (range (.getChildCount view))))

(defn list-picker [^Activity a items callback]
  "items is a list of strings.
  callback is a function that will be called with the postion of the
  selected item."
  (proxy [DialogFragment] []
    (onCreateDialog [savedInstanceState]
      (-> (AlertDialog$Builder. a)
          (.setItems
           (into-array String items)
           ^android.content.DialogInterface$OnClickListener
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

(defn prompt-for-inputs [^Activity a ^String title
                         callback attr]
  (show-picker
   a (let [text-views (map #(make-text-input nil (into {:layout-width :fill} %))
                           attr)
           ^android.widget.LinearLayout
           view (make-ui a `[:linear-layout {:padding 10, :orientation :vertical}
                             ~@text-views])]
       (proxy [DialogFragment] []
         (onCreateDialog [savedInstanceState]
           (on-ui
            (-> (AlertDialog$Builder. a)
                (.setView view)
                (.setTitle title)
                (.setNegativeButton
                 "Cancel" ^android.content.DialogInterface$OnClickListener
                 (dl/on-click))
                (.setPositiveButton
                 "OK" ^android.content.DialogInterface$OnClickListener
                 (dl/on-click 
                  (callback (map get-text (get-view-children view)))))
                (.create))))))))


(defn set-layout! [a layout & args]
  "Call set-content-view! on activity a with (layout a).
Extra args are passed to layout along with a."
  ;; (let [ui-layout (make-ui a (apply layout a args))]
  ;;   (neko.threading/on-ui
  ;;    (if-let [^View view (:screen-view @(.state a))]
  ;;      (.setView view ui-layout)
  ;;      (neko.activity/set-content-view! a ui-layout))))
  (neko.threading/on-ui
   (neko.activity/set-content-view! a (apply layout a args))))

(defn set-action-bar!
  "Configures activity's action bar according to the attributes
  provided in key-value fashion. For more information,
  see `(describe :action-bar)`."
  [^Activity activity, attributes-map]
  (let [action-bar (.getActionBar activity)]
    (.removeAllTabs action-bar)
    (neko.ui/apply-attributes :action-bar action-bar attributes-map {})))

(defmacro make-button [text pad enabled & [head & tail :as body]]
  `[:linear-layout ~(into {:orientation :horizontal}
                          (if pad {:padding pad}))
    [:button {:text ~text,
              :padding 20,
              :enabled ~enabled
              :on-click
              ~(if (and (not tail) (symbol? head))
                 head
                 `(fn [~'button] ~@body))}]])

(defn hide-keyboard [^Activity a]
  (let [^android.view.inputmethod.InputMethodManager
        imm (context/get-service a :input-method)]
    (.hideSoftInputFromWindow
     imm (.getWindowToken (.getDecorView (.getWindow a))) 0)))
