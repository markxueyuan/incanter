(ns tryincanter.vocabulary
  (:use [clojure.java.io :only [file]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import
   [edu.mit.jwi IDictionary Dictionary RAMDictionary]
   [edu.mit.jwi.item IIndexWord ISynset IWordID IWord Word POS]
   [edu.mit.jwi.data ILoadPolicy]
   [edu.mit.jwi.morph WordnetStemmer]
   [edu.mit.jwi.item POS Pointer]))

(def dictionary
  (doto (Dictionary. (file "D:/data/dict/"))
    .open))

(defn get-stem
  [word & [part-of-speech]]
  (let [stemmer (WordnetStemmer. dictionary)
        pos (cond (nil? part-of-speech) 0
                  (get #{"n" "v" "a" "r" "s"} (name part-of-speech))
                  (get (name part-of-speech) 0)
                  :else 0)]
    (.findStems stemmer word (POS/getPartOfSpeech pos))))

(defn if-exist
  [word]
  (let [POSs (POS/values)
        index-words (map #(.getIndexWord dictionary word %) POSs)]
    (not (empty? (remove nil? index-words)))))


(defn tokenize
  [text]
  (map string/lower-case (re-seq #"\w+" text)))


;(def middle "D:/data/primitive_words/middle.txt")

;(def high "D:/data/primitive_words/high.txt")

;(def college "D:/data/primitive_words/college.txt")

;(def primitive (->> [middle high college]
;                    (mapcat (comp tokenize slurp))
;                    set
;                    vec
;                    sort
;                    (remove #(re-find #"\d+" %))))


;(def primitive-map (reduce #(assoc %1 %2 0) {} primitive))



;(with-open [f-out (io/writer "D:/data/vocabulary.json")]
;  (json/write primitive-map f-out))




(defn add-word
  [word category]
  (let [voc (json/read-str (slurp "D:/data/vocabulary.json")
               :value-fn (fn [key val] val)
               :key-fn name)]
    (if (nil? (get voc word))
      (with-open [f-out (io/writer "D:/data/vocabulary.json")]
        (do (json/write (assoc voc word category) f-out) (println word "is added successfully!")))
      (println word "already exists!"))))

;(add-word "a" 0)

(defn addFamiliarWord
  [word]
  (add-word word 0))

(defn addNewWord
  [word]
  (add-word word 1))

;(addNewWord "zen")

(defn changeMode
  [word]
  (let [voc (json/read-str (slurp "D:/data/vocabulary.json")
                           :value-fn (fn [key val] val)
                           :key-fn name)]
  (cond
   (nil? (get voc word)) (println word "does not exist yet!")
   (= 0 (get voc word)) (with-open [f-out (io/writer "D:/data/vocabulary.json")]
                          (do (json/write (assoc voc word 1) f-out) (println word "is switched from familiar to new!")))
   (= 1 (get voc word)) (with-open [f-out (io/writer "D:/data/vocabulary.json")]
                          (do (json/write (assoc voc word 0) f-out) (println word "is switched from new to familiar!"))))))
;(changeMode "xue")

(defn filterWorkingWords
  [file]
  (let [voc (json/read-str (slurp "D:/data/vocabulary.json")
               :value-fn (fn [key val] val)
               :key-fn name)
        lst (->> file slurp tokenize distinct
                 (remove #(re-find #"\d+" %))
                 (mapcat #(get-stem %))
                 (filter if-exist)
                 distinct)
        brand-new (-> (filter #(nil? (get voc %)) lst) sort)
        review-lst (-> (filter #(= 1 (get voc %)) lst) sort)
        c1 (count brand-new)
        c2 (count review-lst)]
    (do
      (with-open [f-out (io/writer "D:/data/brand_new_list.txt" :append true)]
        (binding [*out* f-out]
          (doseq [a brand-new]
            (println a))
          (println "-------------------")))
      (with-open [f-out (io/writer "D:/data/review_list.txt" :append true)]
        (binding [*out* f-out]
          (doseq [a review-lst]
            (println a))
          (println "-------------------")))
      (println c1 "brandnew words are sifted out.")
      (println c2 "words are added for reviewing."))))

(filterWorkingWords "D:/data/eng.srt")
