(ns com.droidmage.server-list
  (:require [clojure.string :as s]
            [com.droidmage.view :as v])
  (:use [neko.threading :only [on-ui]]
        [com.droidmage.shared-preferences :only [defpreference]]
        [com.droidmage.toast])
  (:import (android.app Activity)))

(def known-servers-url "http://176.31.186.181/files/server-list.txt")
;;; Provide a default value just in case. It will be updated as soon
;;; as the app starts anyway.
(defpreference known-servers
  [{:name "XMage.de 1"    :desc "(Europe/Germany) fast" :address "xmage.de"              :port 17171}
   {:name "woogerworks"   :desc "(North America/USA)"   :address "xmage.woogerworks.com" :port 17171}
   {:name "XMage.info 1"  :desc "(Europe/France) slow"  :address "176.31.186.181"        :port 17171}
   {:name "XMage.info 2"  :desc "(Europe/France) slow"  :address "176.31.186.181"        :port 17000}
   {:name "IceMage"       :desc "(Europe/Netherlands)"  :address "ring0.cc"              :port 17171}
   {:name "Seedds Server" :desc "(Asia)"                :address "115.29.203.80"         :port 17171}])

(defpreference current-server
  {:name "local" :desc "Mine!"
   :address "192.168.1.88" :port 17171})

(defn show-server-picker [button]
  (on-ui
   (let [a (.getContext ^android.view.View button)
         candidates (map (fn [{:keys [name desc address port]}]
                           (str name " - " address ":" port
                                "\n      " desc))
                         @known-servers)]
     (v/show-list-picker
      a candidates
      (fn [pos]
        (reset! current-server (if (< pos (count @known-servers))
                                 ((vec @known-servers) pos)
                                 (last @known-servers)))
        (v/set-text button (:name @current-server)))))))

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

;; @Override
;; public boolean onCreateOptionsMenu(Menu menu)
;; {// Inflate the menu; this adds items to the action bar if it is present.
;;  getSupportMenuInflater().inflate(R.menu.main, menu);

;;  mActionMenu = menu;

;;  configureSearchView(menu);
;;  sd.updateMatches(sharedText);
;;  sharedText = "";

;;  updateActionButtons();

;;  return super.onCreateOptionsMenu(menu);
;;  }

;; // Hide and show action buttons depending on the nature of current
;; // tab.
;; public void updateActionButtons()
;; {if (sectionPager != null)
;;  {boolean isDocPage = sectionPager
;;   .tabIsDocPage(actionBar.getSelectedNavigationIndex());
;;   if (mActionMenu != null)
;;   {if (isDocPage)
;;    {if (keptText.equals("")) 
;;     keptText = editSearch.getText().toString();
;;     menuSearch.collapseActionView();
;;     }
;;    mActionMenu.findItem(R.id.menu_search).setVisible(!isDocPage);
;;    mActionMenu.findItem(R.id.zoom_in).setVisible(isDocPage);
;;    mActionMenu.findItem(R.id.zoom_out).setVisible(isDocPage);
;;    mActionMenu.findItem(R.id.share_url).setVisible(isDocPage);
;;    // mActionMenu.findItem(R.id.share_text).setVisible(isDocPage);
;;    mActionMenu.findItem(R.id.close_page).setVisible(isDocPage);
;;    }
;;   }
;;  }
