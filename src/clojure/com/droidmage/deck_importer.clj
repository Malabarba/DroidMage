(ns com.droidmage.deck-importer
  (:require [clojure.string :as s])
  (:use com.droidmage.iop)
  (:import mage.cards.CardImpl
           mage.cards.decks.DeckCardInfo))

(defn deckcardinfo-to-cardinfo [^DeckCardInfo card]
  (.findCard mage.cards.repository.CardRepository/instance
             (.getSetCode card) (.getCardNum card)))


;; (mage.cards.repository.CardInfo.
;;  (CardImpl/createCard "mage.sets.worldwake.StoneforgeMystic"))
;; ;; (cardinfo-as-map)
;; (def mystic
;;   (mage.cards.repository.CardInfo.
;;    (CardImpl/createCard "mage.sets.worldwake.StoneforgeMystic")))

;; ;; "mage.sets.zendikar.AdventuringGear"

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
