(ns vegacharttemplates
  (:require [reacttabledefinition :as reacttabledefinition])
  )

(defn cases-and-deaths-bar []
  (let [source (take-last 10 (drop-last (sort-by :cases @reacttabledefinition/res)))
        data (apply concat (into [] (for [line source] [{:location (:location line) :cases-or-deaths "cases" :number (:cases line)}
                                                        {:location (:location line) :cases-or-deaths "deaths" :number (:deaths line)}])))]
    {:data   {:values data}
     :width  {:step 36},
     :mark   "bar",
     :encoding
             {:column {:field "location", :type "nominal", :spacing 10},
              :y
                      {:aggregate "sum",
                       :field     "number",
                       :title     "Amount",
                       :axis      {:grid false}},
              :x      {:field "cases-or-deaths", :axis {:title ""}},
              :color  {:field "cases-or-deaths", :scale {:range ["#675193" "#ca8861"]}}},
     :config {:view {:stroke "transparent"}, :axis {:domainWidth 1}}}
    )
  )

(defn cases-and-deaths-scatter-with-tooltip []
  (let [data (take-last 30 (drop-last (sort-by :cases @reacttabledefinition/res)))]
    {:data     {:values data}
     :width    800
     :mark     "point",
     :encoding {
                :x       {:field "cases", :axis {:title "Cases" :titleFontSize 14 :labelFontSize 12} :type "quantitative"},
                :y       {:field "deaths", :axis {:title "Deaths" :titleFontSize 14 :labelFontSize 12} :type "quantitative"}
                :tooltip [{:field "location" :type "nominal"}
                          {:field "cases" :type "quantitative"}
                          {:field "deaths" :type "quantitative"}]},
     :config   {:view {:stroke "transparent"}, :axis {:domainWidth 1}}}
    )
  )