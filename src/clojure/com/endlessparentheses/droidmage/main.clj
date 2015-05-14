(ns com.endlessparentheses.droidmage.main
  ;; (:require 
  ;;  [com.endlessparentheses.droidmage.server :as server])
  (:use [neko.activity  :only [defactivity set-content-view!]]
        [neko.notify    :only [toast]]
        [neko.listeners.dialog :as dl]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.ui        :only [config]]
        [neko.find-view :only [find-view]]
        [neko.threading :only [on-ui]]
        [clojure.string :only [join]])
  (:import android.widget.TextView
           (java.util Calendar)
           (android.app Activity)
           (android.app DatePickerDialog DatePickerDialog$OnDateSetListener)
           (android.app AlertDialog AlertDialog$Builder)
           (android.app DialogFragment)))

(defn get-text [a view]
  (str (.getText ^TextView (find-view a view))))

(defn set-text [a view s]
  (on-ui (config (find-view a view) :text s)))

(defn list-picker [a items callback]
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
  ([^Activity a picker]
   (show-picker a picker (java.util.UUID/randomUUID)))
  ([^Activity a ^DialogFragment picker tag]
   (.show picker (.getFragmentManager a) (str tag))))

(def known-servers-url "http://176.31.186.181/files/server-list.txt") 
(def known-servers
  (atom
   [{:name "XMage.de 1" :desc "    (Europe/Germany) fast" :address "xmage.de"              :port "17171"}
    {:name "woogerworks" :desc "   (North America/USA)"   :address "xmage.woogerworks.com" :port "17171"}
    {:name "XMage.info 1" :desc "  (Europe/France) slow"  :address "176.31.186.181"        :port "17171"}
    {:name "XMage.info 2" :desc "  (Europe/France) slow"  :address "176.31.186.181"        :port "17000"}
    {:name "IceMage" :desc "       (Europe/Netherlands)"  :address "ring0.cc"              :port "17171"}
    {:name "Seedds Server" :desc " (Asia)"                :address "115.29.203.80"         :port "17171"}]))
(def current-server (atom (first @known-servers)))

(defn main-layout [act]
  [:linear-layout {:orientation :vertical
                   :layout-width :match-parent
                   :gravity :center}
   [:linear-layout {:orientation :horizontal
                    :layout-width :match-parent
                    :gravity :center}
    [:text-view {:layout-weight 25}]
    [:edit-text {:hint "Username",  :id ::username,
                 :layout-weight 50, :gravity :center}]
    [:text-view {:layout-weight 25}]]
   [:text-view {}]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:text "Server: "}]
    [:button {:text (:name @current-server),
              :id ::server-button,
              :on-click (fn [_]
                          (on-ui
                           (let [candidates (map #(fn [{:keys [name desc address port]}]
                                                    (str name desc "\n"
                                                         address ":" port))
                                                 @known-servers)]
                             (->> (fn [pos]
                                    (reset! current-server ((vec @known-servers) pos))
                                    (set-text act ::server-button (:name @current-server))
                                    (toast act (str pos " " @current-server) :long))
                                  (list-picker act candidates)
                                  (show-picker act)))))}]]
   [:button {:text "Connect",
             ;; :on-click
             ;; (fn [_] (server/connect act @current-server))
             }]])

(defn- server-line-to-map [line]
  (let [[name desc ad port]
        (rest (re-find #"^([^ ]+)(.*):([^ ]+):([^ ]+)$" line))]
    {:name name, :desc desc, :address ad, :port port}))

(defn update-server-list [a]
  (future
    (let [newval (->> (slurp known-servers-url)
                      (clojure.string/split-lines)
                      (map server-line-to-map)
                      (remove #(= (:name %) "localhost"))
                      vec)]
      (when-not (= newval @known-servers)
        (reset! known-servers newval)
        (on-ui (toast a "Server list updated." :short))))))

(defactivity com.endlessparentheses.droidmage.MainActivity
  :key :main
  :on-create
  (fn [this bundle]
    (keep-screen-on this)
    (on-ui
     (set-content-view! this (main-layout this)))
    (update-server-list)))
