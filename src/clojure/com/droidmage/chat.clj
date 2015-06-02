(ns com.droidmage.chat
  (:require [clojure.string :as s]
            [com.droidmage.view :as v]
            [neko.listeners.text-view :as tl])
  (:use com.droidmage.iop
        com.droidmage.toast
        [neko.ui.adapters :only [ref-adapter]])
  (:import android.app.Activity
           org.mage.network.Client))

(defobjectmap msg-as-map 
  mage.view.ChatMessage
  :keyname {getMessage :text})

(def ^:dynamic *chats* (atom {}))

(defn add-message [id message]
  "Convert `message` to a map and append to chat `id`."
  (let [msg (msg-as-map message)]
    (swap! *chats* #(let [chat (conj (vec (% id)) msg)]
                      (assoc % id chat)))))

(defn make-chat-adapter [id]
  (add-message id {:time "" :text "Connected"})
  (ref-adapter
   (fn [_]
     [:linear-layout {:id-holder true}
      [:text-view {:id ::chat-message
                   :text-size 24}]])
   (fn [position parent _ {:keys [text username time]}]
     (v/set-text parent ::chat-message
                 (if username
                   (str time " " username ":" text)
                   (str time " " text))))
   *chats*
   #(% id)))

(def id (atom nil))

(defn chat-layout [{:keys [^Client client]} id]
  (reset! @#'id id)
  [:linear-layout {:orientation :vertical
                   :id-holder true
                   :layout-width :match-parent
                   :gravity :bottom}
   [:list-view {:adapter (make-chat-adapter id)
                :background-color android.graphics.Color/BLACK}]
   (v/make-text-input (fn [view _action _event]
                        (.sendChatMessage client id (v/get-text view))
                        (v/set-text view ""))
                      {:id ::chat-box
                       :padding [2 :dp]
                       :layout-width :match-parent
                       :ime-options android.view.inputmethod.EditorInfo/IME_ACTION_SEND
                       :single-line false})])

