(ns missionops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 / Wave1 Lane A-no-demo): this repo previously had a hand-authored
  robotics-themed sample page and no generator at all. This namespace
  drives the REAL actor stack (`missionops.operation` ->
  `missionops.governor` -> `missionops.store`) through a scenario adapted
  from this repo's own `missionops.sim` demo driver (`clojure -M:dev:run`,
  confirmed BEFORE writing this file to produce a sensible ledger against
  the real seeded deployment ids `deployment-1`..`deployment-6` that DO
  match `missionops.store/demo-data`), trimmed to a representative subset
  (one full intake->assess->dispatch->report lifecycle, one clean
  cross-border lifecycle, and four distinct HARD-hold reasons) and
  rendered deterministically -- no invented numbers, no timestamps in the
  page content, byte-identical across reruns against the same seed
  (verify by diffing two consecutive runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [kotoba.lang.text :as str]
            [missionops.store :as store]
            [missionops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :mission-operations-coordinator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach: deployment-1 clears a full lifecycle -- intake
  (auto-commit clean at phase 3, no field-facing risk), a jurisdiction
  assessment (phase-gated -- not yet auto-eligible -- approved), a
  mission dispatch (ALWAYS escalates -- `:actuation/dispatch-mission` is
  permanently high-stakes, never auto at any phase -- approved) and a
  public report (ALWAYS escalates -- `:actuation/publish-report`, same
  posture -- approved); deployment-6 clears the cross-border path with
  notification confirmed (assess/dispatch/report each approved);
  deployment-2 HARD-holds a jurisdiction assessment with no official
  spec-basis for its (deliberately unregistered) jurisdiction;
  deployment-3 clears assess+dispatch then HARD-holds a report whose
  claimed aid value (8000.0) does not match the independently recomputed
  aid-quantity x unit-value (120 x 60 = 7200.0); deployment-4 HARD-holds
  a dispatch outside credential scope; deployment-5 HARD-holds a
  cross-border dispatch whose notification is unconfirmed. Every HARD
  hold never reaches a human. Returns the resulting store -- every field
  read by `render` below is real governor/store output, not a hand-typed
  copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "t1-intake" {:op :deployment/intake :subject "deployment-1"
                              :patch {:id "deployment-1" :mission "Kita Observer Mission"}})

    (exec! actor "t1-assess" {:op :jurisdiction/assess :subject "deployment-1"})
    (approve! actor "t1-assess")

    (exec! actor "t1-dispatch" {:op :deployment/dispatch :subject "deployment-1"})
    (approve! actor "t1-dispatch")

    (exec! actor "t1-report" {:op :deployment/report :subject "deployment-1"})
    (approve! actor "t1-report")

    (exec! actor "t6-intake" {:op :deployment/intake :subject "deployment-6"
                              :patch {:id "deployment-6" :mission "Chuo Cross-Border Aid Convoy"}})

    (exec! actor "t6-assess" {:op :jurisdiction/assess :subject "deployment-6"})
    (approve! actor "t6-assess")

    (exec! actor "t6-dispatch" {:op :deployment/dispatch :subject "deployment-6"})
    (approve! actor "t6-dispatch")

    (exec! actor "t6-report" {:op :deployment/report :subject "deployment-6"})
    (approve! actor "t6-report")

    (exec! actor "t2-assess" {:op :jurisdiction/assess :subject "deployment-2" :no-spec? true})

    (exec! actor "t3-assess" {:op :jurisdiction/assess :subject "deployment-3"})
    (approve! actor "t3-assess")

    (exec! actor "t3-dispatch" {:op :deployment/dispatch :subject "deployment-3"})
    (approve! actor "t3-dispatch")

    (exec! actor "t3-report" {:op :deployment/report :subject "deployment-3"})

    (exec! actor "t4-assess" {:op :jurisdiction/assess :subject "deployment-4"})
    (approve! actor "t4-assess")

    (exec! actor "t4-dispatch" {:op :deployment/dispatch :subject "deployment-4"})

    (exec! actor "t5-assess" {:op :jurisdiction/assess :subject "deployment-5"})
    (approve! actor "t5-assess")

    (exec! actor "t5-dispatch" {:op :deployment/dispatch :subject "deployment-5"})
    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger deployment-id]
  (last (filter #(= (:subject %) deployment-id) ledger)))

(defn- status-cell [ledger deployment-id]
  (let [f (last-fact-for ledger deployment-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (or (-> f :violations first :rule) (-> f :basis first))]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- lifecycle-cell [{:keys [dispatched? reported?]}]
  (cond
    reported? "<span class=\"ok\">dispatched &amp; reported</span>"
    dispatched? "<span class=\"warn\">dispatched, not yet reported</span>"
    :else "<span class=\"muted\">not dispatched</span>"))

(defn- deployment-row [ledger {:keys [id mission robot-type jurisdiction] :as d}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc mission) (esc (name (or robot-type :n-a))) (esc jurisdiction)
          (lifecycle-cell d)
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", ")) (some-> disposition name) ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own closed op contract
  ;; (README Actuation, `missionops.governor`/`missionops.phase`) --
  ;; documentation of fixed behavior, not runtime telemetry, so it is
  ;; legitimately hand-described rather than derived from a live run.
  ["        <tr><td><code>:deployment/intake</code></td><td><span class=\"ok\">phase-3 auto-commit when clean, no field-facing risk</span></td></tr>"
   "        <tr><td><code>:jurisdiction/assess</code></td><td><span class=\"warn\">phase-3: human approval (not yet auto-eligible) &middot; official spec-basis required</span></td></tr>"
   "        <tr><td><code>:deployment/dispatch</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; credential-scope + conditional cross-border-notification checks</span></td></tr>"
   "        <tr><td><code>:deployment/report</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; independent aid-value recompute</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        deployments (store/all-deployments db)
        deployment-rows (str/join "\n" (map (partial deployment-row ledger) deployments))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-9900 &middot; extraterritorial-mission-operations</title><style>"
   (jp-go-dds.skin/dds+skin)
   "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Extraterritorial organization &amp; body activities (ISIC 9900) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · mission dispatch/public report always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Deployments</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>missionops.store</code> via <code>missionops.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Deployment</th><th>Mission</th><th>Robot type</th><th>Jurisdiction</th><th>Dispatch/report status</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     deployment-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Mission Operations Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. Aid values are independently recomputed, never trusted from the proposal; a dispatch is blocked outright outside credential scope or with a missing cross-border notification; a report is blocked on an aid-value mismatch.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Deployment</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/dispatch-history db)) "mission dispatches,"
             (count (store/report-history db)) "public reports )")))
