(ns com.droidmage.screens.home
  (:require [com.droidmage.server :as server]
            [com.droidmage.server-list :as sl]
            [com.droidmage.view :as v]
            [neko.ui :as ui]
            [com.droidmage.chat :as chat])
  (:use [com.droidmage.shared-preferences :only [defpreference initialize-preferences]]
        [com.droidmage.toast]
        [neko.activity  :only [simple-fragment]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]])
  (:import (android.app Activity)))

(def is-connecting (atom false))
(defpreference last-username nil)
(declare attempt-connection)
(declare add-server-dialog)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Layout
(defn screen-layout [^Activity act]
  (let [input-view
        (ui/make-ui
         act (v/make-text-input (fn [view _action _event]
                                  (attempt-connection act (v/get-text view)))
                                {:hint "Username",  :id ::username,
                                 :gravity :center, :layout-width :fill,
                                 :text (or @last-username ""),
                                 :enabled (not @is-connecting)
                                 :single-line true}))]
    [:linear-layout {:orientation :vertical
                     :padding-top 30
                     :padding-bottom 30
                     :id-holder true
                     :layout-width :match-parent
                     :gravity :center-horizontal}

     [:linear-layout {:orientation :horizontal
                      :padding 10, :padding-top 20
                      :id-holder true
                      :layout-width :match-parent
                      :gravity :center}
      [:text-view {:layout-weight 25}]
      [:linear-layout {:layout-weight 50} input-view]
      [:text-view {:layout-weight 25}]]

     [:linear-layout {:padding 10,
                      :orientation :horizontal}
      [:text-view {:text "Server: "}]
      [:button {:text (:name @sl/current-server),
                :id ::server-button,
                :enabled (not @is-connecting)
                :on-click sl/show-server-picker}]]

     (v/make-button
      "Connect" 30 (not @is-connecting)
      (attempt-connection act (v/get-text input-view)))

     (v/make-button "Add Server" nil (not @is-connecting)
                    add-server-dialog)

     [:progress-bar {:visibility (if @is-connecting :visible :invisible)
                     :padding 30}]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Connection
(defn add-server-dialog [^android.view.View button]
  "items is a list of strings.
  callback is a function that will be called with the postion of the
  selected item."
  (let [a (.getContext button)]
    (v/prompt-for-inputs a "Add Server"
                         (fn [[n ad p]]
                           (try (let [server {:name n :desc "" :address ad
                                              :port (Integer/parseInt p)}]
                                  (swap! sl/known-servers #(vec (distinct (conj % server))))
                                  (reset! sl/current-server server)))
                           (on-ui (v/set-layout! a screen-layout)))
                         [{:hint "Name"}
                          {:hint "Address" :input-type :number}
                          {:hint "Port" :input-type :number}])))

(defn connected-layout
  "Reset the layout of ServerActivity to the proper tabs."
  [a {:keys [name address]} chat-id]
  (ld "Connected Layout: " name address chat-id)
  (on-ui (v/set-action-bar!
          a {:title name, :subtitle address
             :display-options :show-title
             :navigation-mode :tabs
             :tabs [[:tab {:text "Tables"
                           :tab-listener (simple-fragment
                                          a [:text-view {:text "Not Implemented"}])}]
                    [:tab {:text "Chat"
                           :tab-listener (simple-fragment
                                          a (chat/chat-layout a chat-id))}]]}))
  [:relative-layout {}])

(defn post-connect [^Activity a connection exception]
  (reset! is-connecting false)
  (if (seq connection)
    ;; Succeeded.
    (let [{:keys [server chat-id]} connection
          act-state (.state a)
          add-section (:add-section-fn @act-state)]
      (swap! act-state update-in [:connections] conj connection)
      (add-section (str (:name server) " Tables") 
                   connected-layout [server chat-id])
      (connected-layout a server chat-id))
    ;; Failed.
    (do (v/set-layout! a screen-layout)
        (to a "Failed Connection: " (str exception)))))

(defn attempt-connection [^Activity a user]
  (reset! last-username user)
  (v/hide-keyboard a)
  (reset! is-connecting true)
  (v/set-layout! a screen-layout)
  (server/connect user @sl/current-server (partial post-connect a)))

