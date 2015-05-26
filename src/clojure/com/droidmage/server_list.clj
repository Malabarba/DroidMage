(ns com.droidmage.server-list
  (:require [clojure.string :as s]
            [com.droidmage.view :as v])
  (:use [neko.threading :only [on-ui]]
        [com.droidmage.toast])
  (:import (android.app Activity)))

(def known-servers-url "http://176.31.186.181/files/server-list.txt")
;;; Provide a default value just in case. It will be updated as soon
;;; as the app starts anyway.
(def known-servers
  (atom
   [{:name "XMage.de 1"    :desc "(Europe/Germany) fast" :address "xmage.de"              :port 17171}
    {:name "woogerworks"   :desc "(North America/USA)"   :address "xmage.woogerworks.com" :port 17171}
    {:name "XMage.info 1"  :desc "(Europe/France) slow"  :address "176.31.186.181"        :port 17171}
    {:name "XMage.info 2"  :desc "(Europe/France) slow"  :address "176.31.186.181"        :port 17000}
    {:name "IceMage"       :desc "(Europe/Netherlands)"  :address "ring0.cc"              :port 17171}
    {:name "Seedds Server" :desc "(Asia)"                :address "115.29.203.80"         :port 17171}]))

(def current-server
  (atom {:name "local" :desc "Mine!"
         :address "192.168.1.88" :port 17171}))

(defn show-server-picker [act-or-view]
  (on-ui
   (let [a (if (instance? Activity act-or-view)
             act-or-view
             (.getContext act-or-view))
         candidates (map (fn [{:keys [name desc address port]}]
                           (str name "\n      "
                                desc "\n      "
                                address ":" port))
                         @known-servers)]
     (v/show-list-picker
      a candidates
      (fn [pos]
        (reset! current-server ((vec @known-servers) pos))
        (v/set-text a ::server-button (:name @current-server)))))))

(defn server-line-to-map [line]
  (let [[name desc ad port]
        (rest (re-find #"^([^(]+[^ ]) +(\(.*[^ ]) *:([^ ]+):([^ ]+)$" line))]
    {:name name, :desc desc, :address ad, :port (Integer/parseInt port)}))

(defn update-server-list [^Activity a]
  (future
    (let [newval (->> (slurp known-servers-url)
                      (s/split-lines)
                      (map server-line-to-map)
                      (remove #(= (:name %) "localhost")))]
      (when-not (= @known-servers
                   (swap! known-servers #(vec (distinct (into % newval)))))
        (to a "Server list updated." :short)))))
