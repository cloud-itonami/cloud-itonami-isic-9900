(ns missionops.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean deployment
  through intake -> jurisdiction assessment -> mission dispatch
  (escalate/approve/commit) -> public report (escalate/approve/
  commit), then a SEPARATE clean cross-border deployment through the
  same lifecycle (demonstrating the conditional cross-border-
  notification check passing cleanly), then shows HARD-hold
  scenarios: a jurisdiction with no spec-basis, an aid-value mismatch
  (verified first), a dispatch outside credential scope, a missing
  cross-border notification, a double dispatch, and a double report.

  Like `retailops`/4711's, `freightops`/4920's, `quarryops`/0810's,
  `agronomyops`/0162's, `hospitalityops`/5510's, `practiceops`/7110's,
  `employmentops`/7810's, `adminops`/8411's, `libraryops`/9101's and
  `domesticops`/9700's own new checks, this actor's new checks
  (`dispatch-outside-credential-scope?`, `cross-border-notification-
  missing?`) are evaluated directly at `:deployment/dispatch` time
  rather than via a separate screening op -- a real dispatch decision
  validates credential scope and cross-border-notification clearance
  at the point of the act itself. Each check is still exercised
  directly and independently below, one deployment per HARD-hold
  scenario, following the SAME 'exercise the failure mode directly,
  never only via a happy-path actuation' discipline `parksafety`'s
  ADR-2607071922 Decision 5 and every sibling since establish."
  (:require [langgraph.graph :as g]
            [missionops.store :as store]
            [missionops.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :mission-operations-coordinator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== deployment/intake deployment-1 (JPN, clean, no cross-border) ==")
    (println (exec-op actor "t1" {:op :deployment/intake :subject "deployment-1"
                                  :patch {:id "deployment-1" :mission "Kita Observer Mission"}} operator))

    (println "== jurisdiction/assess deployment-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "deployment-1"} operator))
    (println (approve! actor "t2"))

    (println "== deployment/dispatch deployment-1 (always escalates -- actuation/dispatch-mission) ==")
    (let [r (exec-op actor "t3" {:op :deployment/dispatch :subject "deployment-1"} operator)]
      (println r)
      (println "-- human mission-operations coordinator approves --")
      (println (approve! actor "t3")))

    (println "== deployment/report deployment-1 (always escalates -- actuation/publish-report) ==")
    (let [r (exec-op actor "t4" {:op :deployment/report :subject "deployment-1"} operator)]
      (println r)
      (println "-- human mission-operations coordinator approves --")
      (println (approve! actor "t4")))

    (println "== deployment/intake deployment-6 (JPN, clean, cross-border, notification confirmed) ==")
    (println (exec-op actor "t5" {:op :deployment/intake :subject "deployment-6"
                                  :patch {:id "deployment-6" :mission "Chuo Cross-Border Aid Convoy"}} operator))

    (println "== jurisdiction/assess deployment-6 (escalates -- human approves) ==")
    (println (exec-op actor "t6" {:op :jurisdiction/assess :subject "deployment-6"} operator))
    (println (approve! actor "t6"))

    (println "== deployment/dispatch deployment-6 (cross-border, notification confirmed -- escalates -- human approves) ==")
    (println (exec-op actor "t7" {:op :deployment/dispatch :subject "deployment-6"} operator))
    (println (approve! actor "t7"))

    (println "== deployment/report deployment-6 (always escalates -- human approves) ==")
    (println (exec-op actor "t7b" {:op :deployment/report :subject "deployment-6"} operator))
    (println (approve! actor "t7b"))

    (println "== jurisdiction/assess deployment-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :jurisdiction/assess :subject "deployment-2" :no-spec? true} operator))

    (println "== jurisdiction/assess deployment-3 (escalates -- human approves; sets up the aid-value-mismatch test) ==")
    (println (exec-op actor "t9" {:op :jurisdiction/assess :subject "deployment-3"} operator))
    (println (approve! actor "t9"))

    (println "== deployment/dispatch deployment-3 (always escalates -- human approves) ==")
    (println (exec-op actor "t9b" {:op :deployment/dispatch :subject "deployment-3"} operator))
    (println (approve! actor "t9b"))

    (println "== deployment/report deployment-3 (claimed 8000 vs recompute 7200 -> HARD hold) ==")
    (println (exec-op actor "t10" {:op :deployment/report :subject "deployment-3"} operator))

    (println "== jurisdiction/assess deployment-4 (escalates -- human approves; sets up the credential-scope test) ==")
    (println (exec-op actor "t11" {:op :jurisdiction/assess :subject "deployment-4"} operator))
    (println (approve! actor "t11"))

    (println "== deployment/dispatch deployment-4 (dispatch outside credential scope -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :deployment/dispatch :subject "deployment-4"} operator))

    (println "== jurisdiction/assess deployment-5 (escalates -- human approves; sets up the cross-border-notification test) ==")
    (println (exec-op actor "t13" {:op :jurisdiction/assess :subject "deployment-5"} operator))
    (println (approve! actor "t13"))

    (println "== deployment/dispatch deployment-5 (cross-border, notification missing -> HARD hold) ==")
    (println (exec-op actor "t14" {:op :deployment/dispatch :subject "deployment-5"} operator))

    (println "== deployment/dispatch deployment-1 AGAIN (double-dispatch -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :deployment/dispatch :subject "deployment-1"} operator))

    (println "== deployment/report deployment-1 AGAIN (double-report -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :deployment/report :subject "deployment-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft dispatch records ==")
    (doseq [r (store/dispatch-history db)] (println r))

    (println "== draft report records ==")
    (doseq [r (store/report-history db)] (println r))))
