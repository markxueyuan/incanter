(ns tryincanter.vocabulary
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [join split]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [incanter.core :as i]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:import
   [edu.mit.jwi IDictionary Dictionary RAMDictionary]
   [edu.mit.jwi.item IIndexWord ISynset IWordID IWord Word POS]
   [edu.mit.jwi.data ILoadPolicy]
   [edu.mit.jwi.morph WordnetStemmer]
   [edu.mit.jwi.item POS Pointer]))

(def dictionary
  (doto (Dictionary. (file "D:/快盘/grandword/dict/"))
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

(defn import-data
  [& files]
  (->> files
       (mapcat (comp tokenize slurp))
       distinct
       (remove #(re-find #"\d+" %))
       sort))


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

;(def gre
;  (->> (slurp "D:/data/GRE.txt")
;     identity
;     (re-seq #"([^\r\n]+)\r")
;     (map second)
;     rest
;     (#(conj % "abacus"))
;     count
;     ))

;(def gre-climax (import-data "D:/data/gre_climax.txt"))

;(def gre-full (concat gre gre-climax))

;(map addNewWord gre-full)

(defn add-word
  [word category]
  (let [voc (json/read-str (slurp "D:/快盘/grandword/voc/vocabulary.json")
               :value-fn (fn [key val] val)
               :key-fn name)]
    (if (nil? (get voc word))
      (with-open [f-out (io/writer "D:/快盘/grandword/voc/vocabulary.json")]
        (do (json/write (assoc voc word category) f-out) (println word "is added successfully!")))
      (println word "already exists!"))))

;(add-word "a" 0)

(defn addFamiliarWord
  [word]
  (add-word (name word) 0))

(defn addNewWord
  [word]
  (add-word (name word) 1))

;(addNewWord "zen")

(defn changeMode
  [word]
  (let [voc (json/read-str (slurp "D:/快盘/grandword/voc/vocabulary.json")
               :value-fn (fn [key val] val)
               :key-fn name)]
  (cond
   (nil? (get voc (name word))) (println word "does not exist yet!")
   (= 0 (get voc (name word))) (with-open [f-out (io/writer "D:/快盘/grandword/voc/vocabulary.json")]
                          (do (json/write (assoc voc (name word) 1) f-out) (println word "is switched from familiar to new!")))
   (= 1 (get voc (name word))) (with-open [f-out (io/writer "D:/快盘/grandword/voc/vocabulary.json")]
                          (do (json/write (assoc voc (name word) 0) f-out) (println word "is switched from new to familiar!"))))))
;(changeMode "xue")
(defn get-definition
  [stem]
  (let [index-words (map #(.getIndexWord dictionary stem %)
                       (POS/values))
        word-ids (mapcat #(.getWordIDs %)
                         (remove nil? index-words))
        word-instances (map #(.getWord dictionary %) word-ids)
        synsets (map #(.getSynset %) word-instances)
        fn-gloss #(.getGloss %)
        fn-POS #(.toString (.getPOS %))
        fn-words (fn [syn] (->> (.getWords syn)
                                (map #(.getLemma %))
                                ;(remove #(= stem %))
                                (join "; ")))
        fn-item #(vector (fn-POS %) (fn-words %) (fn-gloss %))]
    (map fn-item synsets)))


(defn word-combine-definition
  [stem]
  (let [definition (get-definition stem)]
    (into (vector (into
                   (vector stem)
                   (first definition)))
          (map
           #(into (vector "") %)
           (rest definition)))))

;(word-combine-definition "word")

(defn view-word-definition
  [stems]
  (i/view
   (i/dataset ["word" "POS" "synonym" "item"]
              (mapcat
               (partial word-combine-definition)
               stems))))

;(view-word-definition ["people" "ghost"])

(defn what-is-the-time
  []
  (let [fmt (f/formatter (t/default-time-zone) "yyyy-MM-dd HH:mm" "yyyy-MM-dd")]
    (f/unparse fmt (t/now))))


(defn filterWorkingWords
  [file]
  (let [voc (json/read-str (slurp "D:/快盘/grandword/voc/vocabulary.json")
               :value-fn (fn [key val] val)
               :key-fn name)
        lst (->> file slurp tokenize distinct
                 (remove #(re-find #"\d+" %))
                 (remove #{"_"})
                 (mapcat #(get-stem %))
                 (filter if-exist)
                 distinct)
        brand-new (-> (filter #(nil? (get voc %)) lst) sort)
        review-lst (-> (filter #(= 1 (get voc %)) lst) sort)
        ;review-defs (map review-list)
        c1 (count brand-new)
        c2 (count review-lst)]
    (do
      (with-open [f-out (io/writer "D:/快盘/grandword/brand_new_list.txt" :append true)]
        (binding [*out* f-out]
          (println "----------" (what-is-the-time) "---------")
          (doseq [a brand-new]
            (println a))))
      (with-open [f-out (io/writer "D:/快盘/grandword/review_list.txt" :append true)]
        (binding [*out* f-out]
          (println "----------" (what-is-the-time) "---------")
          (doseq [a review-lst]
            (println a))))
      (view-word-definition review-lst)
      (println c1 "brandnew words are sifted out.")
      (println c2 "words are added for reviewing."))))

;(filterWorkingWords "D:/data/inf-itl.eng.srt")

(defn query
  [word]
  (let [stems (get-stem (name word))]
    (if (empty? stems)
      (println "no definition")
      (doseq [stem stems]
        (let [voc (json/read-str (slurp "D:/快盘/grandword/voc/vocabulary.json")
                                 :value-fn (fn [key val] val)
                                 :key-fn name)
              included (cond (= (get voc stem) 1) "new"
                             (= (get voc stem) 0) "familiar"
                             :else "")]
          (println (apply str (repeat 24 "-")) stem (apply str (repeat 27 "-")))
          (println (str (apply str (repeat (- (+ 53 (count stem)) (count included)) " ")) included))
          (doseq [item (get-definition stem)]
            (let [defs (if (re-find #"\"" (last item))
                         (second (re-find #"(.+?)\s\"" (last item)))
                         (last item))
                  cases (re-seq #"\"[^\"]+\"" (last item))]
              (if (empty? (second item))
                (println (first item))
                (println (first item) (str "[" (second item) "]")))
              (println defs)
              (doseq [s cases]
                (println "   " s))
              (println))))))))


(apply str (repeat 24 "-"))

(defmacro high-order
  [word]
  (fn [operate]
    (if (symbol? word)
       (operate (name word))
       (operate word))))

(defmacro ?
  [word]
  ((eval `(high-order ~word)) query))

(defmacro !
  [word]
  ((eval `(high-order ~word)) addFamiliarWord))

(defmacro $
  [word]
  ((eval `(high-order ~word)) addNewWord))

(defmacro %
  [word]
  ((eval `(high-order ~word)) changeMode))

(def & filterWorkingWords)

