(ns reacttabletools
  (:require
    [goog.string :as gstring]
    [goog.string.format]
    [reagent.core :as r])
  (:import (goog.i18n NumberFormat)
           (goog.i18n.NumberFormat Format)))


;AGGREGATION: react-table will just feed the aggregate function with a sequence

(defn sum-rows [vals] (reduce + vals))

(defn median [coll]
  (let [sorted (sort (remove nil? coll))
        cnt (count sorted)
        halfway (quot cnt 2)]
    (cond
      (zero? cnt) nil
      (odd? cnt) (nth sorted halfway)
      :else (let [bottom (dec halfway) bottom-val (nth sorted bottom) top-val (nth sorted halfway)] (* 0.5 (+ bottom-val top-val))))))


;FILTERING
;react-table calls the filter function with filterfn and row, where filterfn is {id: column_name value: text_in_filter_box

(defn case-insensitive-filter [filterfn row]
  "filterfn is {id: column_name value: text_in_filter_box
  OR through comma separation"
  (let [filter-values (clojure.string/split (.toLowerCase ^string (aget filterfn "value")) ",")]
    (some true? (map #(.includes ^string (.toLowerCase ^string (str (aget row (aget filterfn "id")))) %) filter-values))))

(defn compare-nb
  "Inequality filter
  Filter cell can take number N, =N, >N or <N. If just N will default to ="
  [filterfn row]
  (let [input (aget filterfn "value")
        rowval (aget row (aget filterfn "id"))]
    (case (subs input 0 1)
      "=" (= rowval (cljs.reader/read-string (subs input 1)))
      ">" (> rowval (cljs.reader/read-string (subs input 1)))
      "<" (< rowval (cljs.reader/read-string (subs input 1)))
      (= rowval (cljs.reader/read-string input)))))


;COLUMN FORMATTING

(defn red-negatives [state rowInfo column]
  (if (and (some? rowInfo) (neg? (aget rowInfo "row" (aget column "id")))) (clj->js {:style {:color "red" :textAlign "right"}}) (clj->js {:style { :textAlign "right"}})))


;CELL RENDERING

(defn nb-cell-format
  "This will write a single cell.
  Anything can be there (even another table!), see example below
  Note that [this] has access to the full row so conditional evaluation is possible (e.g. change column B based on values in column A)
  Here we take the input value if it's there, scale it (useful for percentages) and format it."
  [fmt m this]
  (if-let [x (aget this "value")] (gstring/format fmt (* m x)) "-"))

(defn nb-thousand-cell-format
  "This will write a single cell."
  [this]
  (if-let [x (aget this "value")] (.format (NumberFormat. Format/DECIMAL) (str (js/Math.round x))) "-"))

(defn full-on-cell-format-example
  "This will write a single cell with another element."
  [this]
  (let [stylefn (fn [y] {:style nil})
        elementfn (fn [y] (aget y "value"))]
    (if (aget this "value")
      (r/as-element [:div (stylefn this) (elementfn this)])
      "")))


;SORTING - custom sorts are possible. Note that on first rendering things are not sorted.

(defn worldsort [a b desc]
  (let [res (cond
              (and (= a "World") (not desc)) (- js/Infinity)
              (and (= b "World") (not desc)) js/Infinity
              (and (= a "World") desc) js/Infinity
              (and (= b "World") desc) (- js/Infinity)
              :else (compare a b))]
    (if (pos? res) 1 -1)))
