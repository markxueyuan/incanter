(ns tryincanter.bootstrap
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.io :as io]
            [incanter.charts :as c]))


(def data-file "D:/data/testlinear.csv")

(def data (io/read-dataset data-file :header true))

(def pop100 (i/sel data :cols :POP100))

(def samples (s/bootstrap pop100 s/median :size 2000))

(i/view (c/histogram samples))
