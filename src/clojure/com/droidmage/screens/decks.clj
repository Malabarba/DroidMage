(ns com.droidmage.screens.decks
  (:require [com.droidmage.deck :as deck]
            [com.droidmage.view :as v]
            [neko.ui :as ui])
  (:use [com.droidmage.shared-preferences :only [defpreference initialize-preferences]]
        [com.droidmage.toast]
        [neko.ui.adapters :only [ref-adapter]]
        [neko.debug     :only [*a keep-screen-on ui-e]]
        [neko.threading :only [on-ui]])
  (:import android.app.Activity
           android.view.View
           java.io.File))

(defn decklist [^Activity a]
  (let [dir (File. (str (.getFilesDir a) "/decks"))]
    (if-not (.exists dir)
      (to a "Directory" (str (.getFilesDir a) "/decks") "doesn't exist!")
      (if-let [files (seq (.listFiles dir))]
        (map deck/import-deck files)
        (to a "No decks in directory" (str (.getFilesDir a) "/decks"))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Layout
(declare screen-layout)
(defn deck-as-layout-element
  [^Activity a {:keys [name cards sideboard author] :as deck}]
  [:linear-layout {:orientation :horizontal, :id-holder true
                   :padding 10, :padding-top 20
                   :layout-width :match-parent, :gravity :center
                   :on-click
                   (fn [& _]
                     (reset! deck/current-deck deck)
                     (v/set-layout! a screen-layout)
                     (to a (str "Cards: " (into [] cards))))}
   [:text-view {:layout-weight 70, :text name
                :text-color android.graphics.Color/WHITE}]
   [:text-view {:layout-weight 30, :text author
                :text-size 8, :gravity :top-right}]])

(defn screen-layout [^Activity a]
  (on-ui (v/set-action-bar!
          a {:title "Decks list", :subtitle (:name @deck/current-deck)
             :display-options :show-title}))
  
  (into [:linear-layout {:orientation :vertical
                         :padding-top 30
                         :padding-bottom 30
                         :id-holder true
                         :layout-width :match-parent
                         :gravity :left}]
        (map (partial deck-as-layout-element a) (decklist a))))

(defn make-deck-adapter [^Activity a filter]
  (ref-adapter
   (fn [context] [:linear-layout {}])
   (fn [position ^View parent _ card]
     (.setView parent (deck/deck-card-layout card)))
   deck/current-deck
   filter))

(defn current-deck-as-layout [^Activity a]
  (neko.ui/make-ui
   a [:linear-layout {:orientation :vertical}
      [:text-view {:text "Main Deck"}]
      [:list-view {:padding-left 10,
                   :adapter (make-deck-adapter a :cards)}]
      [:text-view {:text "Sideboard", :padding-top 10}]
      [:list-view {:padding-left 10,
                   :adapter (make-deck-adapter a :sideboard)}]]))
