(ns ^:no-doc gyfu.examples.pain-mdr
  (:require [clojure.java.io :as io]
            [clojure.data.zip.xml :refer [xml1-> attr text]]
            [gyfu.elements :refer :all]
            [clojure.edn :as edn]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.local :as l])
  (:refer-clojure :exclude [assert])
  (:import (org.iban4j BicUtil IbanUtil)))

;; I'm an example Gyfu schema.
;;
;; You can use me to test that your pain.001.001.03 payment initiation XML
;; message is valid.

(def iso4217-currency-codes
  "Read the set of valid ISO 4217 currency codes from an EDN file."
  (-> "ccy.edn" io/resource slurp edn/read-string delay))

;; FUNCTIONS
;;
;; Functions that you can use to validate things in a pain.001.001.03 XML
;; document.

(defn has-valid-currency-code?
  "Check whether a payments initiation message amount has a valid ISO 4217
  currency code."
  [amount]
  (boolean (some #{(xml1-> amount (attr :Ccy))} @iso4217-currency-codes)))

(defn has-valid-iban-number?
  "Check whether the given IBAN is valid."
  [iban]
  (IbanUtil/validate (xml1-> iban text)))

(defn has-valid-bic-code?
  "Check whether the given BIC is valid."
  [bic]
  (BicUtil/validate (xml1-> bic text)))

(defn date-is-within-allowed-interval?
  "Check whether the requested execution date of a payment is within the allowed
  interval."
  [min-days max-days date]
  (let [current-date (l/local-now)
        lower-bound (t/minus current-date (t/days min-days))
        upper-bound (t/plus current-date (t/days max-days))]
    (t/within? (t/interval lower-bound upper-bound) date)))

;; The actual schema definition.

(def pain-mdr-schema
  "A Schematron-like schema for validating a ISO 20022 Payments Initiation XML message."

  (schema "ISO20022 Payments Initiation Message Schema"
          ;; Set XPath variable `CdtTrfTxInf` that points to all `<CdtTrfTxInf>` elements in the document.
          {:let {:CdtTrfTxInf "Document/CstmrCdtTrfInitn/PmtInf/CdtTrfTxInf"}
           ;; Declare the available ISO Schematron phases.
           :phases {:ISO [:Ccy :BIC :IBAN] :Date [:ReqdExctnDt]}
           ;; Use Schematron metadata.
           :see "https://www.iso20022.org/sites/default/files/documents/general/ISO20022MDR_PaymentsInitiation_2016_2017.zip"}

          (pattern "Currency"
                   {:id :Ccy
                    :see "https://www.currency-iso.org/en/home/tables/table-a1.html"}
                   ;; The `of` is an alias for an empty map. You can also use `nil` or `{}` if you want.
                   (rule "*[@Ccy]" of
                         (assert "The currency code is a valid active or historic currency code."
                                 ;; In addition to XPath assertions, you can also call Clojure functions.
                                 has-valid-currency-code?)))

          (pattern "BIC"
                   {:id  :BIC
                    :see "https://www.iso9362.org"}
                   (rule "BIC" of
                         (assert "The BIC is a valid ISO 9362 Business Identifier Code."
                                 ;; The wealth of JVM libraries is at your disposal to check your XML.
                                 has-valid-bic-code?)))

          (pattern "IBAN"
                   {:id  :IBAN
                    :see "https://www.iso.org/standard/41031.html"}
                   (rule "IBAN" of
                         (assert "The IBAN is a valid ISO 13616 International Bank Account Number."
                                 has-valid-iban-number?)))

          (pattern "Requested Execution Date"
                   {:id :ReqdExctnDt}
                   (rule "ReqdExctnDt"
                         (assert "The requested execution date must be a maximum of 5 days before or 90 days after the current date."
                                 #(date-is-within-allowed-interval? 5 90 (c/to-date (xml1-> % text))))))

          (pattern "Group Header"
                   {:id :GrpHdr}
                   (rule "GrpHdr/NbOfTxs" of
                         (assert "The reported number of transactions matches the actual number of transactions in the message."
                                 "xs:int(.) eq count($CdtTrfTxInf)"))
                   (rule "GrpHdr/CtrlSum" of
                         (assert "The reported control sum matches the actual total sum of all transactions in the message."
                                 "xs:double(.) eq sum($CdtTrfTxInf/Amt/*)")))

          (pattern "Charge Bearer"
                   {:id :ChrgBr}
                   (rule "PmtInf[ChrgBr]" of
                         (assert "If the payment defines a charge bearer, the transaction must not define it."
                                 "empty(CdtTrfTxInf/ChrgBr)"))
                   (rule "CdtTrfTxInf[ChrgBr]" of
                         (assert "If the transaction defines a charge bearer, the payment must not define it."
                                 "empty(../ChrgBr)")))

          (pattern "Charges Account"
                   {:id :ChrgsAcct}
                   (rule "ChrgsAcctAgt" of
                         (assert "If charges account agent is defined, then charges account must be defined."
                                 "exists(../ChrgsAcct)")))

          (pattern "Cheque"
                   {:id :Chq}
                   (rule "PmtInf[PmtMtd eq 'CHK']" of
                         (assert "If payment method is CHK, then transaction creditor account is not allowed."
                                 "empty(CdtTrfTxInf/CdtrAcct)"))
                   (rule "PmtInf[PmtMtd eq 'CHK'][CdtTrfTxInf/ChqInstr/DlvrTo/Cd = ('MLFA', 'CRFA', 'RGFA', 'PUFA')]"
                         (assert "If cheque delivery method is one of MLFA, CRFA, RGFA, or PUFA, then creditor agent must be defined."
                                 "exists(CdtTrfTxInf/CdtrAgt)"))
                   (rule "PmtInf[PmtMtd eq 'CHK'][not(CdtTrfTxInf/ChqInstr/DlvrTo/Cd = ('MLFA', 'CRFA', 'RGFA', 'PUFA'))]"
                         (assert "If cheque delivery method is different from MLFA, CRFA, RGFA, or PUFA, then creditor agent is not allowed."
                                 "empty(CdtTrfTxInf/CdtrAgt)"))
                   (rule "PmtInf[not(PmtMtd eq 'CHK')]"
                         (assert "If payment method is different from cheque, then cheque instruction is not allowed."
                                 "empty(CdtTrfTxInf/ChqInstr)")))

          (pattern "Credit Note"
                   {:id :CREN}
                   (rule "RmtInf[Strd/RfrdDocInf/Tp/CdOrPrtry/Cd = 'CREN']" of
                         (assert "The sum total amount of all credit notes within a transaction must be less than or equal to the sum total amount of the credit invoices within that transaction."
                                 "sum(Strd[RfrdDocInf/Tp/CdOrPrtry/Cd = 'CREN']/RfrdDocAmt/CdtNote) le sum(Strd[RfrdDocInf/Tp/CdOrPrtry/Cd = 'CINV']/RfrdDocAmt/*)")))))
