(ns tryincanter.data
  (:require [incanter.core :as i :refer [$ $= $where $group-by $join $rollup $map $order]]
            [incanter.datasets :as ds]
            [incanter.io :as io]
            [incanter.zoo :as zoo]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.java.io :as jio]
            [incanter.charts :as ch]
            [incanter.stats :as s]
            [clojure.set :refer (union) :as set]
            [clojure.string :as string]
            [clj-time.format :as f]))

;built-in datasets
(def iris (ds/get-dataset :iris))

(i/col-names iris)

(i/nrow iris)

(set ($ :Species iris))

;;;;;;;;;;;;;;;clojure data structure

;vector of vectors

(i/to-dataset 3)

(def matrix-set (i/to-dataset [[1 2 3] [4 5 6]]))

(i/nrow matrix-set)
(i/col-names matrix-set)

;map

(def map-set (i/to-dataset {:a 1 :b 2 :c 3}))

(i/nrow map-set)
(i/col-names map-set)

;maps

;这个真的很low B

(def maps-set (i/to-dataset [{:a 2 :b 3 :c 4} {:a 5 :b 6 :c 7}]))

(i/nrow maps-set)

(i/col-names maps-set)

;vectors of vectors with col-name


(def matrix-set-2
  (i/dataset [:a :b :c] [[1 2 3] [4 5 6]]))

(i/nrow matrix-set-2)

(i/col-names matrix-set-2)

;view

;(i/view iris)

;io

(def data-file "D:/data/all_160_in_51.P35.csv")

(def va-data (io/read-dataset data-file :header true))

;matrix

(def va-matrix (i/to-matrix ($ [:POP100 :HU100 :P035001] va-data)))

(first va-matrix)

(take 5 va-matrix)
(count va-matrix)

(reduce i/plus va-matrix)
;infix formulas


($= 7 * 4)

($= 7 * 4 + 3)

($= 4 * va-matrix)

($= 4 * (first va-matrix))

($= (i/sum (first va-matrix)) / (count (first va-matrix)))

($= (reduce i/plus va-matrix) / (count va-matrix))

(macroexpand-1 '($= (reduce i/plus va-matrix) / (count va-matrix)))

;selecting columns

(def data-file2 "D:/data/all_160.P3.csv")

(def race-data (io/read-dataset data-file2 :header true))

($ :POP100 race-data)

($ [:STATE :POP100 :POP100.2000] race-data)
;$ is a wrapper arount incanter's sel

(i/sel race-data :cols [:STATE :POP100 :POP100.2000])

;select rows


;one row as seq
($ 100 :all race-data)
;multiple rows as datasets

;(i/view ($ [0 1 2 3 4 5] :all race-data))

;(i/view ($ [0 1 2 3 4 5] [:NAME] race-data))

;$where clause

(def richmond ($where {:NAME "Richmond city"} race-data))

;(i/view richmond)

(def small ($where {:POP100 {:lte 1000}} race-data))

;(i/view small)

(i/nrow small)

(def medium ($where {:POP100 {:gt 1000 :lte 40000}} race-data))

;(i/view medium)

(def random-half
  ($where {:GEOID {:$fn (fn [_] (< 0.5 (rand)))}} race-data))

;(i/view random-half)


;;;;;;group by

;notice that $group-by does not return a dataset as other operators

(def by-state ($group-by :STATE race-data))

(keys by-state)


#_(i/view ($ :all (get by-state {:STATE 51})))

(def census2010 ($ [:STATE :NAME :POP100 :P003002 :P003003 :P003004 :P003005 :P003006 :P003007 :P003008] race-data))

;write to csv
#_(with-open [f-out (jio/writer "D:/data/census_2010.csv")]
  (csv/write-csv f-out [(map name (i/col-names census2010))])
  (csv/write-csv f-out (i/to-list census2010)))

;write to json


#_(with-open [f-out (jio/writer "D:/data/census_2010.json")]
  (json/write (:rows census2010) f-out))

;;;;;;;;;;;;;;join

(def data-file3 "D:/data/all_160_in_51.P3.csv")


(def racial-data (io/read-dataset data-file3 :header true))

(set/intersection (set (i/col-names va-data))
                  (set (i/col-names racial-data)))

(defn dedup-second
  [a b id-col]
  (let [a-cols (set (i/col-names a))]
    (conj (filter #(not (contains? a-cols %)) (i/col-names b)) id-col)))

(def racial-short ($ (vec (dedup-second va-data racial-data :GEOID)) racial-data))

(def all-data ($join [:GEOID :GEOID] va-data racial-short))

(i/col-names all-data)

(= (i/nrow va-data) (i/nrow racial-data) (i/nrow all-data))


;;;;;;;;;;;;;;;;;;;roll up;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

($rollup  :mean :POP100 :STATE race-data)

($rollup s/sd :POP100 :STATE race-data)


;;;;;;;;;;;;;;;;;;;differencing variables;;;;;;;;;;;;;;;;

(defn replace-empty
  [x]
  (if (nil? x)
    0
    x))

(def growth-rates
  (->> racial-data
       identity
       ($map replace-empty :POP100.2000)
       (i/minus (i/sel racial-data :cols :POP100))
       (i/dataset [:POP.DELTA])
       (i/conj-cols racial-data)))

(i/sel growth-rates
       :cols [:NAME :POP100 :POP100.2000 :POP.DELTA]
       :rows (range 5))

;;;;;;;;;;;;;;;;;;;;;;;;scaling variables;;;;;;;;;;;;;;;;;;;;;

(def ordered-data
  ($order :POP100 :asc racial-data))


(def scaled-data
  (->> (i/sel ordered-data :cols :POP100)
       (#(i/div % 1000.0))
       (i/dataset [:POP100.1000])
       (i/conj-cols ordered-data))
  )

(def log-scaled-data
  (->> (i/sel ordered-data :cols :POP100)
       i/log10
       (i/dataset [:POP100.LOG10])
       (i/conj-cols ordered-data)))

;(i/view log-scaled-data)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;incanter zoo;;;;;;;;;;;;;;;

(def data-file4 "D:/data/ibm.csv")

(def ^:dynamic *formatter* (f/formatter "yyyy-MM-dd"))

(defn parse-date
  [date]
  (f/parse *formatter* date))

(def stock-price-data
  (i/with-data
   (i/col-names (io/read-dataset data-file4 :header true) [:date-str :open :high :low :close :volume])
   (->> ($map parse-date :date-str)
        (i/dataset [:date])
        (i/conj-cols i/$data))))


(def stock-price-data-zoo (zoo/zoo stock-price-data :date))

(defn data-roll
  [data roll-period]
  (->> (i/sel data :cols :close)
       (zoo/roll-mean roll-period)
       (i/dataset [(keyword (str roll-period "-day"))])
       (i/conj-cols data)))

(data-roll stock-price-data-zoo 30)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;tips;;;;;;;;;;;;;;;;;;;;;;;

(i/to-list census2010)
(:rows census2010)
(i/minus [1 2 3] [4 5 6])

;compare

(i/sel ordered-data :cols :POP100)
(i/sel ordered-data :cols [:POP100])



(i/col-names (i/to-dataset [[1 2 3] [4 5 6]]) [:a :b :c])
;is the same as

(i/dataset [:a :b :c] [[1 2 3] [4 5 6]])













































