(ns missionops.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [missionops.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/deployment s "deployment-1"))))
      (is (= 5000.0 (:claimed-aid-value (store/deployment s "deployment-1"))))
      (is (true? (:within-credential-scope? (store/deployment s "deployment-1"))))
      (is (false? (:involves-cross-border-movement? (store/deployment s "deployment-1"))))
      (is (= 8000.0 (:claimed-aid-value (store/deployment s "deployment-3"))))
      (is (false? (:within-credential-scope? (store/deployment s "deployment-4"))))
      (is (true? (:involves-cross-border-movement? (store/deployment s "deployment-5"))))
      (is (false? (:cross-border-notification-confirmed? (store/deployment s "deployment-5"))))
      (is (true? (:cross-border-notification-confirmed? (store/deployment s "deployment-6"))))
      (is (false? (:dispatched? (store/deployment s "deployment-1"))))
      (is (false? (:reported? (store/deployment s "deployment-1"))))
      (is (= ["deployment-1" "deployment-2" "deployment-3" "deployment-4" "deployment-5" "deployment-6"]
             (mapv :id (store/all-deployments s))))
      (is (nil? (store/assessment-of s "deployment-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/dispatch-history s)))
      (is (= [] (store/report-history s)))
      (is (zero? (store/next-dispatch-sequence s "JPN")))
      (is (zero? (store/next-report-sequence s "JPN")))
      (is (false? (store/deployment-already-dispatched? s "deployment-1")))
      (is (false? (store/deployment-already-reported? s "deployment-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :deployment/upsert
                                 :value {:id "deployment-1" :mission "Kita Observer Mission"}})
        (is (= "Kita Observer Mission" (:mission (store/deployment s "deployment-1"))))
        (is (= 5000.0 (:claimed-aid-value (store/deployment s "deployment-1"))) "unrelated field preserved"))
      (testing "assessment payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["deployment-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "deployment-1"))))
      (testing "dispatch drafts a record and advances the dispatch sequence"
        (store/commit-record! s {:effect :deployment/mark-dispatched :path ["deployment-1"]})
        (is (= "JPN-DSP-000000" (get (first (store/dispatch-history s)) "record_id")))
        (is (= "dispatch-draft" (get (first (store/dispatch-history s)) "kind")))
        (is (true? (:dispatched? (store/deployment s "deployment-1"))))
        (is (= 1 (count (store/dispatch-history s))))
        (is (= 1 (store/next-dispatch-sequence s "JPN")))
        (is (true? (store/deployment-already-dispatched? s "deployment-1"))))
      (testing "report publication drafts a record and advances the report sequence"
        (store/commit-record! s {:effect :deployment/mark-reported :path ["deployment-1"]})
        (is (= "JPN-RPT-000000" (get (first (store/report-history s)) "record_id")))
        (is (= "report-draft" (get (first (store/report-history s)) "kind")))
        (is (true? (:reported? (store/deployment s "deployment-1"))))
        (is (= 1 (count (store/report-history s))))
        (is (= 1 (store/next-report-sequence s "JPN")))
        (is (true? (store/deployment-already-reported? s "deployment-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/deployment s "nope")))
    (is (= [] (store/all-deployments s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/dispatch-history s)))
    (is (= [] (store/report-history s)))
    (is (zero? (store/next-dispatch-sequence s "JPN")))
    (is (zero? (store/next-report-sequence s "JPN")))
    (store/with-deployments s {"x" {:id "x" :mission "m" :robot-type :observation
                                    :aid-quantity 1 :unit-value 1.0 :claimed-aid-value 1.0
                                    :within-credential-scope? true
                                    :involves-cross-border-movement? false :cross-border-notification-confirmed? false
                                    :dispatched? false :reported? false
                                    :jurisdiction "JPN" :status :intake}})
    (is (= "m" (:mission (store/deployment s "x"))))))
