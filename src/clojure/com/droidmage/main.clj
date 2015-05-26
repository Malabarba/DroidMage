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
                   :gravity :center-horizontal}
   [:text-view {}]
   [:text-view {}]
   [:linear-layout {:orientation :horizontal
                    :layout-width :match-parent
                    :gravity :center}
    [:text-view {:layout-weight 25}]
    (v/make-text-input (fn [view _action _event]
                         (server/connect act (v/get-text view) @sl/current-server))
                       {:hint "Username",  :id ::username,
                        :layout-weight 50, :gravity :center
                        :single-line true})
    [:text-view {:layout-weight 25}]]
   [:text-view {}]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:text "Server: "}]
    [:button {:text (:name @sl/current-server),
              :id ::server-button,
              :on-click sl/show-server-picker}]]
   [:text-view {}]
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
    (v/set-editor-action!
     (*a) ::username
     (fn [view _action _event]
       (server/connect (*a) (v/get-text view) @sl/current-server)))
    ;; (let [{:keys [user ]}])
    (sl/update-server-list this)))

;; (let [lay (neko.ui/make-ui (*a) (main-layout (*a)))]
;;   (v/set-editor-action!
;;    lay ::username
;;    (fn [view _action _event]
;;      (server/connect (*a) (v/get-text view) @sl/current-server))
;;    android.view.inputmethod.EditorInfo/IME_ACTION_GO)
;;   (v/set-layout! (*a) (constantly lay)))
