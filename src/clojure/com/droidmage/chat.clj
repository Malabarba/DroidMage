(ns com.droidmage.chat
  (:require [clojure.string :as s]
            [com.droidmage.view :as v])
  (:use [neko.ui.adapters :only [ref-adapter]])
  ;; (:import mage.utils.MageVersion
  ;;          ;; mage.interfaces.MageServer
  ;;          org.mage.network.Client)
  )

(defn enum-to-keyword [enum]
  (-> (.toString enum)
      (s/lower-case)
      (s/replace "_" "-")
      (keyword)))

(defn msg-to-map [^mage.view.ChatMessage msg] 
  {:text (.getMessage msg)
   :user (.getUsername msg)
   :time (.getTime msg)  
   :type  (enum-to-keyword (.getMessageType msg))
   :color (enum-to-keyword (.getColor msg))})

(def ^:dynamic *chats* (atom {}))

(defn add-message [id message]
  "Convert `message` to a map and append to chat `id`."
  (let [msg (msg-to-map message)]
    (swap! *chats* #(let [chat (conj (vec (% id)) msg)]
                      (assoc % id chat)))))

(defn make-chat-adapter [a id]
  (ref-adapter
   (fn [_]
     [:linear-layout {:id-holder true}
      [:text-view {:id ::chat-message
                   :text-size 24}]])
   (fn [position parent _ [text user time]]
     (v/set-text parent ::chat-message
                 (if user
                   (str time " " user ":" text)
                   (str time " " text))))
   *chats*
   #(% id)))

(defn chat-layout [act id]
  [:linear-layout {:orientation :vertical
                   :id-holder true
                   :layout-width :match-parent
                   :gravity :bottom}
   [:list-view {:adapter (make-chat-adapter act id)}]
   [:edit-text {:id ::chat-box
                :layout-width :match-parent
                :gravity :center
                ;; :on-editor-action-listener
                ;; (fn [view _action _event]
                ;;   (add-message id (v/get-text view)))
                }]])
