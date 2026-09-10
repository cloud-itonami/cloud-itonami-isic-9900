(ns missionops.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls ('credentials cannot be issued outside their
  declared scope; public reports require source evidence')
  implemented faithfully. The single invariant under test:

    MissionOps-LLM never dispatches a mission or publishes a report
    the Mission Operations Governor would reject, `:deployment/
    dispatch`/`:deployment/report` NEVER auto-commit at any phase,
    `:deployment/intake` (no direct field-facing risk) MAY auto-
    commit when clean, and every decision (commit OR hold) leaves
    exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [missionops.store :as store]
            [missionops.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :mission-operations-coordinator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- dispatch!
  "Walks `subject` through dispatch -> approve, leaving :dispatched?
  true. Assumes `assess!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-dispatch") {:op :deployment/dispatch :subject subject} operator)
  (approve! actor (str tid-prefix "-dispatch")))

(defn- report!
  "Walks `subject` through report -> approve, leaving :reported? true.
  Assumes `assess!` and `dispatch!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-report") {:op :deployment/report :subject subject} operator)
  (approve! actor (str tid-prefix "-report")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :deployment/intake :subject "deployment-1"
                   :patch {:id "deployment-1" :mission "Kita Observer Mission"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Observer Mission" (:mission (store/deployment db "deployment-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "deployment-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "deployment-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "deployment-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "deployment-1")) "no assessment written"))))

(deftest dispatch-without-assessment-is-held
  (testing "deployment/dispatch before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :deployment/dispatch :subject "deployment-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest dispatch-outside-credential-scope-is-held-and-unoverridable
  (testing "a dispatch outside the mission's own declared credential scope -> HOLD, and never reaches request-approval -- the FLAGSHIP genuinely new check this vertical adds, the 92nd unconditional-evaluation-discipline grounding overall, grounded in the Vienna Convention on Diplomatic Relations 1961, the US's Foreign Missions Act, the UK's Diplomatic Privileges Act 1964 and Germany's WÜD-Ausführungsgesetz"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "deployment-4")
          res (exec-op actor "t5" {:op :deployment/dispatch :subject "deployment-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:dispatch-outside-credential-scope} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest aid-value-mismatch-is-held
  (testing "a claimed aid value that doesn't equal aid-quantity x unit-value's own recompute -> HOLD (the ground-truth-recompute discipline every sibling's cost/total-matching check establishes, this time self-contained arithmetic since no bespoke capability library exists for this vertical)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "deployment-3")
          _ (dispatch! actor "t6pre" "deployment-3")
          res (exec-op actor "t6" {:op :deployment/report :subject "deployment-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:aid-value-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/report-history db))))))

(deftest cross-border-notification-missing-is-held-and-unoverridable
  (testing "a cross-border deployment missing its own cross-border-notification confirmation -> HOLD, and never reaches request-approval -- a genuinely new check, the 93rd unconditional-evaluation-discipline grounding overall, the SIXTEENTH conditional variant (see this actor's governor ns docstring / the full accumulated ADR-0001 chain: parksafety's ADR-2607071922 Decision 5 through leathergoods's, ictrepair's, retailops's, freightops's, quarryops's, agronomyops's, hospitalityops's, practiceops's, employmentops's, adminops's, libraryops's and domesticops's own)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "deployment-5")
          res (exec-op actor "t7" {:op :deployment/dispatch :subject "deployment-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:cross-border-notification-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest dispatch-is-a-noop-when-no-cross-border-movement-involved
  (testing "the cross-border-notification check is CONDITIONAL: a deployment with no cross-border movement has no such requirement at all"
    (let [[_db actor] (fresh)
          _ (assess! actor "t7bpre" "deployment-1")
          res (exec-op actor "t7b" {:op :deployment/dispatch :subject "deployment-1"} operator)]
      (is (= :interrupted (:status res)) "clean dispatch still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest report-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-aid-value report still ALWAYS interrupts for human approval -- actuation/publish-report is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "deployment-1")
          _ (dispatch! actor "t8pre" "deployment-1")
          r1 (exec-op actor "t8" {:op :deployment/report :subject "deployment-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, report record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:reported? (store/deployment db "deployment-1"))))
          (is (= 1 (count (store/report-history db))) "one draft report record"))))))

(deftest dispatch-always-escalates-then-human-decides
  (testing "a clean, fully-assessed dispatch still ALWAYS interrupts for human approval -- actuation/dispatch-mission is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "deployment-1")
          r1 (exec-op actor "t9" {:op :deployment/dispatch :subject "deployment-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, dispatch record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:dispatched? (store/deployment db "deployment-1"))))
          (is (= 1 (count (store/dispatch-history db))) "one draft dispatch record"))))))

(deftest deployment-double-dispatch-is-held
  (testing "dispatching the same deployment record twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "deployment-1")
          _ (dispatch! actor "t10pre" "deployment-1")
          res (exec-op actor "t10" {:op :deployment/dispatch :subject "deployment-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-dispatched} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/dispatch-history db))) "still only the one earlier dispatch"))))

(deftest deployment-double-report-is-held
  (testing "publishing a report for the same deployment twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "deployment-1")
          _ (dispatch! actor "t11pre" "deployment-1")
          _ (report! actor "t11pre" "deployment-1")
          res (exec-op actor "t11" {:op :deployment/report :subject "deployment-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-reported} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/report-history db))) "still only the one earlier report"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :deployment/intake :subject "deployment-1"
                          :patch {:id "deployment-1" :mission "Kita Observer Mission"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "deployment-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
