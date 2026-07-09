(ns missionops.missionopsllm
  "MissionOps-LLM client -- the *contained intelligence node* for the
  community-mission-operations actor.

  It normalizes deployment intake, drafts a per-jurisdiction
  accreditation/cross-border-notification evidence checklist, drafts
  the mission-dispatch action, and drafts the public-report action.
  CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real dispatch/publication. Every output is
  censored downstream by `missionops.governor` before anything touches
  the SSoT, and `:deployment/dispatch`/`:deployment/report` proposals
  NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/dispatch-mission | :actuation/publish-report | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [missionops.facts :as facts]
            [missionops.registry :as registry]
            [missionops.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the robot, aid quantity/unit-value or jurisdiction.
  High confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "配備記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :deployment/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-jurisdiction
  "Per-jurisdiction mission-accreditation/cross-border-notification
  evidence checklist draft. `:no-spec?` injects the failure mode we
  must defend against: proposing a checklist for a jurisdiction with
  NO official spec-basis in `missionops.facts` -- the Mission
  Operations Governor must reject this (never invent a jurisdiction's
  requirements)."
  [db {:keys [subject no-spec?]}]
  (let [d (store/deployment db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction d))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "missionops.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-dispatch
  "Draft the actual MISSION-DISPATCH action -- dispatching a real
  robot/mission into the field. ALWAYS `:stake :actuation/dispatch-
  mission` -- this is a REAL-WORLD act (a robot/mission physically
  enters the field, potentially crossing a border), never a draft the
  actor may auto-run. See README `Actuation`: no phase ever adds this
  op to a phase's `:auto` set (`missionops.phase`); the governor also
  always escalates on `:actuation/dispatch-mission`. Two independent
  layers agree, deliberately."
  [db {:keys [subject]}]
  (let [d (store/deployment db subject)]
    {:summary    (str subject " 向け派遣提案"
                      (when d (str " (mission=" (:mission d) ")")))
     :rationale  (if d
                   (str "within-credential-scope?=" (:within-credential-scope? d)
                        " involves-cross-border-movement?=" (:involves-cross-border-movement? d)
                        " cross-border-notification-confirmed?=" (:cross-border-notification-confirmed? d)
                        " jurisdiction=" (:jurisdiction d))
                   "deploymentが見つかりません")
     :cites      (if d [subject] [])
     :effect     :deployment/mark-dispatched
     :value      {:deployment-id subject}
     :stake      :actuation/dispatch-mission
     :confidence (if (and d (:within-credential-scope? d)
                       (or (not (:involves-cross-border-movement? d))
                           (:cross-border-notification-confirmed? d)))
                   0.9 0.3)}))

(defn- propose-report
  "Draft the actual PUBLIC-REPORT action -- publishing a real public
  report on a completed deployment (triggering aid-value disclosure).
  ALWAYS `:stake :actuation/publish-report` -- this is a REAL-WORLD
  act (a real public disclosure), never a draft the actor may auto-
  run. See README `Actuation`: no phase ever adds this op to a
  phase's `:auto` set (`missionops.phase`); the governor also always
  escalates on `:actuation/publish-report`. Two independent layers
  agree, deliberately."
  [db {:keys [subject]}]
  (let [d (store/deployment db subject)
        aid-value-ok? (and d (registry/aid-value-matches-claim? d))]
    {:summary    (str subject " 向け公開報告提案"
                      (when d (str " (mission=" (:mission d) ")")))
     :rationale  (if d
                   (str "claimed-aid-value=" (:claimed-aid-value d)
                        " recompute=" (registry/compute-aid-value d))
                   "deploymentが見つかりません")
     :cites      (if d [subject] [])
     :effect     :deployment/mark-reported
     :value      {:deployment-id subject}
     :stake      :actuation/publish-report
     :confidence (if aid-value-ok? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :deployment/intake        (normalize-intake db request)
    :jurisdiction/assess      (assess-jurisdiction db request)
    :deployment/dispatch      (propose-dispatch db request)
    :deployment/report        (propose-report db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは地域ミッション運用事業者の派遣・公開報告エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。"
       "説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:deployment/upsert|:assessment/set|:deployment/mark-dispatched|"
       ":deployment/mark-reported) "
       ":stake(:actuation/dispatch-mission か :actuation/publish-report か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"
       "資格証明の範囲や越境通報の完了状況を偽って報告してはいけません。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :jurisdiction/assess    {:deployment (store/deployment st subject)}
    :deployment/dispatch    {:deployment (store/deployment st subject)}
    :deployment/report      {:deployment (store/deployment st subject)}
    {:deployment (store/deployment st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Mission Operations
  Governor escalates/holds -- an LLM hiccup can never auto-dispatch a
  mission or auto-publish a report."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :missionopsllm-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
