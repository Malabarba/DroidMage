(ns com.droidmage.deck
  (:require [clojure.string :as s]
            [com.droidmage.card :as card]
            [lazy-map.iop :as iop]
            [com.droidmage.shared-preferences :refer [defpreference]])
  (:import mage.cards.CardImpl
           mage.cards.decks.DeckCardInfo
           mage.cards.decks.importer.DckDeckImporter
           mage.cards.decks.importer.DeckImporterUtil))

;; (def a-deck
;;   (let [file "/home/artur/Git-Projects/mage/Mage.Client/release/sample-decks/2011/Zen_M11_SoM/Boros.dck"]
;;     (DeckImporterUtil/importDeck file)))

(iop/extend-lazy-map mage.cards.decks.DeckCardLists)
(iop/extend-lazy-map mage.cards.decks.DeckCardInfo)


;;; Decks
(defpreference current-deck {:name "None selected"})

(defn deckcardinfo-to-cardinfo [card]
  (let [{:keys [set-code card-num]} (iop/to-lazy-map card)]
    (iop/to-lazy-map
     (.findCard mage.cards.repository.CardRepository/instance
                set-code card-num))))

(defn import-deck [file]
  (iop/to-lazy-map
   (DeckImporterUtil/importDeck file)))

(defn deck-card-layout
  [{:keys [quantity] :as card}]
  [:linear-layout {:orientation :horizontal, :layout-width :match-parent
                   :padding 10}
   [:text-view {:text-size 18, :text quantity
                :gravity :center-vertical}]
   (card/card-layout (deckcardinfo-to-cardinfo card))])

;; (defmacro define-deck-importer [class]
;;   `(defn ~(symbol (str "make-" (name (lowercase class)))) [^Activity a file]
;;      (let [~(alter-meta 'importer :tag class)
;;            (proxy [~class] []
;;              (importDeck [filename]
;;                (with-open [in (.open (.getAssets a) filename)]
;;                  (let [decklist (mage.cards.decks.DeckCardLists.)
;;                        ^DeckImporter imp this]
;;                    (doseq [line (s/split-lines (slurp in))]
;;                      (.readLine imp ^String (s/trim line) decklist))
;;                    (when-let [e (.getErrors imp)]
;;                      (when-not (= e "")
;;                        (to a e) (le e) (println e)))
;;                    decklist))))]
;;        (.importDeck importer file))))

;; (define-deck-importer DckDeckImporter)
;; (define-deck-importer TxtDeckImporter)
;; (define-deck-importer MWSDeckImporter)
;; (define-deck-importer DecDeckImporter)

;; (def deck-importers
;;   {"dck" (make-dckdeckimporter)
;;    "txt" (make-txtdeckimporter)
;;    "mws" (make-mwsdeckimporter)
;;    "dec" (make-decdeckimporter)})




;; (mage.cards.repository.CardInfo.
;;  (CardImpl/createCard "mage.sets.worldwake.StoneforgeMystic"))
;; ;; (iop/to-lazy-map)
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
