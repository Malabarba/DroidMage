(ns com.droidmage.main
  (:require [com.droidmage.server :as server]
            [com.droidmage.view :as v]
            [com.droidmage.server-list :as sl]
            [com.droidmage.chat :as chat]
            [clojure.string :as s])
  (:use [neko.activity  :only [defactivity]]
        [com.droidmage.toast]
        [neko.threading :only [on-ui]]
        [neko.debug     :only [*a keep-screen-on ui-e]])
  (:import ;; mage.interfaces.MageClient
   ;; mage.interfaces.MageServer
   ;; mage.remote.Connection
   (java.util Calendar)
   (android.app Activity)))

(defn main-layout [^Activity act]
  [:linear-layout {:orientation :vertical
                   :layout-width :match-parent
                   :gravity :center}
   [:text-view {:layout-weight 10}]
   [:linear-layout {:orientation :horizontal
                    :layout-weight 90
                    :layout-width :match-parent
                    :gravity :center}
    [:text-view {:layout-weight 25}]
    [:edit-text {:hint "Username",  :id ::username,
                 :layout-weight 50, :gravity :center
                 ;; :on-editor-action-listener
                 ;; (fn [view _action _event]
                 ;;   (server/connect act
                 ;;                   (v/get-text view)
                 ;;                   @sl/current-server))
                 }]
    [:text-view {:layout-weight 25}]]
   [:text-view {}]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:text "Server: "}]
    [:button {:text (:name @sl/current-server),
              :id ::server-button,
              :on-click sl/show-server-picker}]]
   [:button {:text "Connect",
             :on-click (fn [_]
                         (server/connect act
                                         (v/get-text act ::username)
                                         @sl/current-server))}]])

(defactivity com.droidmage.MainActivity
  :key :main
  :on-create
  (fn [this bundle]
    (keep-screen-on this)
    (v/set-layout! this main-layout)
    ;; (let [{:keys [user ]}])
    (sl/update-server-list this)))
