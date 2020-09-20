# react-table-vega-template

A minimal template for displaying tabular data and charts with Clojurescript, through react-table and vega-lite.


## The problem

Analysing data is great, but at some point you need to display it. Tables and charts are the natural way. Unfortunately I found the tooling in Clojurescript quite painful. This is an attempt at providing a clean template.

## The tooling

For table data we are using react-table v6 https://github.com/tannerlinsley/react-table/tree/v6. The current version is v7 but it is basically a completely different package and paradigm. Version 6 is battle tested, with plenty of documentation and examples online. The code has a bunch of examples / gotchas.

For charts we are using vega through https://github.com/metasoarous/oz/. The code has some examples but it is all better covered in the Vega website https://vega.github.io/vega-lite/.

For building we are using http://shadow-cljs.org/. Their documentation is great, check it out.

## The installation

Things can get tricky here. First, don't use Leiningen, go for `deps.edn` - somehow I found Leiningen doesn't play well with shadow-cljs. The easiest way to make sure everything works:
* install Clojure (https://clojure.org/guides/getting_started)
* install NPM (https://nodejs.org/en/)
* install shadow-cljs globally: `npm install -g shadow-cljs`
* install the right npm dependencies in your project. `npm install` in the project folder should suffice, but otherwise do `npm install react react-dom react-table-v6 vega vega-embed vega-lite vega-tooltip`

## Usage

In the project folder run `npx shadow-cljs watch app`. Take your browser to `localhost:8080`. You should see a bunch of tables and charts.

