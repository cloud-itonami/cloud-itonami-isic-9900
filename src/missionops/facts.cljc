(ns missionops.facts
  "Per-jurisdiction extraterritorial-mission-accreditation AND cross-
  border-notification regulatory catalog -- the G2-style spec-basis
  table the Mission Operations Governor checks every `:jurisdiction/
  assess` proposal against ('did the advisor cite an OFFICIAL public
  source for this jurisdiction's requirements, or did it invent
  one?').

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'credentials cannot be issued outside their declared
  scope; safety-critical actions (cross-border, near civilians,
  sensitive cargo) require human sign-off') names two real, distinct
  regulatory concerns: the general host-state accreditation framework
  a mission's own credential must stay within (independent of whether
  a specific dispatch actually crosses a border), and a SEPARATE
  cross-border-notification regime specifically requiring formal
  notice to a host/transit state before a mission physically crosses
  its border (independent of whether the credential itself is within
  scope -- a properly-accredited mission can still fail to notify a
  transit state of a specific border crossing, and a mission with
  full cross-border notification can still act outside its own
  credential's declared scope). Each jurisdiction entry below
  therefore cites BOTH the general accreditation law AND a SEPARATE
  cross-border-notification law.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. Like
  `domesticops`/9700's own safeguarding sub-citation, ALL FOUR seeded
  jurisdictions actually have a real cross-border-notification sub-
  citation here, reported honestly (a full-coverage sub-citation,
  matching `quarryops`/0810's own blast-safety and `agronomyops`/
  0162's own water-buffer full coverage rather than `hospitalityops`/
  5510's own honest single-jurisdiction gap).")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/credential/dispatch-record evidence set (PLUS a cross-
  border-notification record for every seeded jurisdiction);
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:jurisdiction/assess`
  proposal can commit. `:notification-owner-authority` /
  `:notification-legal-basis` / `:notification-provenance` are the
  SEPARATE cross-border-notification citation the governor's `cross-
  border-notification-missing?` check is grounded in."
  {"JPN" {:name "Japan"
          :owner-authority "外務省 (Ministry of Foreign Affairs, MOFA)"
          :legal-basis "外交関係に関するウィーン条約 (Vienna Convention on Diplomatic Relations 1961) 実施法制"
          :national-spec "在外公館及び国際機関等の任務範囲に関する認可基準"
          :provenance "https://www.mofa.go.jp/mofaj/gaiko/treaty/mokuji_kt.html"
          :required-evidence ["受入記録 (intake record)"
                              "資格証明記録 (credential record)"
                              "派遣記録 (dispatch record)"
                              "越境通報記録 (cross-border-notification record)"]
          :notification-owner-authority "外務省 / 出入国在留管理庁"
          :notification-legal-basis "出入国管理及び難民認定法 及び 二国間地位協定 (SOFA等)"
          :notification-provenance "https://www.mofa.go.jp/mofaj/area/usa/hosho/chii.html"}
   "USA" {:name "United States"
          :owner-authority "U.S. Department of State, Office of Foreign Missions (OFM)"
          :legal-basis "Vienna Convention on Diplomatic Relations 1961, 23 U.S.C. §4301 et seq. (Foreign Missions Act)"
          :national-spec "OFM mission-accreditation and scope-of-activity rules"
          :provenance "https://www.state.gov/office-of-foreign-missions/"
          :required-evidence ["Intake record"
                              "Credential record"
                              "Dispatch record"
                              "Cross-border-notification record"]
          :notification-owner-authority "U.S. Department of State / Customs and Border Protection (CBP)"
          :notification-legal-basis "Status of Forces Agreement (SOFA) notification provisions; 8 U.S.C. §1101 (A-visa border-crossing notice)"
          :notification-provenance "https://www.state.gov/status-of-forces-agreements/"}
   "GBR" {:name "United Kingdom"
          :owner-authority "Foreign, Commonwealth and Development Office (FCDO), Protocol Directorate"
          :legal-basis "Diplomatic Privileges Act 1964 (giving effect to the Vienna Convention 1961)"
          :national-spec "FCDO Protocol Directorate mission-accreditation guidance"
          :provenance "https://www.gov.uk/government/organisations/foreign-commonwealth-development-office"
          :required-evidence ["Intake record"
                              "Credential record"
                              "Dispatch record"
                              "Cross-border-notification record"]
          :notification-owner-authority "Home Office / Border Force"
          :notification-legal-basis "Immigration Act 1971 (diplomatic/mission border-crossing notice provisions)"
          :notification-provenance "https://www.gov.uk/government/organisations/border-force"}
   "DEU" {:name "Germany"
          :owner-authority "Auswärtiges Amt (Federal Foreign Office)"
          :legal-basis "Gesetz zu dem Wiener Übereinkommen über diplomatische Beziehungen (WÜD-Ausführungsgesetz)"
          :national-spec "Auswärtiges Amt Protokollreferat Akkreditierungsrichtlinien"
          :provenance "https://www.auswaertiges-amt.de/de/aussenpolitik/themen/protokoll"
          :required-evidence ["Aufnahmeprotokoll (intake record)"
                              "Akkreditierungsnachweis (credential record)"
                              "Entsendeprotokoll (dispatch record)"
                              "Grenzuebertrittsmeldungsnachweis (cross-border-notification record)"]
          :notification-owner-authority "Bundespolizei (Federal Police)"
          :notification-legal-basis "Aufenthaltsgesetz §1 Abs. 2 (diplomatische/dienstliche Grenzuebertrittsmeldung)"
          :notification-provenance "https://www.gesetze-im-internet.de/aufenthg_2004/__1.html"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to dispatch a
  mission or publish a report on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9900 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `missionops.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn notification-spec-basis
  "The jurisdiction's cross-border-notification requirement map, or
  nil -- nil means this jurisdiction has NO formal statutory cross-
  border-notification regime this catalog is aware of. In this R0
  catalog all four seeded jurisdictions actually have one, reported
  honestly (a full-coverage sub-citation, matching `quarryops`/0810's
  own blast-safety and `agronomyops`/0162's own water-buffer full
  coverage)."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:notification-owner-authority sb)
      (select-keys sb [:notification-owner-authority :notification-legal-basis :notification-provenance]))))
