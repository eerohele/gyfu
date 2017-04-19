(ns gyfu.elements
  "Constructor functions for ISO Schematron elements."
  (:refer-clojure :exclude [assert report]))

(defrecord Test [message test])
(defrecord Rule [context attributes tests])
(defrecord Pattern [title attributes rules])
(defrecord Phase [id active-patterns])
(defrecord Schema [title attributes patterns])

(defn schema
  "Define an ISO Schematron schema."
  [title attributes & patterns]
  (->Schema title attributes patterns))

(defn pattern
  "Define an ISO Schematron pattern."
  [title attributes & rules]
  (->Pattern title attributes rules))

(defn rule
  "Define an ISO Schematron rule."
  [context attributes & tests]
  (->Rule context attributes tests))

(defn assert
  "Define an ISO Schematron assertion."
  [message test]
  (->Test message test))

(def report
  "An alias for [[assert]]."
  assert)

(def of
  "An alias for an empty map.

  If you want, you can use this to de-clutter pattern and rule declarations.

  For example:

  ```
  (rule \"match/a/node\" of
        (assert \"The int value of <node> is > 0\" \"xs:int(.) gt 0\"))
  ```" {})
