(ns missionops.store
  "SSoT for the community-mission-operations actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/missionops/store_contract_test.clj), which is the whole
  point: the actor, the Mission Operations Governor and the audit
  ledger never know which SSoT they run on.

  Like `domesticops`/9700's own `assignment`, the primary entity here
  is a `deployment` -- mission-dispatch and public-report actuation
  events apply SEQUENTIALLY to the SAME deployment record (dispatch
  first, report later), matching the freight/quarry/agronomy/
  hospitality/practice/employment/administration/library/domestic-
  employment cluster's own sequential entity shape. Dedicated double-
  actuation-guard booleans (`:dispatched?`/`:reported?`, never a
  `:status` value).

  The ledger stays append-only on every backend: 'which deployment was
  screened for a dispatch outside credential scope or a missing
  cross-border notification, which mission was dispatched, which
  report was published, on what jurisdictional basis, approved by
  whom' is always a query over an immutable log -- the audit trail a
  mission or aid-and-relief body trusting an operator needs, and the
  evidence an operator needs if a dispatch or a report is later
  disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [missionops.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (deployment [s id])
  (all-deployments [s])
  (assessment-of [s deployment-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (dispatch-history [s] "the append-only mission-dispatch history (missionops.registry drafts)")
  (report-history [s] "the append-only public-report history (missionops.registry drafts)")
  (next-dispatch-sequence [s jurisdiction] "next dispatch-number sequence for a jurisdiction")
  (next-report-sequence [s jurisdiction] "next report-number sequence for a jurisdiction")
  (deployment-already-dispatched? [s deployment-id] "has this deployment already been dispatched?")
  (deployment-already-reported? [s deployment-id] "has this deployment already been reported?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-deployments [s deployments] "replace/seed the deployment directory (map id->deployment)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained deployment set covering both actuation
  lifecycles (dispatch, report) plus the governor's own new checks,
  so the actor + tests run offline."
  []
  {:deployments
   {"deployment-1" {:id "deployment-1" :mission "Kita Observer Mission" :robot-type :observation
                     :aid-quantity 100 :unit-value 50.0 :claimed-aid-value 5000.0
                     :within-credential-scope? true
                     :involves-cross-border-movement? false :cross-border-notification-confirmed? false
                     :dispatched? false :reported? false
                     :jurisdiction "JPN" :status :intake}
    "deployment-2" {:id "deployment-2" :mission "Atlantis Observer Mission" :robot-type :observation
                     :aid-quantity 80 :unit-value 50.0 :claimed-aid-value 4000.0
                     :within-credential-scope? true
                     :involves-cross-border-movement? false :cross-border-notification-confirmed? false
                     :dispatched? false :reported? false
                     :jurisdiction "ATL" :status :intake}
    "deployment-3" {:id "deployment-3" :mission "Minami Aid Delivery" :robot-type :aid-delivery
                     :aid-quantity 120 :unit-value 60.0 :claimed-aid-value 8000.0
                     :within-credential-scope? true
                     :involves-cross-border-movement? false :cross-border-notification-confirmed? false
                     :dispatched? false :reported? false
                     :jurisdiction "JPN" :status :intake}
    "deployment-4" {:id "deployment-4" :mission "Higashi Demining Survey" :robot-type :demining-survey
                     :aid-quantity 60 :unit-value 55.0 :claimed-aid-value 3300.0
                     :within-credential-scope? false
                     :involves-cross-border-movement? false :cross-border-notification-confirmed? false
                     :dispatched? false :reported? false
                     :jurisdiction "JPN" :status :intake}
    "deployment-5" {:id "deployment-5" :mission "Nishi Cross-Border Aid Convoy" :robot-type :aid-delivery
                     :aid-quantity 200 :unit-value 45.0 :claimed-aid-value 9000.0
                     :within-credential-scope? true
                     :involves-cross-border-movement? true :cross-border-notification-confirmed? false
                     :dispatched? false :reported? false
                     :jurisdiction "JPN" :status :intake}
    "deployment-6" {:id "deployment-6" :mission "Chuo Cross-Border Aid Convoy" :robot-type :aid-delivery
                     :aid-quantity 180 :unit-value 45.0 :claimed-aid-value 8100.0
                     :within-credential-scope? true
                     :involves-cross-border-movement? true :cross-border-notification-confirmed? true
                     :dispatched? false :reported? false
                     :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- dispatch-mission!
  "Backend-agnostic `:deployment/mark-dispatched` -- looks up the
  deployment via the protocol and drafts the dispatch record, and
  returns {:result .. :deployment-patch ..} for the caller to
  persist."
  [s deployment-id]
  (let [d (deployment s deployment-id)
        seq-n (next-dispatch-sequence s (:jurisdiction d))
        result (registry/register-dispatch deployment-id (:jurisdiction d) seq-n)]
    {:result result
     :deployment-patch {:dispatched? true
                        :dispatch-number (get result "dispatch_number")}}))

(defn- publish-report!
  "Backend-agnostic `:deployment/mark-reported` -- looks up the
  deployment via the protocol and drafts the report record, and
  returns {:result .. :deployment-patch ..} for the caller to
  persist."
  [s deployment-id]
  (let [d (deployment s deployment-id)
        seq-n (next-report-sequence s (:jurisdiction d))
        result (registry/register-report deployment-id (:jurisdiction d) seq-n)]
    {:result result
     :deployment-patch {:reported? true
                        :report-number (get result "report_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (deployment [_ id] (get-in @a [:deployments id]))
  (all-deployments [_] (sort-by :id (vals (:deployments @a))))
  (assessment-of [_ deployment-id] (get-in @a [:assessments deployment-id]))
  (ledger [_] (:ledger @a))
  (dispatch-history [_] (:dispatch-records @a))
  (report-history [_] (:report-records @a))
  (next-dispatch-sequence [_ jurisdiction] (get-in @a [:dispatch-sequences jurisdiction] 0))
  (next-report-sequence [_ jurisdiction] (get-in @a [:report-sequences jurisdiction] 0))
  (deployment-already-dispatched? [_ deployment-id] (boolean (get-in @a [:deployments deployment-id :dispatched?])))
  (deployment-already-reported? [_ deployment-id] (boolean (get-in @a [:deployments deployment-id :reported?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :deployment/upsert
      (swap! a update-in [:deployments (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :deployment/mark-dispatched
      (let [deployment-id (first path)
            {:keys [result deployment-patch]} (dispatch-mission! s deployment-id)
            jurisdiction (:jurisdiction (deployment s deployment-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:dispatch-sequences jurisdiction] (fnil inc 0))
                       (update-in [:deployments deployment-id] merge deployment-patch)
                       (update :dispatch-records registry/append result))))
        result)

      :deployment/mark-reported
      (let [deployment-id (first path)
            {:keys [result deployment-patch]} (publish-report! s deployment-id)
            jurisdiction (:jurisdiction (deployment s deployment-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:report-sequences jurisdiction] (fnil inc 0))
                       (update-in [:deployments deployment-id] merge deployment-patch)
                       (update :report-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-deployments [s deployments] (when (seq deployments) (swap! a assoc :deployments deployments)) s))

(defn seed-db
  "A MemStore seeded with the demo deployment set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :dispatch-sequences {} :dispatch-records []
                           :report-sequences {} :report-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts, dispatch/
  report records) are stored as EDN strings so `langchain.db` doesn't
  expand them into sub-entities -- the same convention every sibling
  actor's store uses."
  {:deployment/id                  {:db/unique :db.unique/identity}
   :assessment/deployment-id       {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :dispatch-record/seq            {:db/unique :db.unique/identity}
   :report-record/seq              {:db/unique :db.unique/identity}
   :dispatch-sequence/jurisdiction     {:db/unique :db.unique/identity}
   :report-sequence/jurisdiction       {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- deployment->tx [{:keys [id mission robot-type aid-quantity unit-value claimed-aid-value
                               within-credential-scope?
                               involves-cross-border-movement? cross-border-notification-confirmed?
                               dispatched? reported?
                               jurisdiction status dispatch-number report-number]}]
  (cond-> {:deployment/id id}
    mission                                       (assoc :deployment/mission mission)
    robot-type                                       (assoc :deployment/robot-type robot-type)
    aid-quantity                                        (assoc :deployment/aid-quantity aid-quantity)
    unit-value                                             (assoc :deployment/unit-value unit-value)
    claimed-aid-value                                         (assoc :deployment/claimed-aid-value claimed-aid-value)
    (some? within-credential-scope?)                             (assoc :deployment/within-credential-scope? within-credential-scope?)
    (some? involves-cross-border-movement?)                         (assoc :deployment/involves-cross-border-movement? involves-cross-border-movement?)
    (some? cross-border-notification-confirmed?)                       (assoc :deployment/cross-border-notification-confirmed? cross-border-notification-confirmed?)
    (some? dispatched?)                                                    (assoc :deployment/dispatched? dispatched?)
    (some? reported?)                                                         (assoc :deployment/reported? reported?)
    jurisdiction                                                                 (assoc :deployment/jurisdiction jurisdiction)
    status                                                                          (assoc :deployment/status status)
    dispatch-number                                                                    (assoc :deployment/dispatch-number dispatch-number)
    report-number                                                                          (assoc :deployment/report-number report-number)))

(def ^:private deployment-pull
  [:deployment/id :deployment/mission :deployment/robot-type :deployment/aid-quantity :deployment/unit-value :deployment/claimed-aid-value
   :deployment/within-credential-scope? :deployment/involves-cross-border-movement? :deployment/cross-border-notification-confirmed?
   :deployment/dispatched? :deployment/reported?
   :deployment/jurisdiction :deployment/status :deployment/dispatch-number :deployment/report-number])

(defn- pull->deployment [m]
  (when (:deployment/id m)
    {:id (:deployment/id m) :mission (:deployment/mission m) :robot-type (:deployment/robot-type m)
     :aid-quantity (:deployment/aid-quantity m) :unit-value (:deployment/unit-value m) :claimed-aid-value (:deployment/claimed-aid-value m)
     :within-credential-scope? (boolean (:deployment/within-credential-scope? m))
     :involves-cross-border-movement? (boolean (:deployment/involves-cross-border-movement? m))
     :cross-border-notification-confirmed? (boolean (:deployment/cross-border-notification-confirmed? m))
     :dispatched? (boolean (:deployment/dispatched? m)) :reported? (boolean (:deployment/reported? m))
     :jurisdiction (:deployment/jurisdiction m) :status (:deployment/status m)
     :dispatch-number (:deployment/dispatch-number m) :report-number (:deployment/report-number m)}))

(defrecord DatomicStore [conn]
  Store
  (deployment [_ id]
    (pull->deployment (d/pull (d/db conn) deployment-pull [:deployment/id id])))
  (all-deployments [_]
    (->> (d/q '[:find [?id ...] :where [?e :deployment/id ?id]] (d/db conn))
         (map #(pull->deployment (d/pull (d/db conn) deployment-pull [:deployment/id %])))
         (sort-by :id)))
  (assessment-of [_ deployment-id]
    (dec* (d/q '[:find ?p . :in $ ?did
                :where [?a :assessment/deployment-id ?did] [?a :assessment/payload ?p]]
              (d/db conn) deployment-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (dispatch-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :dispatch-record/seq ?s] [?e :dispatch-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (report-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :report-record/seq ?s] [?e :report-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-dispatch-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :dispatch-sequence/jurisdiction ?j] [?e :dispatch-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-report-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :report-sequence/jurisdiction ?j] [?e :report-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (deployment-already-dispatched? [s deployment-id]
    (boolean (:dispatched? (deployment s deployment-id))))
  (deployment-already-reported? [s deployment-id]
    (boolean (:reported? (deployment s deployment-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :deployment/upsert
      (d/transact! conn [(deployment->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/deployment-id (first path) :assessment/payload (enc payload)}])

      :deployment/mark-dispatched
      (let [deployment-id (first path)
            {:keys [result deployment-patch]} (dispatch-mission! s deployment-id)
            jurisdiction (:jurisdiction (deployment s deployment-id))
            next-n (inc (next-dispatch-sequence s jurisdiction))]
        (d/transact! conn
                     [(deployment->tx (assoc deployment-patch :id deployment-id))
                      {:dispatch-sequence/jurisdiction jurisdiction :dispatch-sequence/next next-n}
                      {:dispatch-record/seq (count (dispatch-history s)) :dispatch-record/record (enc (get result "record"))}])
        result)

      :deployment/mark-reported
      (let [deployment-id (first path)
            {:keys [result deployment-patch]} (publish-report! s deployment-id)
            jurisdiction (:jurisdiction (deployment s deployment-id))
            next-n (inc (next-report-sequence s jurisdiction))]
        (d/transact! conn
                     [(deployment->tx (assoc deployment-patch :id deployment-id))
                      {:report-sequence/jurisdiction jurisdiction :report-sequence/next next-n}
                      {:report-record/seq (count (report-history s)) :report-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-deployments [s deployments]
    (when (seq deployments) (d/transact! conn (mapv deployment->tx (vals deployments)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:deployments ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [deployments]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-deployments s deployments))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo deployment set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
