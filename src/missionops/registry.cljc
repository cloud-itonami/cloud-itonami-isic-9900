(ns missionops.registry
  "Pure-function mission-dispatch + public-report record construction
  -- an append-only mission-operations book-of-record draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a dispatch or report record -- every
  mission/jurisdiction assigns its own reference format. This
  namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the
  same honest, non-fabricating discipline `missionops.facts` uses.

  `aid-value-matches-claim?` is an HONEST reapplication of the SAME
  ground-truth-recompute DISCIPLINE `domesticops.registry`'s own
  `payroll-matches-contract?`, `libraryops.registry`'s own `late-fee-
  matches-claim?`, `adminops.registry`'s own `assessed-fee-matches-
  claim?` and every other sibling actor's own cost/total-matching
  check establish (verify a claimed monetary total against the
  entity's own recorded quantity x unit fields), reapplied to a
  deployment's aid-value line rather than a payroll, late-fee or
  assessed-fee line -- not claimed as new code, though no literal
  code is shared (different domain). Unlike `domesticops`/9700's own
  reapplication (which delegated to a REAL capability library,
  `kotoba.labor/wages-for`), this vertical has no bespoke capability
  library to delegate to (see docs/adr/0001-architecture.md Decision
  9), so the arithmetic is self-contained, matching the majority of
  this fleet's actors.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real mission-operations system. It builds the RECORD an
  operator would keep, not the act of dispatching a mission or
  publishing a report itself (that is `missionops.operation`'s
  `:deployment/dispatch`/`:deployment/report`, always human-gated --
  see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the mission operator's act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-aid-value
  "The ground-truth aid value for `deployment`'s own `:aid-quantity`
  and `:unit-value` -- a single flat quantity x unit-value
  calculation, not a full customs/valuation engine."
  [{:keys [aid-quantity unit-value]}]
  (* (double aid-quantity) (double unit-value)))

(defn aid-value-matches-claim?
  "Does `deployment`'s own `:claimed-aid-value` equal the
  independently recomputed `compute-aid-value`? A pure ground-truth
  check against the deployment's own permanent fields -- see ns
  docstring for why this is an honest reapplication of the SAME
  discipline every sibling actor's own cost/total-matching check
  establishes, not a new concept."
  [{:keys [claimed-aid-value] :as deployment}]
  (== (double claimed-aid-value) (double (compute-aid-value deployment))))

(defn register-dispatch
  "Validate + construct the MISSION-DISPATCH registration DRAFT -- the
  mission operator's own act of dispatching a real robot/mission
  (aid delivery, observation, demining-survey) into the field. Pure
  function -- does not touch any real mission-operations system; it
  builds the RECORD an operator would keep. `missionops.governor`
  independently re-verifies the deployment's own credential-scope and
  cross-border-notification ground truth, and blocks a double-
  dispatch of the same record, before this is ever allowed to
  commit."
  [deployment-id jurisdiction sequence]
  (when-not (and deployment-id (not= deployment-id ""))
    (throw (ex-info "dispatch: deployment_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "dispatch: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "dispatch: sequence must be >= 0" {})))
  (let [dispatch-number (str (str/upper-case jurisdiction) "-DSP-" (zero-pad sequence 6))
        record {"record_id" dispatch-number
                "kind" "dispatch-draft"
                "deployment_id" deployment-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "dispatch_number" dispatch-number
     "certificate" (unsigned-certificate "MissionDispatch" dispatch-number dispatch-number)}))

(defn register-report
  "Validate + construct the PUBLIC-REPORT registration DRAFT -- the
  mission operator's own act of publishing a real public report on a
  completed deployment (triggering aid-value disclosure). Pure
  function -- does not touch any real publication system; it builds
  the RECORD an operator would keep. `missionops.governor`
  independently re-verifies the deployment's own aid-value ground
  truth, and blocks a double-publication of the same record, before
  this is ever allowed to commit."
  [deployment-id jurisdiction sequence]
  (when-not (and deployment-id (not= deployment-id ""))
    (throw (ex-info "report: deployment_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "report: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "report: sequence must be >= 0" {})))
  (let [report-number (str (str/upper-case jurisdiction) "-RPT-" (zero-pad sequence 6))
        record {"record_id" report-number
                "kind" "report-draft"
                "deployment_id" deployment-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "report_number" report-number
     "certificate" (unsigned-certificate "PublicReport" report-number report-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
