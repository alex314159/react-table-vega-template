(ns reacttabledefinition
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            ["react-table-v6" :as rt :default ReactTable]
            [reacttabletools :as tools]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            ))


;DATA PREPARATION

(def res (r/atom []))

(defn clean-covid-data
  "Here it's important to return an empty table if the asset isn't ready"
  [data]
  (if (pos? (count data))
    (sort-by (juxt #(= (:continent %) "World") :continent :location)
             (into [] (for [[k m] data] (assoc
                                          (dissoc m :data)
                                          :continent (if (= (:location m) "World") "World" (:continent m))
                                          :code k
                                          :days (count (m :data))
                                          :deaths (last (map :total_deaths (m :data)))
                                          :cases (last (map :total_cases (m :data)))))))
    []))

(def owid-covid-data (go (let [response (<! (http/get "assets/owid-covid-data.json"))] ;source https://covid.ourworldindata.org/data/owid-covid-data.json
                           (reset! res (mapv #(assoc % :dummy 0) (clean-covid-data (:body response))))))) ;the dummy will be helfpul for pivots


;REACT-TABLE

(def table-filter (r/atom []))

(def covid-table-columns
  "Notes:
    This table has a 2 header structure with grouped headings. You can't have more than 2.
    We are creating a custom style so headers can be multiline.
    If a props needs a field to e.g. conditional format a column, the field needs to be in the columns. You can use :show false in the definition to hide it
  "
  (let [header-style {:overflow nil :whiteSpace "pre-line" :wordWrap "break-word"}]
    [
     {:Header "Identification" :columns [{:Header "Total" :accessor "dummy" :show false :width 20}
                                         {:Header "Continent" :accessor "continent" :style {:fontWeight "bold"} :width 150 :sortMethod (partial tools/custom-text-sort "World")}
                                         {:Header "Country" :accessor "location" :width 150 :sortMethod (partial tools/custom-text-sort "World")}
                                         {:Header "Country code" :accessor "code" :show false}]}
     {:Header "Population" :columns [
                                     {:Header "Amount" :accessor "population" :width 110 :style {:textAlign "right"} :Cell tools/nb-thousand-cell-format :aggregate tools/sum-rows :filterMethod tools/nb-filter-OR-AND}
                                     {:Header "Density" :accessor "population_density" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :filterMethod tools/nb-filter-OR-AND}
                                     {:Header "Median age" :accessor "median_age" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :filterMethod tools/nb-filter-OR-AND}
                                     {:Header "65+" :accessor "aged_65_older" :width 75 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f%" 1) :aggregate tools/median :filterMethod tools/nb-filter-OR-AND}
                                     {:Header "70+" :accessor "aged_70_older" :width 75 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f%" 1) :aggregate tools/median :filterMethod tools/nb-filter-OR-AND}]}
     {:Header "Economy" :columns [
                                  {:Header "GDP/capita" :accessor "gdp_per_capita" :width 100 :style {:textAlign "right"} :Cell tools/nb-thousand-cell-format :aggregate tools/median :filterMethod tools/nb-filter-OR-AND}
                                  {:Header "Human dev. index" :accessor "human_development_index" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.2f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}
                                  {:Header "Life expectancy" :accessor "life_expectancy" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}]}
     {:Header "Risk factors" :columns [
                                       {:Header "Cardiovasc. death rate" :accessor "cardiovasc_death_rate" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.0f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}
                                       {:Header "Diabetes prevalence" :accessor "diabetes_prevalence" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}
                                       {:Header "Handwashing facilities" :accessor "handwashing_facilities" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}
                                       {:Header "Hospital beds per 1000" :accessor "hospital_beds_per_thousand" :width 100 :style {:textAlign "right"} :Cell (partial tools/nb-cell-format "%.1f" 1) :aggregate tools/median :headerStyle header-style :filterMethod tools/nb-filter-OR-AND}]}

     {:Header "COVID data" :columns [
                                     {:Header "Days" :accessor "days" :width 100 :style {:textAlign "right"} :aggregate tools/median}
                                     {:Header "Cases" :accessor "cases" :width 110 :style {:textAlign "right"} :Cell tools/nb-thousand-cell-format :aggregate tools/sum-rows}
                                     {:Header "Deaths" :accessor "deaths" :width 100 :style {:textAlign "right"} :Cell tools/nb-thousand-cell-format :aggregate tools/sum-rows}]}
     ]))


(defn covid-react-table
  "Note data can't be nil - always provide an empty vector fallback
  The filter persistence slows the table because at every keystroke the table gets re-rendered and
  @res is deeply converted to a js object. If no need to manipulate it, having it a js object on mount is faster.
  "
  []
  [:> ReactTable
   {:data                @res
    :columns             covid-table-columns
    :showPagination      true
    :sortable            true
    :filterable          true
    :defaultPageSize     10
    :showPageSizeOptions true
    :pageSizeOptions     [10 20 50]
    :className           "-striped -highlight"
    :defaultFilterMethod tools/text-filter-OR
    :defaultFiltered     @table-filter                    ;this will allow filter persistence if table gets re-rendered - could also be: [{:id "continent" :value "Asia"} {:id "location" :value "Vietnam"}]
    :onFilteredChange    #(reset! table-filter %)
    }])

(def expander (r/atom {0 {}}))

(defn covid-react-table-pivoted
  "Filtering is a lot harder - we filter the source table directly in CLJS which is slow"
  []
  [:> ReactTable
   {:data                (tools/cljs-text-filter-OR @table-filter @res)
    :columns             covid-table-columns
    :showPagination      false
    :sortable            true
    :filterable          true
    :pageSize            (count (distinct (map :continent @res)))
    :showPageSizeOptions false
    :pivotBy             ["dummy" "continent"]                    ;you could pivot by many columns if needed. dummy is used to see sum of selections
    :expanded            @expander
    :onExpandedChange    #(reset! expander %)
    ;:defaultExpanded {0 {}};this is buggy see https://github.com/tannerlinsley/react-table/issues/306 hence slow workaround
    :defaultFilterMethod (fn [filterfn row] true)
    :onFilteredChange    #(reset! table-filter %)
    }])

(defn alert-fn [state rowInfo instance] (clj->js {:onClick #(js/alert (str "You clicked on line where a=" (aget rowInfo "row" "a"))) :style {:cursor "pointer"}}))

(defn dummy-table []
  (let [data (into [] (for [i (range 10)] {:label i :a (- (rand-int 20) 10) :b (- (rand-int 50) 25)}))]
    [:> ReactTable
     {:data       data
      :columns    [{:Header "Label" :accessor "label"}
                   {:Header "A" :accessor "a" :getProps tools/red-negatives}
                   {:Header "B" :accessor "b" :getProps tools/red-negatives}]
      :className           "-striped -highlight"
      :pageSize 10
      :showPageSizeOptions false
      :showPagination false
      :getTrProps alert-fn}]))