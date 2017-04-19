(ns gyfu.core-test
  (:require [clojure.test :refer :all :exclude [report]]
            [gyfu.core :as g]
            [gyfu.elements :refer :all]
            [gyfu.xpath :as xpath]
            [gyfu.saxon :as saxon])
  (:import (java.net URI)))

(def ^:private compiler (xpath/compiler))
(def ^:private match (partial xpath/match compiler))
(def ^:private select (partial xpath/select compiler))
(def ^:private xpath-pattern (partial xpath/pattern compiler))

(deftest compile-schema
  (let [schema (schema "foo" {:let {:bar "xs:int(foo/@bar)"}}
                       (pattern "bar" nil
                                (rule "baz" nil
                                      (assert "bar plus baz is 3" "$bar + xs:int(.) eq 3"))))]
    (is (= (g/compile-schema schema nil)
           {:tests   [{:message       "bar plus baz is 3"
                       :pattern       {:attributes nil
                                       :title      "bar"}
                       :rule          {:attributes nil
                                       :context    "baz"}
                       :test          "$bar + xs:int(.) eq 3"
                       :xpath-pattern (xpath-pattern "baz")}]
            :options nil
            :schema  {:attributes {:let {:bar "xs:int(foo/@bar)"}}
                      :title      "foo"}}))))

(deftest successful-assertion
  (let [node (saxon/build "<foo><bar>a</bar><bar>b</bar></foo>")
        assertion (assert "bar is a or b" ". eq 'a' or . eq 'b'")]
    (is (= (map #(g/apply-test compiler % nil assertion) (match node "bar"))
           [{:node          (select node "foo/bar[. eq 'a']")
             :node-name     (saxon/->qname "bar")
             :line-number   1
             :column-number 11
             :message       "bar is a or b"
             :success       true}
            {:node          (select node "foo/bar[. eq 'b']")
             :node-name     (saxon/->qname "bar")
             :line-number   1
             :column-number 23
             :message       "bar is a or b"
             :success       true}]))))

(deftest unsuccessful-assertion
  (let [node (saxon/build "<foo><bar>a</bar><bar>c</bar></foo>")
        assertion (assert "bar is a or b" ". eq 'a' or . eq 'b'")]
    (is (= (map #(g/apply-test compiler % nil assertion) (match node "bar"))
           [{:node          (select node "foo/bar[. eq 'a']")
             :node-name     (saxon/->qname "bar")
             :line-number   1
             :column-number 11
             :message       "bar is a or b"
             :success       true}
            {:node          (select node "foo/bar[. eq 'c']")
             :node-name     (saxon/->qname "bar")
             :line-number   1
             :column-number 23
             :message       "bar is a or b"
             :success       false}]))))

(deftest bind-schema-variable
  (let [node (saxon/build "<foo bar=\"1\"><baz>2</baz></foo>")
        schema (schema "foo" {:let {:bar "xs:int(foo/@bar)"}}
                       (pattern "bar" nil
                                (rule "baz" nil
                                      (assert "bar plus baz is 3" "$bar + xs:int(.) eq 3"))))]
    (is (= (-> schema (g/compile-schema {}) (g/apply-schema node))
           {:document-uri (URI. "")
            :schema       {:title      "foo",
                           :attributes {:let {:bar "xs:int(foo/@bar)"}}},
            :tests        [{:pattern       {:title "bar", :attributes nil},
                            :rule          {:context "baz", :attributes nil},
                            :message       "bar plus baz is 3",
                            :node          (select node "foo/baz")
                            :node-name     (saxon/->qname "baz")
                            :line-number   1,
                            :column-number 19,
                            :success       true}]}))))

(deftest bind-rule-variable
  (let [node (saxon/build "<foo bar=\"1\"><baz>2</baz></foo>")
        schema (schema nil nil
                       (pattern "bar" nil
                                (rule "foo"
                                      {:let {:bar "xs:int(@bar)"}}
                                      (assert "bar plus baz is 3" "$bar + xs:int(baz) eq 3"))))]
    (is (= (-> schema (g/compile-schema {}) (g/apply-schema node))
           {:document-uri (URI. "")
            :schema       {:attributes nil
                           :title      nil}
            :tests        [{:column-number 14
                            :line-number   1
                            :message       "bar plus baz is 3"
                            :node          (select node "foo")
                            :node-name     (saxon/->qname "foo")
                            :pattern       {:attributes nil
                                            :title      "bar"}
                            :rule          {:attributes {:let {:bar "xs:int(@bar)"}}
                                            :context    "foo"}
                            :success       true}]}))))

(deftest active-patterns-with-match-returns-only-matched-patterns
  (let [a (pattern "a" {:id :a} (rule "a" nil (assert "a" "a")))
        b (pattern "b" {:id :b} (rule "b" nil (assert "b" "b")))
        c (pattern "c" {:id :c} (rule "c" nil (assert "c" "c")))]
    (is (= (g/get-active-patterns
            (schema "schema" {:phases {:my-phase #{:a :b}}} a b c) :my-phase)
           #{a b}))))

(deftest active-patterns-nil-returns-all-patterns
  (let [a (pattern "a" {:id :a} (rule "a" nil (assert "a" "a")))
        b (pattern "b" {:id :b} (rule "b" nil (assert "b" "b")))]
    (is (= (g/get-active-patterns (schema nil nil a b) nil) #{a b}))))

(deftest active-patterns-without-match-returns-empty-set
  (let [a (pattern "a" {:id :a} (rule "a" nil (assert "a" "a")))
        b (pattern "b" {:id :b} (rule "b" nil (assert "b" "b")))]
    (is (= (g/get-active-patterns (schema nil nil a b) :nope) #{}))))
