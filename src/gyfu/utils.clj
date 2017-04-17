(ns gyfu.utils
  (:import (clojure.lang IFn)))

(defn function?
  "Return `true` if `x` is a function."
  [x]
  (instance? IFn x))

;; Shamelessly lifted from http://dev.clojure.org/jira/browse/CLJ-2056 until
;; added into clojure.core.
(defn seek
  "Return the first item in `coll` that matches `pred`."
  ([pred coll]
   (transduce (comp (filter pred) (halt-when any?)) identity nil coll)))
