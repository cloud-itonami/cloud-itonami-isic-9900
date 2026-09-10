(ns missionops.phase
  "Phase 0->3 staged rollout for the mission-operations actor.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- deployment intake allowed, every
                                 write needs human approval.
    Phase 2  assisted-assess  -- adds jurisdiction assessment writes,
                                 still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:deployment/intake` (no field-facing
                                 risk yet) may auto-commit.
                                 `:deployment/dispatch`/`:deployment/
                                 report` NEVER auto-commit, at any
                                 phase.

  `:deployment/dispatch`/`:deployment/report` are deliberately ABSENT
  from every phase's `:auto` set, including phase 3 -- a permanent
  structural fact, not a rollout milestone still to come. Dispatching
  a real mission into the field and publishing a real public report
  are the two real-world acts this actor performs; both are always a
  human mission-operations coordinator's call. `missionops.governor`'s
  `:actuation/dispatch-mission`/`:actuation/publish-report` high-
  stakes gate enforces the same invariant independently -- two
  layers, not one, agree on this. Like every prior sibling's phase 3
  `:auto` set, this domain has only ONE member (`:deployment/intake`)
  -- no separate no-field-facing-risk 'file' lifecycle distinct from
  the deployment itself.")

(def read-ops  #{})
(def write-ops #{:deployment/intake :jurisdiction/assess :deployment/dispatch :deployment/report})

;; NOTE the invariant: `:deployment/dispatch`/`:deployment/report` are
;; members of `write-ops` (governor-gated like any write) but are
;; NEVER members of any phase's `:auto` set below. Do not add them
;; there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                                                              :auto #{}}
   1 {:label "assisted-intake" :writes #{:deployment/intake}                                            :auto #{}}
   2 {:label "assisted-assess" :writes #{:deployment/intake :jurisdiction/assess}                        :auto #{}}
   3 {:label "supervised-auto" :writes write-ops
      :auto #{:deployment/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:deployment/dispatch`/`:deployment/report` are never auto-eligible
    at any phase, so they always escalate once the governor clears
    them (or hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Mission Operations Governor verdict to a base disposition
  before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
