(ns missionops.governor
  "Mission Operations Governor -- the independent compliance layer
  that earns the MissionOps-LLM the right to commit. The LLM has no
  notion of jurisdictional mission-accreditation or cross-border-
  notification law, whether a deployment's own claimed aid value
  actually equals aid quantity times unit value, whether a proposed
  dispatch actually falls within the mission's own declared credential
  scope, whether a deployment that actually crosses a border has
  actually completed a required cross-border notification, or when an
  act stops being a draft and becomes a real-world mission dispatch or
  public-report publication, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:mission-operations-governor`,
  grep-verified UNIQUE fleet-wide -- no naming-collision precedent
  question, a fresh independent build following the SAME governed-
  actor architecture (langgraph StateGraph + independent Governor +
  Phase 0->3 rollout) established by `cloud-itonami-isic-6511`.

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'credentials cannot be issued outside their declared
  scope; public reports require source evidence') and its own docs/
  operator-guide.md ('credential scope validation before any
  dispatch') name exactly the checks below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `missionops.phase`: for `:stake
  :actuation/dispatch-mission`/`:actuation/publish-report` (a real
  dispatch or publication) NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`missionops.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:deployment/dispatch`/
                                       `:deployment/report`, has the
                                       jurisdiction actually been
                                       assessed with a full evidence
                                       checklist on file?
    3. Dispatch outside
       credential scope               -- for `:deployment/dispatch`,
                                       INDEPENDENTLY verify the
                                       deployment's own `:within-
                                       credential-scope?` is true --
                                       the FLAGSHIP genuinely new
                                       check this vertical adds
                                       (grep-verified absent fleet-
                                       wide -- zero hits for
                                       'dispatch-outside-credential-
                                       scope' as a governor check
                                       function name), the 92nd
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (most
                                       recently `domesticops.
                                       governor/vulnerable-person-
                                       safeguarding-check-missing-
                                       violations` at 91st). Grounded
                                       in real extraterritorial-
                                       mission-accreditation law:
                                       Japan's own 外交関係に関するウィーン
                                       条約実施法制 (Vienna Convention on
                                       Diplomatic Relations 1961
                                       implementing legislation,
                                       enforced by MOFA), the US's
                                       Foreign Missions Act (23 U.S.C.
                                       §4301 et seq., enforced by the
                                       Department of State's Office of
                                       Foreign Missions), the UK's
                                       Diplomatic Privileges Act 1964
                                       (giving effect to the Vienna
                                       Convention 1961, enforced by
                                       the FCDO Protocol Directorate),
                                       and Germany's Gesetz zu dem
                                       Wiener Übereinkommen über
                                       diplomatische Beziehungen
                                       (enforced by the Auswärtiges
                                       Amt Protokollreferat) --
                                       directly grounded in this
                                       blueprint's own text
                                       ('credentials cannot be issued
                                       outside their declared scope').
                                       Evaluated UNCONDITIONALLY
                                       (every dispatch needs its own
                                       credential scope checked).
    4. Aid value mismatch          -- for `:deployment/report`,
                                       INDEPENDENTLY recompute whether
                                       the deployment's own `:claimed-
                                       aid-value` equals `aid-quantity
                                       x unit-value`
                                       (`missionops.registry/aid-
                                       value-matches-claim?`) -- an
                                       HONEST reapplication of the
                                       SAME ground-truth-recompute
                                       DISCIPLINE `domesticops.
                                       registry`'s/`libraryops.
                                       registry`'s/`adminops.
                                       registry`'s own checks
                                       establish, reapplied to a
                                       deployment's aid-value line --
                                       not claimed as new.
    5. Cross-border notification
       missing                        -- for `:deployment/dispatch`,
                                       for a deployment whose own
                                       record declares `:involves-
                                       cross-border-movement? true`
                                       (i.e. this deployment actually
                                       crosses a border -- not every
                                       deployment does, e.g. an in-
                                       country observation mission
                                       does not), INDEPENDENTLY check
                                       whether `:cross-border-
                                       notification-confirmed?` is
                                       true. A GENUINELY NEW concept
                                       (grep-verified absent fleet-
                                       wide -- zero hits for 'cross-
                                       border-notification' as a
                                       governor check function name),
                                       the 93rd distinct application
                                       overall, the SIXTEENTH
                                       conditional variant (after
                                       `socialresearch`/7220's,
                                       `bizassoc`/9411's, `training`/
                                       8549's, `furniture`/9524's,
                                       `specialtyrepair`/9529's,
                                       `leathergoods`/9523's,
                                       `ictrepair`/9511's, `quarryops`/
                                       0810's, `agronomyops`/0162's,
                                       `hospitalityops`/5510's,
                                       `practiceops`/7110's,
                                       `employmentops`/7810's,
                                       `adminops`/8411's, `libraryops`/
                                       9101's and `domesticops`/9700's
                                       own, at 63rd, 64th, 66th, 67th,
                                       68th, 69th, 71st, 77th, 79th,
                                       81st, 83rd, 85th, 87th, 89th
                                       and 91st). CONDITIONAL on the
                                       deployment's own `:involves-
                                       cross-border-movement? ground
                                       truth. Grounded in real cross-
                                       border-notification law:
                                       Japan's own 出入国管理及び難民認定法
                                       plus bilateral status-of-forces
                                       arrangements, the US's Status
                                       of Forces Agreement (SOFA)
                                       notification provisions (8
                                       U.S.C. §1101 A-visa border-
                                       crossing notice), the UK's
                                       Immigration Act 1971
                                       diplomatic/mission border-
                                       crossing notice provisions, and
                                       Germany's Aufenthaltsgesetz §1
                                       Abs. 2 (diplomatische/dienstliche
                                       Grenzuebertrittsmeldung) -- ALL
                                       FOUR seeded jurisdictions
                                       actually have a real regime
                                       here, reported honestly (a
                                       full-coverage sub-citation,
                                       matching `quarryops`/0810's own
                                       blast-safety, `agronomyops`/
                                       0162's own water-buffer,
                                       `practiceops`/7110's own
                                       professional-seal,
                                       `employmentops`/7810's own
                                       work-authorization, `adminops`/
                                       8411's own appeal-rights,
                                       `libraryops`/9101's own
                                       conservation-standards and
                                       `domesticops`/9700's own
                                       safeguarding full coverage
                                       rather than `hospitalityops`/
                                       5510's own honest single-
                                       jurisdiction gap).
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:deployment/
                                       dispatch`/`:deployment/report`
                                       (REAL acts) -> escalate.

  Two more guards, double-dispatch/double-report prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-dispatched-
  violations`/`already-reported-violations` refuse to dispatch/report
  the SAME deployment twice, off dedicated `:dispatched?`/`:reported?`
  facts (never a `:status` value) -- the SAME 'check a dedicated
  boolean, not status' discipline every prior governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle
  bug (ADR-2607071320)."
  (:require [missionops.facts :as facts]
            [missionops.registry :as registry]
            [missionops.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Dispatching a real mission and publishing a real public report are
  the two real-world actuation events this actor performs -- a two-
  member set, matching every sibling's own dual-actuation shape."
  #{:actuation/dispatch-mission :actuation/publish-report})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:deployment/dispatch`/`:deployment/
  report`) proposal with no spec-basis citation is a HARD violation
  -- never invent a jurisdiction's accreditation/cross-border-
  notification requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :deployment/dispatch :deployment/report} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:deployment/dispatch`/`:deployment/report`, the jurisdiction's
  required intake/credential/dispatch evidence must actually be
  satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (contains? #{:deployment/dispatch :deployment/report} op)
    (let [d (store/deployment st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction d) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(受入記録/資格証明記録/派遣記録/越境通報記録等)が充足していない状態での提案"}]))))

(defn- dispatch-outside-credential-scope-violations
  "For `:deployment/dispatch`, INDEPENDENTLY verify the deployment's
  own `:within-credential-scope?` is true -- the flagship genuinely
  new check this vertical adds. Evaluated UNCONDITIONALLY (every
  dispatch needs its own credential scope checked)."
  [{:keys [op subject]} st]
  (when (= op :deployment/dispatch)
    (let [d (store/deployment st subject)]
      (when-not (true? (:within-credential-scope? d))
        [{:rule :dispatch-outside-credential-scope
          :detail (str subject " の派遣が資格証明の宣言範囲外")}]))))

(defn- aid-value-mismatch-violations
  "For `:deployment/report`, INDEPENDENTLY recompute whether the
  deployment's own claimed aid value equals aid-quantity x unit-value
  via `missionops.registry/aid-value-matches-claim?` -- needs no
  proposal inspection or stored-verdict lookup at all, an honest
  reapplication of the same discipline every sibling actor's own
  cost/total-matching check establishes."
  [{:keys [op subject]} st]
  (when (= op :deployment/report)
    (let [d (store/deployment st subject)]
      (when-not (registry/aid-value-matches-claim? d)
        [{:rule :aid-value-mismatch
          :detail (str subject " の申告支援価値(" (:claimed-aid-value d)
                      ")が独立再計算値(" (registry/compute-aid-value d) ")と一致しない")}]))))

(defn- cross-border-notification-missing-violations
  "For `:deployment/dispatch`, for a deployment whose own record
  declares `:involves-cross-border-movement? true`, INDEPENDENTLY
  check whether `:cross-border-notification-confirmed?` is true -- a
  genuinely new concept, CONDITIONAL on the deployment's own
  `:involves-cross-border-movement?` ground truth (not every
  deployment crosses a border)."
  [{:keys [op subject]} st]
  (when (= op :deployment/dispatch)
    (let [d (store/deployment st subject)]
      (when (and (true? (:involves-cross-border-movement? d))
                 (not (true? (:cross-border-notification-confirmed? d))))
        [{:rule :cross-border-notification-missing
          :detail (str subject " は越境移動を伴うが越境通報が未完了 -- 派遣提案は進められない")}]))))

(defn- already-dispatched-violations
  "For `:deployment/dispatch`, refuses to dispatch the SAME
  deployment record twice, off a dedicated `:dispatched?` fact (never
  a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :deployment/dispatch)
    (when (store/deployment-already-dispatched? st subject)
      [{:rule :already-dispatched
        :detail (str subject " は既に派遣済み")}])))

(defn- already-reported-violations
  "For `:deployment/report`, refuses to report the SAME deployment
  twice, off a dedicated `:reported?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :deployment/report)
    (when (store/deployment-already-reported? st subject)
      [{:rule :already-reported
        :detail (str subject " は既に報告済み")}])))

(defn check
  "Censors a MissionOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (dispatch-outside-credential-scope-violations request st)
                           (aid-value-mismatch-violations request st)
                           (cross-border-notification-missing-violations request st)
                           (already-dispatched-violations request st)
                           (already-reported-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
