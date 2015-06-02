(ns com.droidmage.card
  (:require [clojure.string :as s]
            [neko.log :as l])
  (:use com.droidmage.iop
        com.droidmage.toast)
  (:import android.app.Activity
           mage.cards.CardImpl
           mage.cards.decks.importer.DckDeckImporter
           mage.cards.decks.importer.DeckImporter
           mage.cards.repository.CardInfo
           mage.cards.decks.DeckCardInfo))

(defobjectmap cardinfo-as-map
  mage.cards.repository.CardInfo
  :exclude [getCard getMockCard])

(defobjectmap cardimpl-as-map
  mage.cards.CardImpl
  :exclude [getRules])

(defn deckcardinfo-to-cardinfo [^mage.cards.decks.DeckCardInfo card]
  (.findCard mage.cards.repository.CardRepository/instance
             (.getSetCode card) (.getCardNum card)))

(def rarity-to-color
  {:mythic   android.graphics.Color/RED
   :rare     android.graphics.Color/YELLOW
   :uncommon android.graphics.Color/GRAY
   :common   android.graphics.Color/WHITE})

(defn str-mana-costs
  "Turn a vector of mana symbols into a string of the mana cost."
  [costs]
  (apply str (map #(second (re-find #"\{(.+)\}" %))
                  costs)))

(defn card-layout
  [{:keys [name mana-costs color
           supertypes types sub-types rarity set-code
           power toughness]
    :as card}]
  (into [:linear-layout {:orientation :vertical
                         :layout-width :match-parent
                         :padding 10}
         [:linear-layout {:orientation :horizontal
                          :layout-width :match-parent
                          :padding-bottom 10}
          [:text-view {:layout-weight 80, :text-size 18, :text name}]
          [:text-view {:layout-weight 20, :gravity :right, :text-size 18,
                       :text (str-mana-costs mana-costs)}]]
         [:linear-layout {:orientation :horizontal
                          :layout-width :match-parent}
          [:text-view {:layout-weight 90, :text (str (s/join " " (concat supertypes types))
                                                     (if (seq sub-types)
                                                       (str " - " (s/join " " sub-types))))}]
          [:text-view {:layout-weight 10, :gravity :right,
                       :text-color (rarity-to-color rarity)
                       :text set-code}]]]
        (when toughness
          [[:text-view {:gravity :right, :layout-width :match-parent,
                        :text (str power "/" toughness)}]])))

(defn import-deck [^Activity a file]
  (let [^DckDeckImporter importer
        (proxy [DckDeckImporter] []
          (importDeck [filename]
            (with-open [in (.open (.getAssets a) filename)]
              (let [decklist (mage.cards.decks.DeckCardLists.)
                    ^DeckImporter imp this]
                (doseq [line (s/split-lines (slurp in))]
                  (.readLine imp ^String (s/trim line) decklist))
                (when-let [e (.getErrors imp)]
                  (when-not (= e "")
                    (to a e) (le e) (println e)))
                decklist))))]
    (.importDeck importer file)))

;; (bean
;;  (mage.cards.repository.CardInfo.
;;   (CardImpl/createCard "mage.sets.worldwake.StoneforgeMystic")))
;; (cardinfo-as-map)

;; (def mystic
;;   (mage.cards.repository.CardInfo.
;;    (CardImpl/createCard "mage.sets.worldwake.StoneforgeMystic")))

;; (cardinfo-as-map mystic)

;; "mage.sets.zendikar.AdventuringGear"

;; (mage.sets.zendikar.AdventuringGear. (java.util.UUID/randomUUID))

;; (:object
;;  (first
;;   (map #(card-as-map (deckcardinfo-to-cardinfo %))
;;        (.getCards a-deck))))

;; (def a-deck
;;   (let [file "/home/artur/Git-Projects/mage/Mage.Client/release/sample-decks/2011/Zen_M11_SoM/Boros.dck"]
;;     (DeckImporterUtil/importDeck file)))
;; (card-layout cost)
;; (:class-name cost)
