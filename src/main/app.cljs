(ns app
      (:require [reagent.core :as r]
                [reagent.dom :as rd]
                ["react-table-v6" :as rt :default ReactTable]
                [oz.core :as oz]
                [reacttabledefinition :as table]
                [vegacharttemplates :as charts]))



(defn some-component []
      [:div
       [:h1 "react-table v6 examples"]
       [:a {:href "https://www.npmjs.com/package/react-table-v6"} "npmjs link with docs"]
       ;[:p.someclass
       ; "I have " [:strong "bold"]
       ; [:span {:style {:color "red"}} " and red"]
       ; " text."]
       [:h3 "Standard covid table"]
       [:p "This table has custom formatting. It also has custom sorts for continent and country such that World is always at the top."]
       [:p "Text filtering is case insensitive and you can use a comma (,) as boolean OR. Number filtering understands = (default) > and <."]
       [:p "You can extract the filter to reuse it. It's currently: " (str (js->clj @table/table-filter))]
       [table/covid-react-table]
       [:br]
       [:h3 "Same table, pivoted."]
       [:p "The aggregate number will be median except for population, cases and deaths where it will be sum. You could easily do population weighted average instead of median."]
       [:p "Filtering is an issue in that case."]
       [table/covid-react-table-pivoted]
       [:br]
       [:p "Finally, a dummy table with custom formatting for negative values, where rows are clickable."]
       [table/dummy-table]
       [:h1 "Vega-lite chart examples"]
       [:p "There is great documentation on the " [:a {:href "https://vega.github.io/vega-lite/"} "Vega website"] " and the editor is extremely handy for trial and error. Once you have something you like in the editor you can convert it with " [:a {:href "http://cljson.com/"} "http://cljson.com/"]]
       [:p "A simple scatter cart - the name of the country will appear if you hover around a point"]
       [oz/vega-lite (charts/cases-and-deaths-scatter-with-tooltip)]
       [:br]
       [:p "A grouped bar chart - these can get tricky"]
       [oz/vega-lite (charts/cases-and-deaths-bar)]
       ])

(defn ^:dev/after-load mountit []
  (rd/render [some-component]
             (.getElementById js/document "root")))

(defn ^:export init []
  (mountit))