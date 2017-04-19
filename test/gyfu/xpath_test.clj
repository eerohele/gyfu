(ns gyfu.xpath-test
  (:require [clojure.test :refer :all]
            [gyfu.saxon :as saxon]
            [gyfu.xpath :as xpath])
  (:import (org.xmlunit.builder Input DiffBuilder)
           (net.sf.saxon.s9api QName XdmAtomicValue)))

(defn- diff-builder
  [actual]
  (.ignoreWhitespace (.checkForIdentical (DiffBuilder/compare (Input/fromString (str actual))))))

(defn- xml-equal?
  [actual expected]
  (every? (fn [[a e]] (not (.hasDifferences (.build (.withTest (diff-builder a) (Input/fromString e))))))
          (map vector actual expected)))

(deftest set-default-xpath-namespace
  (let [compiler
        (xpath/set-default-namespace (xpath/compiler) "foo")]
    (is (= "foo" (xpath/default-namespace compiler)))))

(deftest match-xpath-pattern
  (let [compiler (xpath/compiler)
        context (saxon/build "<foo a=\"b\"><bar><foo c=\"d\"/></bar></foo>")]
    (is (xml-equal? (xpath/match compiler context "foo")
                    ["<foo a=\"b\"><bar><foo c=\"d\"/></bar></foo>" "<foo c=\"d\"/>"]))))

(deftest select-xpath-expression
  (let [compiler (xpath/compiler saxon/processor nil [(xpath/ns "q" "quux")])
        context (saxon/build "<foo xmlns=\"quux\"><bar a=\"b\"/><bar c=\"d\"/></foo>")]
    (is (xml-equal? (seq (xpath/select compiler context "q:foo/q:bar"))
                    ["<bar xmlns=\"quux\" a=\"b\"/>" "<bar xmlns=\"quux\" c=\"d\"/>"]))))

(deftest value-of-xpath-expression
  (let [compiler (xpath/compiler)
        node (saxon/build "<num>1</num>")]
    (is (= (xpath/value-of compiler node "xs:int(num)") 1))))

(deftest matches-xpath-pattern
  (let [compiler (xpath/compiler)
        node (xpath/select compiler (saxon/build "<num>1</num>") "num[1]")]
    (is (= (xpath/matches? compiler node (xpath/pattern compiler "num[xs:int(.) eq 1]")) true))))

(deftest set-xpath-variable-for-expression
  (let [compiler (xpath/compiler)
        context (saxon/build "<num>1</num>")]
    (is (xpath/is? compiler context "xs:integer(num) * $two eq 2" [:two 2]))))

(deftest set-xpath-variable-for-pattern
  (let [compiler (xpath/compiler)
        context (saxon/build "<num>1</num>")]
    (is (xml-equal? (xpath/match compiler context "num[xs:integer(.) eq $one]" [:one 1])
                    ["<num>1</num>"]))))