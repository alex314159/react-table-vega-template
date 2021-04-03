(ns reacttabletools
  (:require
    [goog.string :as gstring]
    [goog.string.format]
    [reagent.core :as r])
  (:import (goog.i18n NumberFormat)
           (goog.i18n.NumberFormat Format)))

;(set! *warn-on-infer* true)

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

(defn lower-case-s-in-value?
  "Checks if s (already assumed lower case) is in value. If s starts by -, excludes it"
  [^js/String s ^js/String value]
  (if (= (.charAt s 0) "-")
    (not (.includes ^js/String (.toLowerCase ^js/String value) (.substring s 1)))
    (.includes ^js/String (.toLowerCase ^js/String value) s)))

(defn text-filter-OR
  "filterfn is {id: column_name value: text_in_filter_box}
  OR through comma separation
  - first will exclude results"
  [filterfn row]
  (some true?
        (map #(lower-case-s-in-value? % (aget row (aget filterfn "id")))
             (clojure.string/split (.toLowerCase ^js/String (aget filterfn "value")) ","))))

(defn comparator-read
  [rowval mult input]
  (case (subs input 0 1)
    ">" (> rowval (* mult (cljs.reader/read-string (subs input 1))))
    "<" (< rowval (* mult (cljs.reader/read-string (subs input 1))))
    "=" (= rowval (* mult (cljs.reader/read-string (subs input 1))))
    (= rowval (* mult (cljs.reader/read-string input)))))

(defn nb-filter-OR-AND
  "filterfn is {id: column_name value: text_in_filter_box
  comma separation is OR. Within comma separation, & is AND."
  [filterfn row]
  (let [compread (partial comparator-read (aget row (aget filterfn "id")) 1.)]
    (some true?
          (map (fn [line] (every? true? (map compread (.split ^js/String line "&"))))
               (.split ^js/String (.toLowerCase ^js/String (aget filterfn "value")) ",")))))

(defn nb-filter-OR-AND-x100
  "filterfn is {id: column_name value: text_in_filter_box
  comma separation is OR. Within comma separation, & is AND."
  [filterfn row]
  (let [compread (partial comparator-read (aget row (aget filterfn "id")) 0.01)]
    (some true?
          (map (fn [line] (every? true? (map compread (.split ^js/String line "&"))))
               (.split ^js/String (.toLowerCase ^js/String (aget filterfn "value")) ",")))))

;filtering pivot tables takes a bit more work - we will filter the original source data

(defn chainfilter
  "Chain filter (boolean AND). Defaults to equality if predicate is not a function.
  warning: only one filter per key (no duplicates)
  example: (chainfilter {:portfolio #(= % \"OGEMCORD\") :weight pos?} @positions)
  equivalent to (chainfilter {:portfolio \"OGEMCORD\" :weight pos?} @positions)"
  [m coll]
  (reduce-kv
    (fn [erg k pred]
      (filter #(if (fn? pred) (pred (get % k)) (= pred (get % k))) erg)) coll m))

(defn cljs-text-filter-OR-fn
  "Used for pivot tables - creates the filter function which will filter the source data directly. Slow as re-renders everytime."
  [filterfn]
  (let [filter-chain (into {} (for [line filterfn] [(keyword (aget line "id")) (aget line "value")]))]
    (into {} (for [[k filter-values] filter-chain]
               [k
                (fn [value]
                  (some true?
                        (map #(lower-case-s-in-value? % value)
                             (.split (.toLowerCase ^js/String filter-values) ","))))]))))

(defn cljs-text-filter-OR
  "Combining the latter two functions to filter the source data"
  [filterfn table]
  (chainfilter (cljs-text-filter-OR-fn filterfn) table))


;COLUMN FORMATTING

(defn red-negatives [state rowInfo column]
  (if (and (some? rowInfo) (neg? (aget rowInfo "row" (aget column "id"))))
    (clj->js {:style {:color "red" :textAlign "right"}})
    (clj->js {:style { :textAlign "right"}})))


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

(defn custom-text-sort [firstword a b desc]
  (let [res (cond
              (and (= a firstword) (not desc)) (- js/Infinity)
              (and (= b firstword) (not desc)) js/Infinity
              (and (= a firstword) desc) js/Infinity
              (and (= b firstword) desc) (- js/Infinity)
              :else (compare a b))]
    (if (pos? res) 1 -1)))
