(ns gyfu.core-spec
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [gyfu.core :as sch]
            [gyfu.core-spec :as cs]
            [gyfu.xpath-spec :as xs])
  (:import (clojure.lang IFn)
           (net.sf.saxon.s9api XdmNode)))

(s/def ::default-xpath-namespace string?)
(s/def ::xpath-namespaces (s/* ::xs/namespace))
(s/def ::phase (s/nilable keyword?))

(s/def ::options
  (s/nilable (s/keys :req-un [] :opt-un [::default-xpath-namespace
                                         ::xpath-namespaces ::phase])))

(s/def ::title string?)
(s/def ::function (partial instance? IFn))

;; TODO
;;
;; :format should be something like:
;;
;; (s/fspec :args (s/cat :name string? :value-of (s/fspec :args ::xs/xpath-expression :ret ::xs/xdmvalue)) :ret string?)
;;
;; But that doesn't work.
(s/def ::message (s/or :message string?
                       :format ::function))

;; TODO
;;
;; IFn should be more specific.
(s/def ::test (s/or ::function ::xs/xpath-expression))

(s/def ::context ::xs/xpath-expression)

;; TODO
;;
;; There should be as spec for common attributes, as well as for the attributes of each element.
;;
;; instead of (partial instance Object), the value predicate should check that the type is either XdmNode or one of the
;; types that has an XdmAtomicValue constructor.
(s/def ::attributes (s/nilable (s/map-of keyword? (partial instance? Object))))

(s/def ::assertion (s/keys :req-un [::message ::test]))
(s/def ::rule (s/keys :req-un [::context ::tests] :opt-un [::attributes]))
(s/def ::rules (s/+ ::rule))
(s/def ::pattern (s/keys :req-un [::title ::rules] :opt-un [::attributes]))
(s/def ::patterns (s/+ ::pattern))
(s/def ::schema (s/keys :req-un [::title ::patterns] :opt-un [::attributes]))
(s/def ::node (partial instance? XdmNode))
(s/def ::column-number int?)
(s/def ::line-number int?)
(s/def ::success boolean?)
