(ns com.droidmage.server
  (:require [com.droidmage.view :as v]
            [com.droidmage.chat :as chat]
            [com.droidmage.server-list :as sl]
            [clojure.string   :as s])
  (:use [com.droidmage.toast]
        [neko.data      :only [like-map]]
        [neko.activity  :only [defactivity]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]])
  (:import mage.utils.MageVersion
           org.mage.network.Client
           (android.os Bundle)
           (android.app Activity)
           (android.app DatePickerDialog DatePickerDialog$OnDateSetListener)
           (android.app AlertDialog AlertDialog$Builder)
           (android.app DialogFragment)))

(def mage-version (MageVersion. 1 4 0 "v0" ""))
(def client-error (atom []))
(def client-message (atom []))
(def server-state (atom nil))

(defn make-client [^Activity a]
  (proxy [org.mage.network.interfaces.MageClient] []
    (inform [msg type]
      (to a "inform: " type " ::" msg))
    (receiveChatMessage [chat-id msg]
      (chat/add-message chat-id msg)
      (to a "receiveChatMessage: " chat-id ":: " msg))
    (receiveBroadcastMessage [msg]
      (swap! client-message conj msg)
      (to a "receiveBroadcastMessage: " msg))
    (clientRegistered [state]
      (reset! server-state state)
      (to a "clientRegistered: " state))
    (getServerState [] @server-state)
    (connected [msg]
      (to a "ClientConnected: " msg))))

;; (def client (Client. (make-client)))

(defn add-to-bundle [b key v]
  (let [k (if (keyword? key) (name key) (str key))]
    (cond
      (instance? android.content.Intent b) (.putExtra b k v)
      (string? v) (.putCharSequence b k v)
      (integer? v) (.putShort b k v)))
  b)

(defn into-bundle
  ([m] (into-bundle (Bundle.) m))
  ([bun m]
   (reduce (fn [b [k v]] (add-to-bundle b (name k) v))
           bun m)))

(defn connect [^Activity a user server]
  (let [intent (android.content.Intent.
                a
                (resolve 'com.droidmage.ServerActivity))]
    (into-bundle intent (assoc server :user user))
    (.startActivity a intent)))


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

(defactivity com.droidmage.ServerActivity
  :key :server
  :on-create
  (fn [this bundle]
    (keep-screen-on this)
    (v/set-layout! this main-layout)
    (let [{:keys [user address port]} (like-map (.getIntent (*a)))
          client (make-client this)]
      (future
        (if (.connect client user address port mage-version)
          (let [main-id (.getMainRoomId @server-state)
                chat-id (.getRoomChatId client main-id)]
            (swap! (.state this) assoc :client client)
            (.joinChat client chat-id)
            (v/set-layout! this chat/chat-layout chat-id))
          (v/set-layout! this failed-layout))))))

;; (l/d "Some log string" {:foo 1, :bar 2})
;; (l/i "Logging to custom tag" [1 2 3] :tag "custom")
;; (l/e "Something went wrong" [1 2 3])
;; (l/d "oaksdoksd")
;; (on-ui (neko.log/e "Some log string" {:foo 1, :bar 2}))
;; (android.util.Log/d "tag" "a message")
