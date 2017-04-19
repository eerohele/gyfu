(ns gyfu.core-bench
  (:require [gyfu.core :as g]
            [gyfu.examples.pain-mdr :as examples]
            [gyfu.saxon :as saxon]
            [criterium.core :refer [bench]]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(defn load-fixture [path]
  (-> (str "examples/" path) io/resource saxon/build))

(deftest ^:bench benchmark-parse-and-validate-same-file-n-times
  (let [options {:default-xpath-namespace
                 "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"}
        schema (g/compile-schema examples/pain-mdr-schema options)
        files ["ISO20022.xml" "DABA_ADV.xml" "DABA_REF.xml" "DABA_SDVA.xml"]]
    (bench
     (dotimes [_ 100]
       (map (partial g/apply-schema schema) (mapv load-fixture files))))))
