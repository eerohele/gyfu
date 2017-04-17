(ns gyfu.core-bench
  (:require [gyfu.examples.pain-mdr :as examples]
            [criterium.core :refer [bench]]
            [clojure.test :refer :all]
            [gyfu.core :as g]
            [clojure.java.io :as io]
            [gyfu.saxon :as saxon]))

(defn load-fixture [path]
  (-> (str "examples/" path) io/resource io/as-file saxon/build))

(deftest ^:bench benchmark-parse-and-validate-same-file-n-times
  (let [options {:default-xpath-namespace
                 "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"}
        schema (g/compile examples/pain-mdr-schema options)
        files ["ISO20022.xml" "DABA_ADV.xml" "DABA_REF.xml" "DABA_SDVA.xml"]]
    (bench
      (dotimes [i 100]
        (map (partial g/apply schema) (mapv load-fixture files))))))


