(ns tryincanter.SOM
  (:require [incanter.core :as core]
            [incanter.som :as som]
            [incanter.datasets :as ds]
            [incanter.stats :as stats]
            [incanter.charts :as charts]))

(def iris (ds/get-dataset :iris))

(core/view iris)

(def iris-clusters
  (som/som-batch-train
   (core/to-matrix
    (core/sel iris :cols [:Sepal.Length :Sepal.Width
                          :Petal.Length :Petal.Width]))))

iris-clusters


(for [[pos rws] (:sets iris-clusters)]
  (str pos \: (frequencies (core/sel iris :cols :Species :rows rws))))

(:dims iris-clusters)

(core/view (charts/xy-plot (range (count (:fit iris-clusters))) (:fit iris-clusters)))

(keys (:sets iris-clusters))

(def cluster-means (map #(map stats/mean (core/trans (core/sel iris :rows ((:sets iris-clusters) %)))) (keys (:sets iris-clusters))))

cluster-means

(map #(core/trans (core/sel iris :rows ((:sets iris-clusters) %))) (keys (:sets iris-clusters)))

