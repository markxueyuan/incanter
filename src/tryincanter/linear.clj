(ns tryincanter.linear
  (:require [incanter.core :as i]
            [incanter.io :as io]
            [incanter.stats :as s]
            [incanter.charts :as c]))

(def data-file "D:/data/all_160_in_51.P35.csv")

(def family-data (io/read-dataset data-file :header true))

(def housing (i/sel family-data :cols :HU100))

(def families (i/sel family-data :cols :P035001))

(def families-lm (s/linear-model housing families :intercept false))

(keys families-lm)

(:r-square families-lm)

(:f-prob families-lm)

(:f-stat families-lm)

(def housing-chart
  (doto
    (c/scatter-plot families housing
                    :title "Relationship of Housing to Families"
                    :x-label "Families"
                    :y-label "Housing"
                    :legend true)
    (c/add-lines families (:fitted families-lm)
                 :series-label "Linear Model")
    (i/view)))
