(ns com.droidmage.server
  (:require [clojure.string   :as s]
            [com.droidmage.chat :as chat]
            [com.droidmage.server-list :as sl]
            [com.droidmage.view :as v]
            [neko.log :as l])
  (:use [com.droidmage.toast]
        [neko.data      :only [like-map]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]])
  (:import java.lang.Exception
           mage.interfaces.ServerState
           mage.utils.MageVersion
           org.mage.network.Client
           org.mage.network.interfaces.MageClient))

(def mage-version (MageVersion. 1 4 0 "v0" ""))
(def client-error (atom []))
(def client-message (atom []))
(def server-state (atom nil))
(def server-client (atom nil))

(defn make-client [state-atom]
  (proxy [MageClient] []
    (inform [msg type]
      (ld "inform: " type " ::" msg))
    (receiveChatMessage [chat-id msg]
      (chat/add-message chat-id msg)
      (ld "receiveChatMessage: " chat-id ":: " msg))
    (receiveBroadcastMessage [msg]
      (swap! client-message conj msg)
      (ld "receiveBroadcastMessage: " msg))
    (clientRegistered [state]
      (swap! state-atom assoc :server-state state)
      (ld "clientRegistered: " state))
    (getServerState [] (:server-state @state-atom))
    (connected [msg]
      (ld "ClientConnected: " msg))))

;; (def fut (future (.disconnect @server-client)))

;; (defn connect [^Activity a user server]
;;   (let [intent (android.content.Intent. a com.droidmage.ServerActivity)]
;;     (into-bundle intent (assoc server :user user))
;;     (.startActivity a intent)))

;; (swap! *game-list* conj "echo")
;; (require 'clojure.tools.nrepl.transport)

(defn main-layout [act]
  [:linear-layout {:orientation :vertical
                   :layout-width :match-parent
                   :gravity :center}
   [:text-view {:text "Connecting..."}]])

(defn failed-layout [act]
  [:linear-layout {:orientation :vertical
                   :layout-width :match-parent
                   :gravity :center}
   [:text-view {:text "Connection Failed. :("}]])


(defn join-chat [^Client client room-id]
  (try (java.lang.Thread/sleep 3000)
       (ld "Woke up, getting chat-id." )
       (when-let [chat-id (.getRoomChatId client room-id)]
         (ld "chat-id:" chat-id)
         (ld "joiningChat: ")
         (.joinChat client chat-id)
         chat-id)
       (catch java.lang.Exception e
         (le "EXCEPTION!!! " :exception e)
         false)))

(defn connect
  "`callback` should take two arguments, where only one will be
  non-nil. If connection was successful, the first argument is a map
  for the connection. If it failed, the second argument is the
  exception."
  [user {:keys [address port] :as server} callback]
  ;; (into @sl/current-server {:user "ABUa"})
  (try
    (let [connection-atom (atom {:server server})
          client (Client. (make-client connection-atom))]
      (swap! connection-atom assoc :client client)
      (add-watch connection-atom :client-registered-watch
                 (fn [_key _ref _old
                     {:keys [^ServerState server-state] :as new-state}]
                   (ld "Changed: " _ref _old new-state)
                   (when server-state
                     (remove-watch connection-atom :client-registered-watch)
                     (let [room-id (.getMainRoomId server-state)]
                       (ld "main-id:" room-id)
                       (swap! connection-atom assoc :main-room room-id)
                       (future
                         (when-let [chat-id (join-chat client room-id)]
                           (swap! connection-atom assoc :main-chat chat-id))
                         (callback connection-atom nil))))))
      
      (future
        (try (when-not (.connect client user address port mage-version)
               (callback nil "Failed without explanation."))
             (catch Exception e
               (le "Failed Connection: " :exception e)
               (callback nil e)))))
    (catch Exception e
      (le "Failed Connection: " :exception e)
      (callback nil e))))

;; (defactivity com.droidmage.ServerActivity
;;   :key :server
;;   :on-create
;;   (fn [^Activity this bundle]
;;     (keep-screen-on this)

;;     (connect)))
