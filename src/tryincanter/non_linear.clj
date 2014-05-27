(ns tryincanter.non-linear
  (:require [incanter.core :as i]
            [incanter.io :as io]
            [incanter.optimize :as o]
            [incanter.stats :as s]
            [incanter.charts :as c])
  (:import [java.lang Math]))


(def data-file "D:/data/accident-fatalities.tsv")
