(ns missionops.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:deployment/dispatch`/`:deployment/report` must NEVER
  be a member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [missionops.phase :as phase]))

(deftest deployment-dispatch-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real mission dispatch"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :deployment/dispatch))
          (str "phase " n " must not auto-commit :deployment/dispatch")))))

(deftest deployment-report-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real public-report publication"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :deployment/report))
          (str "phase " n " must not auto-commit :deployment/report")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-field-facing-risk-ops
  (testing ":deployment/intake carries no direct field-facing risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:deployment/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :deployment/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :deployment/dispatch} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :deployment/report} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :deployment/intake} :commit)))))
