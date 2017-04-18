(ns gyfu.formatters.console
  (:require [clansi :as c]))

(defn write
  [color & message]
  (print (c/style (apply str message) color)))

(defn writeln
  [color & message]
  (println (c/style (apply str message) color)))

(defn write-tag
  [tag]
  (write :grey "<" tag ">"))

(defn write-results
  [results]
  (writeln :cyan (-> results :schema :title))
  (write :white "on ")
  (writeln :underline (:document-uri results))
  (println)

  (let [failed-tests (filter (complement :success) (:tests results))]
    (doseq [test failed-tests]
      (write :red "✗ ")
      (write-tag (:node-name test))
      (write :default (str " at line " (:line-number test) ", column " (:column-number test) ":\n"))
      (writeln :white (:message test)))))
