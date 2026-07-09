(ns missionops.registry-test
  (:require [clojure.test :refer [deftest is]]
            [missionops.registry :as r]))

;; ----------------------------- aid-value-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/aid-value-matches-claim?
       {:id "deployment-1" :aid-quantity 100 :unit-value 50.0 :claimed-aid-value 5000.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/aid-value-matches-claim?
            {:id "deployment-3" :aid-quantity 120 :unit-value 60.0 :claimed-aid-value 8000.0}))))

(deftest compute-aid-value-is-quantity-times-unit-value
  (is (= 5000.0 (r/compute-aid-value
                 {:id "deployment-1" :aid-quantity 100 :unit-value 50.0}))))

;; ----------------------------- register-dispatch -----------------------------

(deftest dispatch-is-a-draft-not-a-real-dispatch
  (let [result (r/register-dispatch "deployment-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest dispatch-assigns-dispatch-number
  (let [result (r/register-dispatch "deployment-1" "JPN" 7)]
    (is (= (get result "dispatch_number") "JPN-DSP-000007"))
    (is (= (get-in result ["record" "deployment_id"]) "deployment-1"))
    (is (= (get-in result ["record" "kind"]) "dispatch-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest dispatch-validation-rules
  (is (thrown? Exception (r/register-dispatch "" "JPN" 0)))
  (is (thrown? Exception (r/register-dispatch "deployment-1" "" 0)))
  (is (thrown? Exception (r/register-dispatch "deployment-1" "JPN" -1))))

;; ----------------------------- register-report -----------------------------

(deftest report-is-a-draft-not-a-real-publication
  (let [result (r/register-report "deployment-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest report-assigns-report-number
  (let [result (r/register-report "deployment-1" "JPN" 7)]
    (is (= (get result "report_number") "JPN-RPT-000007"))
    (is (= (get-in result ["record" "deployment_id"]) "deployment-1"))
    (is (= (get-in result ["record" "kind"]) "report-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest report-validation-rules
  (is (thrown? Exception (r/register-report "" "JPN" 0)))
  (is (thrown? Exception (r/register-report "deployment-1" "" 0)))
  (is (thrown? Exception (r/register-report "deployment-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-dispatch "deployment-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-dispatch "deployment-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-DSP-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-DSP-000001" (get-in hist2 [1 "record_id"])))))
