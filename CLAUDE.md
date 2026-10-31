# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Produkt

Wissli ist eine Android-App rund um Wissensfragen ("Warum ist der Himmel blau?") mit
altersgerechten Erklärungen. Nutzer:innen organisieren sich in frei benennbaren **Gruppen**
(z. B. "Familie Scholler", aber genauso ein Freundeskreis oder jede andere Konstellation) —
keine feste Family-/Parent-Child-Hierarchie. Kernfeature ist der **Solo-Fortschritt**: Fragen
gehören zu Themen (z. B. "Natur"), jedes Thema hat Level à 20 Fragen; wer alle Fragen eines
Levels gelöst hat, schaltet das nächste frei und steigt pro Thema im **Rang** auf (Neuling →
Entdecker → Kenner → Experte → Meister). Nach jeder Antwort gibt es Feedback und die Erklärung.
Später kommt ein **Gruppenmodus** dazu, der die früher geplanten 1:1-Challenges ersetzt: eigener
Fragenpool (zählt nicht für den Solo-Fortschritt), alle beantworten parallel mit Timer pro
Frage, danach sieht man, wer richtig lag; Schwierigkeit leicht/mittel/schwer/gemischt. Ziel ist
eine veröffentlichbare Play-Store-App, langfristig mit KI- und Premium-Funktionen.

## Scope

Aktuell **ausschließlich natives Android** (kein iOS, kein KMP/Compose Multiplatform –
der Entwickler hat keinen Mac). Die Architektur muss trotzdem so sauber getrennt sein
(Repository-Abstraktionen, keine UI-Kopplung an Datenquellen), dass eine spätere
Portierung nicht durch Android-spezifische Kurzschlüsse blockiert wird.

Repo-Realität: Alle Ziel-Libraries (Koin, Room, Navigation Compose, Firebase, Coroutines,
Turbine) sind im Version Catalog gepflegt und im `app`-Modul als Dependencies eingebunden.
Implementiert ist bisher der **Auth-Flow** (Google Sign-In) als Referenz-Feature für alle
folgenden Features: `WissliNavHost` → `AuthGateViewModel` (liegt bewusst in `core/navigation/` statt in `data/` oder
`feature/`, da es kein Screen ist, sondern die Root-Navigation steuert; beobachtet
`AuthRepository.observeCurrentUser()` und entscheidet die Start-Destination — `Route.Login`
oder `Route.Home` — statt beim App-Start immer hart bei Login zu starten; nur der *erste*
Status zählt, spätere Login-/Logout-Wechsel navigiert der Graph explizit) → `LoginScreen` →
`LoginViewModel` → `AuthRepository`/`GoogleAuthDataSource`. `Route.Home` zeigt das Dashboard
(`features/dashboard/`, Design-Stand): Header mit Gruppenwahl + Begrüßung (Tipp auf den Avatar öffnet
`Route.Profile`), "Weiter lernen"-Karte, "Frage des Tages" (direkt in der Karte beantwortbar),
"Deine Lern-Übersicht" (`LearningStatsCard`) und `WissliBottomBar` (`core/navigation/`, Tabs Start/Lernen/Gruppen; Gruppen
noch ohne Ziel). Der Solo-Fortschritt besteht aus `features/learn/` (Tab "Lernen": Themen mit
Rang + Weiter-Karte), `features/category/` (Rang-Leiter + Level-Pfad eines Themas) und
`features/level/` (Level spielen; falsch beantwortete Fragen kommen ans Ende, Level endet erst,
wenn alle gelöst sind; danach Abschluss mit Rang-Aufstieg; geschaffte Level lassen sich ohne
Fortschrittsänderung üben). `LevelViewModel` nutzt einen reinen `LevelReducer`. Fortschritts-
und Rang-Logik liegt im Modell (`CategoryProgress.withSolved()`, `Rank.forCompletedLevels()`).
ViewModels lesen über `GroupRepository`/`QuestionRepository`/`ProgressRepository`
(`data/group|question|progress/`). `GroupRepository` ist aktuell nur durch
`PlaceholderGroupRepository` gebunden (statische Beispieldaten) — für Firestore nur die Bindung in
`di/GroupModule.kt` austauschen. `QuestionRepository` ist durch `RoomQuestionRepository` gebunden
(siehe "Fragen-Content"), `ProgressRepository` durch `OfflineFirstProgressRepository` (siehe
"Solo-Fortschritt speichern"). Wiederverwendbare UI-Bausteine
(Karten-Stapel, Initialen-Avatar, Logo-Kachel, Rang-Badge/-Emblem, Fortschrittsbalken) liegen
unter `ui/components/`.
Der Login-Screen hat zwei Varianten: Erst-Login ("Mit Google anmelden") und "Willkommen zurück"
mit Konto-Karte ("Weiter als …" / "Anderes Konto verwenden"). Dafür merkt sich
`FirebaseAuthRepository` nach jedem Login Anzeigename + E-Mail des Kontos über
`LastAccountLocalDataSource` (SharedPreferences, nur auf dem Gerät, vom Backup ausgenommen) —
die E-Mail landet bewusst nie in Firestore.
`FirebaseAuthRepository` ist die einzige Stelle, die `FirebaseAuth`/`FirebaseFirestore` anfasst;
`CredentialManagerGoogleAuthDataSource` holt das Google-ID-Token über die Android Credential
Manager API und ist bewusst von `AuthRepository` getrennt (reine Plattform-API, kein
Firebase-Zugriff). Koin ist initialisiert (`WissliApplication` startet `startKoin { ... }`) mit
`di/AppModule.kt` (`appModule`), das Bereichs-Module per `includes()` einhängt (`authModule`,
`databaseModule`, `settingsModule`, `groupModule`, `questionModule`, `progressModule`, `statsModule`, `dashboardModule`,
`learnModule`, `profileModule`).
**Frage des Tages**: `DailyQuestionRepository` (`data/daily/`, gebunden an `LocalDailyQuestionRepository`)
wählt pro Kalendertag (lokale Zeit) zufällig eine Frage aus allen Themen, nie dieselbe wie am Vortag,
und merkt sich Tag, Frage und gegebene Antwort per `DailyQuestionStore` (SharedPreferences, pro
Gerät, nicht pro User). Zählt nicht für den Solo-Fortschritt; um Mitternacht kommt die nächste Frage.
**Lern-Übersicht**: `StatsRepository` (`data/stats/`, gebunden an `LocalStatsRepository`) schreibt jede
Level-Antwort (richtig oder falsch, auch beim Üben) in die Room-Tabelle `answers` (pro `userId`, mit lokalem
`epochDay`) und berechnet daraus per `learningStats()` (`core/model/LearningStats.kt`) Trefferquote gesamt
und pro Thema, Tages-Serie und die letzten 7 Tage; "gelöste Fragen" kommt aus dem Fortschritt. Nur auf dem
Gerät, noch ohne Firestore-Sync; die Frage des Tages zählt nicht mit. `MemberCard` ist aus dem Dashboard
raus und wartet auf den Gruppen-Tab.
**Profil** (`features/profile/`): App-Sprache DE/EN, Benachrichtigungen an/aus (nur gespeichert, noch
ohne Funktion, kommt mit Gruppen in Firestore) und Abmelden. Einstellungen über `SettingsRepository`
(`data/settings/`): die Sprache ist die Per-App-Language von AppCompat
(`AppCompatDelegate.setApplicationLocales`, `MainActivity` ist deshalb eine `AppCompatActivity` mit
AppCompat-Theme, `generateLocaleConfig` + `res/resources.properties`); `RoomQuestionRepository` liest
Fragen- und Themen-Texte in der aktuellen Sprache.
Noch **nicht implementiert**: Favorites, Groups (inkl. Gruppenmodus), Push-Benachrichtigungen,
echte Firestore-Repositories außer Auth und Fortschritt, Remote-Updates des Fragen-Pakets.
Beim Lesen des Codes den tatsächlichen Stand prüfen statt dies als vollständig anzunehmen.

Konvention für neue Features (siehe `features/login/` als Vorlage): pro Feature ein flaches
Paket `features/<feature>/` mit zwei Unterpaketen:
- `presentation/` — MVI-Kern: `<Screen>Contract.kt` (immutable State + sealed `Intent`/`Effect`),
  `<Screen>ViewModel.kt` (nimmt Intents via `onIntent()` entgegen, emittiert State über
  `StateFlow` und einmalige Effekte über einen `SharedFlow`), ggf. `<Screen>Reducer.kt` (siehe
  "Architektur").
- `ui/` — `<Screen>Screen.kt` (Composable, holt das ViewModel per `koinViewModel()`, sammelt
  Effects in einem `LaunchedEffect`) plus screen-spezifische Composables.

Bewusst **kein** `data/`- oder `domain/`-Unterpaket pro Feature: Repositories werden
feature-übergreifend genutzt (z. B. `QuestionRepository` von Questions, Favorites und
Challenges) und liegen deshalb zentral unter `data/<bereich>/`; eine Use-Case-Schicht gibt es
nicht (siehe "Architektur"). Tests spiegeln das Main-Paket (z. B.
`app/src/test/.../features/login/presentation/LoginViewModelTest.kt`). Pro fachlichem Bereich
ein eigenes `di/<Bereich>Module.kt`, das in `appModule` inkludiert wird — kein monolithisches
DI-Modul.

Firebase-Projekt existiert (`wii-21c9b`), `app/google-services.json` liegt im Repo,
`google-services`- und `firebase-crashlytics`-Plugin sind in `app/build.gradle.kts` aktiv.
**Offene Entscheidung**: `app/google-services.json` ist aktuell mit committed — laut Firebase
offiziell kein echtes Secret (API Key ist über Package-Name/SHA-Fingerprint + Firestore-Regeln
geschützt, nicht durch Geheimhaltung der Datei), aber bewusst noch nicht final entschieden, ob
sie stattdessen wie `local.properties`/`GOOGLE_WEB_CLIENT_ID` aus dem Repo rausgehalten werden
soll (dann bräuchte die CI-Action einen GitHub Secret, der die Datei zur Build-Zeit
rekonstruiert). `GOOGLE_WEB_CLIENT_ID` in `local.properties` bleibt trotzdem separat nötig —
das ist die *Web*-Client-ID für den Credential-Manager-Flow, nicht in `google-services.json`
enthalten (siehe "Commands" unten).

## Tech Stack

Kotlin (aktuelle stabile Version) · Jetpack Compose + Material 3 · Coroutines/Flow/StateFlow ·
Navigation Compose · Koin (DI) · Room (lokale Persistenz) · Firebase: Auth, Firestore, Cloud
Messaging, Storage, Crashlytics, Analytics, App Check, Emulator Suite für lokale Entwicklung ·
Gradle Kotlin DSL mit Version Catalog (`gradle/libs.versions.toml`).

Keine unnötigen Zusatzbibliotheken — Jetpack-/Google-Lösungen bevorzugen. Ausnahme ist die
bewusste Entscheidung für Koin statt Hilt als DI-Framework (kein Codegen/KSP nötig,
einfachere Modul-Definition per Kotlin-DSL). SQLDelight nur statt Room, wenn dafür ein
konkreter, im PR/Commit begründeter Grund vorliegt.

**Room 3.0** (`androidx.room3`, seit 07/2026 stabil; Room 2.x nur noch Maintenance-Modus) statt
Room 2.x — bewusst früh gewechselt, um eine spätere Migration zu vermeiden. Bisher einzige
Datenbank: `data/local/WissliDatabase` (Fragen-Content und Solo-Fortschritt, siehe unten); Schema-
Änderungen über `autoMigrations` im `@Database` (Version 1 → 2 hat die Tabelle `progress` ergänzt,
2 → 3 die Texte in die Tabellen `category_names`/`question_texts` pro Sprache verschoben, mit
`LocalizedContentMigration` als `AutoMigrationSpec`, 3 → 4 die Tabelle `answers`),
nie destruktiv, weil dort Fortschritt liegt. Wichtige Breaking Changes
ggü. Room 2.x, die bei neuen Entities/DAOs zu beachten sind:
- Nur noch KSP, kein KAPT/Java-Codegen (KSP ist bereits eingerichtet).
- DAO-Funktionen müssen `suspend` sein (außer beobachtbare Rückgabetypen wie `Flow`).
- `Room.databaseBuilder(...)` braucht zwingend `.setDriver(BundledSQLiteDriver())`
  (`androidx.sqlite:sqlite-bundled`) — bewusst der bundled statt der Android-Framework-Treiber,
  passend zum Portierbarkeits-Anspruch oben (siehe "Scope").
- Schema-Export ist per KSP-Arg `room.schemaLocation` auf `app/schemas/` konfiguriert — die
  generierten Schema-JSONs committen (Grundlage für spätere Auto-Migrations).
- Package-Imports sind `androidx.room3.*` statt `androidx.room.*`; `@TypeConverter` heißt jetzt
  `@ColumnTypeConverter`.

## Architektur

```
UI (Compose) → ViewModel → Repository → Data Sources (Firebase remote / Room local)
```

- MVI-orientiert: **immutable UI State** + **sealed Events/Intents** pro Screen, State-Fluss
  unidirektional über `StateFlow`. Für einfache Screens (wenige Intents, State-Übergänge ohne
  Überschneidungen — siehe `features/login/`) reicht `_state.update { ... }` direkt inline
  in der Intent-Behandlung des ViewModels, ohne separaten Reducer. Sobald ein Screen komplexer
  wird (viele Intents/Seiteneffekte, die den State auf überlappende Weise verändern — z. B.
  Timer + Netzwerk-Sync + User-Eingaben gleichzeitig), einen expliziten, reinen Reducer
  einführen: `reduce(state: State, event: Event): State` ohne Seiteneffekte; Seiteneffekte
  (Netzwerk-/Repository-Aufrufe) laufen separat und lösen anschließend ein Result-Event aus,
  das durch den Reducer geht. Das macht State-Übergänge zentral nachvollziehbar und pur
  testbar, sobald Inline-Updates unübersichtlich würden.
- Repository Pattern; Business-Logik liegt im Repository oder in klar benannten, kleinen
  Domain-/Utility-Komponenten — **keine** Clean-Architecture-Use-Case/Interactor-Schicht.
  `ViewModel → Repository` direkt, niemals `ViewModel → UseCase → Repository`. Das ist eine
  bewusste, wiederholt bestätigte Entscheidung — beim Anlegen neuer Features keine
  Use-Case-Klassen einführen, auch nicht "nur für diesen einen Fall".
- Firebase-/Room-Zugriffe ausschließlich hinter Repository-/DataSource-Interfaces. Keine
  Firebase- oder DB-Aufrufe direkt aus Composables oder ViewModels.
- Keine Business-Logik in Composables. Keine unnötigen Singletons, keine God Classes, keine
  Magic Strings, keine hardcodierten Firebase-IDs, keine Secrets im Repo.
- Code-Kommentare (inkl. KDoc, Log- und Exception-Texte, Testnamen) auf **Englisch** und nur dort,
  wo sie ein nicht offensichtliches Warum erklären — keine Kommentare, die nur den Code nacherzählen.
- UI-Texte ausschließlich als String-Ressourcen, immer zweisprachig: `values/strings.xml`
  (Englisch, Default) und `values-de/strings.xml` (Deutsch). Nicht übersetzbare Strings
  (Markennamen o. Ä.) mit `translatable="false"` nur in `values/`.

## Domain-Modelle (Kern)

- **User**: `id, displayName, createdAt`. Keine Rolle und keine Gruppenzugehörigkeit auf dem
  User selbst — beides lebt pro Mitgliedschaft in `GroupMember`, da ein User Mitglied mehrerer
  Gruppen sein kann. So wenig personenbezogene Daten wie möglich über `displayName` hinaus
  (kein voller Name, keine Adresse, kein Geburtsdatum).
- **Group**: `id, name, createdBy, createdAt`. Ersetzt das frühere starre Family-Konzept
  (Parent → Child) — eine Gruppe hat keinen festen Typ, kann eine Familie, ein Freundeskreis
  oder jede andere Konstellation sein. Jede:r User kann beliebig viele Gruppen erstellen.
- **GroupMember**: Mitgliedschaft + Rolle einer Person in einer Gruppe — `userId, role,
  displayName, joinedAt` (+ `inviteCode` beim Beitritt); `GroupRole = OWNER | MEMBER`. `OWNER` ist
  automatisch, wer die Gruppe erstellt hat, und einzige Rolle, die Einladungscodes erzeugen,
  Mitglieder entfernen oder die Gruppe umbenennen/löschen darf. `displayName` ist eine Kopie aus dem
  Profil, weil `users/{uid}` privat ist.
- **InviteCode**, **Round**, **RoundParticipant**: Gruppenmodus, siehe "Gruppen & Gruppenmodus".
- **Question**: globaler, gruppenunabhängiger Content — `id, title, explanation,
  categoryId, level, order, answers, correctAnswerIndex, ageFrom, ageTo, difficulty, answerType,
  imageUrl?, published, premium, createdAt, updatedAt`. `level` + `order` legen die Position im
  Level-Pfad des Themas fest (`QUESTIONS_PER_LEVEL = 20`). Im Code (`core/model/Question.kt`)
  bisher nur die Felder, die das Spielen braucht.
  Fragen-IDs sind eine fortlaufende Nummer pro Thema (`nature-0001`), unabhängig von Level und
  Position, und werden nie wiederverwendet — der Fortschritt speichert gelöste IDs, eine Frage
  darf also zwischen Leveln wandern, ohne dass Fortschritt verloren geht. `difficulty`
  (`EASY|MEDIUM|HARD`) ist vorbereitet, aber optional; "gemischt" wird nicht gespeichert, sondern
  ist ein Filter im Gruppenmodus.
- **Category** (= Thema): `id, name, order, levelCount`; `order` sortiert und bestimmt die Farbe.
- **CategoryProgress**: Solo-Fortschritt eines Users pro Thema — `completedLevels,
  solvedInCurrentLevel (IDs), updatedAt`. Levels werden strikt der Reihe nach gespielt, deshalb
  genügt das (ein kleines Dokument pro Thema statt eines pro Antwort; geschrieben wird nur bei
  richtigen Antworten). **Rank** ergibt sich aus `completedLevels` (0/1/3/6/10) statt aus einem
  Anteil am Thema, damit neue Level niemandem den Rang wieder wegnehmen.
- **Favorite**: gehört einem User, referenziert eine Question per ID — verändert nie die
  Question selbst.
- **Challenge** (entfallen): 1:1-Challenges sind durch den Gruppenmodus ersetzt; es gibt dafür
  weder Code noch Firestore-Regeln mehr.

Admin-Rechte für Content-Pflege (Questions/Categories, über die künftige Admin-Web-App) sind
noch nicht modelliert — kein globales Rollenkonzept mehr auf User. Lösung (z. B. feste Liste von
Admin-UIDs) folgt, wenn die Admin-Web-App ansteht; bis dahin läuft Content-Pflege über die
Firebase-Konsole/Admin-SDK.

## Fragen-Content

Die Solo-Fragen werden als JSON-Paket mit der App ausgeliefert: `app/src/main/assets/content/manifest.json`
(`contentVersion`, `languages` und Themen mit `id, name: {de, en}, order`) und pro Thema eine Datei
`content/categories/<categoryId>.json` (Format siehe `QuestionContentParser`). Beim ersten Zugriff
übernimmt `RoomQuestionRepository` das Paket in Room, wenn seine `contentVersion` höher ist als die
gespeicherte. Spielen kostet so keine Firestore-Reads und geht offline. Ein Paket wird immer
komplett in einer Transaktion ersetzt; ein kaputtes Paket lässt den gespeicherten Stand stehen.
Bei jeder inhaltlichen Änderung `contentVersion` erhöhen; ein Unit-Test prüft, dass das gebündelte
Paket gültig ist und jedes Level genau 20 Fragen hat. Jede Frage hat ihre Texte (`title, answers,
explanation`) unter `texts` pro Sprache; `correctAnswerIndex` gilt für alle Sprachen, die Antworten
stehen also überall in derselben Reihenfolge. Jede Sprache aus `languages` ist Pflicht.

Aktuell 12 Themen: `nature, animals, space, body, tech, history, world, kitchen, arts, sports` mit je
drei Levels, dazu `math` (ab Niveau 1. Klasse) und `english` mit je zwei Levels. Im Thema Englisch
fragt die deutsche Fassung nach englischen Vokabeln, die englische nach entsprechendem Grundwissen
(andere Fragen, gleiche Position der richtigen Antwort). Einstufung nach dem Konzept "Themen & Fragenkatalog": `difficultyScore` 0–8
aus vier Kriterien (Vorwissen, Bekanntheit, Denkschritte, Falschantworten), daraus `difficulty`
(0–2 EASY, 3–5 MEDIUM, 6–8 HARD); Level 1 = 14 leicht + 6 mittel, Level-Reihenfolge (`order`)
grob nach Punkten, Frage 1 und jede fünfte leicht, höchstens 5 Fragen pro `subtopic` und Level.
`difficultyScore` und `subtopic` stehen nur in den Dateien (Parser ignoriert sie). Die richtige
Antwort steht an zufälliger Stelle mit explizitem `correctAnswerIndex`.

Geplant: Remote-Updates über ein `contentVersion`-Dokument (Firestore oder Remote Config) plus
ein herunterladbares Paket (z. B. Firebase Storage) im selben Format — einzuhängen in
`RoomQuestionRepository.ensureContent()`. Da richtige Antworten im Paket lesbar sind, bleiben
die Fragen des Gruppenmodus serverseitig und kommen nie in dieses Paket.

## Solo-Fortschritt speichern

Offline-first: Room (`progress`-Tabelle, Zeilen pro `userId` + `categoryId`) ist die Quelle für die
UI, Firestore (`users/{uid}/progress/{categoryId}`) die Sicherung und Brücke zwischen Geräten.
`OfflineFirstProgressRepository` schreibt jede richtige Antwort sofort lokal (`pendingSync = true`)
und lädt die neueste Zeile direkt hoch. Beim Login bzw. App-Start (`AuthRepository.observeUserId()`)
holt es den Server-Stand (`Source.SERVER`, offline wird übersprungen), führt beide Seiten mit
`CategoryProgress.mergedWith()` zusammen (mehr geschaffte Level gewinnt, sonst Vereinigung der
gelösten Fragen) und lädt Ausstehendes hoch. Mehrere offline geschaffte Level gehen in einzelnen
Schritten hoch, weil die Regeln nur ein Level pro Schreibvorgang erlauben.

## Firebase & Firestore

Collections: `users, groups, inviteCodes, groupQuestions, questions, categories, favorites`; Mitgliedschaft als
Subcollection `groups/{groupId}/members/{uid}` (Query "alle Gruppen eines Users" über eine
Collection-Group-Query auf `members`, gefiltert nach `userId`); Solo-Fortschritt als
`users/{uid}/progress/{categoryId}`; Runden des Gruppenmodus als `groups/{groupId}/rounds/...`
(siehe "Gruppen & Gruppenmodus"). Solo-Fragen kommen nicht aus Firestore, sondern
aus dem gebündelten Paket (siehe "Fragen-Content"). Repository-Abstraktionen
pro fachlichem Bereich, z. B.
`QuestionRepository.observeCategories()/getLevelQuestions()/observeFavorites()/addFavorite()`,
`ProgressRepository.observeProgress()/markSolved()`,
`GroupRepository.observeGroups()/createGroup()/createInviteCode()/joinGroup(code)/removeMember()/observeMembers()`,
`AuthRepository.observeCurrentUser()/signIn()/signOut()`.

Security-Regeln sind Pflicht, nicht optional — Sicherheitslogik nie ausschließlich im Client:

- User lesen nur eigene private Daten; den eigenen Fortschritt nur um höchstens ein Level pro
  Schreibvorgang erhöhen (nicht fälschungssicher, deshalb vergleicht der Gruppenmodus nie den
  Solo-Fortschritt).
- Gruppen lesbar für Mitglieder; erstellen kann jede:r (wird automatisch Owner); Codes erzeugen,
  Mitglieder entfernen, umbenennen und löschen nur der Owner. Beitritt nur per Einladungscode.
- Gruppenmodus: richtige Antworten erst lesbar, wenn man die Runde fertig gespielt hat oder sie
  vorbei ist; Timer pro Frage wird in den Regeln geprüft.
- Questions öffentlich lesbar nur wenn `published == true`; Schreiben vorerst komplett
  gesperrt (siehe Admin-Hinweis oben).

Umgesetzt in `firestore.rules` (Repo-Root), referenziert über `firebase.json`. Das Firebase-Projekt
(`wii-21c9b`) existiert, ist lokal aber noch nicht mit der Firebase CLI verknüpft (keine
`.firebaserc`) — vor dem ersten Deploy einmalig `firebase use --add wii-21c9b`, dann:
`firebase deploy --only firestore` (Regeln + `firestore.indexes.json`). Die Regeln für
Gruppen, Codes und Runden sind gegen den Emulator getestet, aber noch nicht gegen echten
Schreibcode (das Group-Repository existiert noch nicht) — bei der Implementierung die Feldnamen
gegen die Rules abgleichen. Für `questions/categories/favorites` gilt dasselbe.

## Gruppen & Gruppenmodus

Datenmodell (Stand: entworfen, Rules + Emulator-Tests fertig, Kotlin-Code und Cloud Functions noch
nicht). Alle Zeitfelder hier sind Firestore-Timestamps, geschrieben als `serverTimestamp()`, damit
die Regeln sie gegen `request.time` prüfen können (`users`/`progress` bleiben bei Epoch-Millis).

```
groups/{groupId}                      name, createdBy, createdAt
  members/{uid}                       userId, role, displayName, joinedAt, inviteCode?
  rounds/{roundId}                    createdBy, createdAt, difficulty, questionCount,
                                      answerCounts[], secondsPerQuestion, endsAt     (nur Function)
    content/questions                 Texte + Antwortoptionen pro Sprache, ohne Lösung (nur Function)
    content/solution                  richtige Indizes + Erklärungen                 (nur Function)
    participants/{uid}                displayName, startedAt, answers: {"0": {choice, at}, ...}
inviteCodes/{code}                    groupId, groupName, createdBy, createdAt, expiresAt
groupQuestions/{questionId}           eigener Fragenpool mit Lösung, für Clients gesperrt
```

- **Gruppe anlegen**: ein Batch aus `groups/{id}` und der eigenen OWNER-Mitgliedschaft.
  **Löschen**: ein Batch, der alle `members` und die Gruppe löscht (der Owner darf seine eigene
  Mitgliedschaft nur so entfernen; austreten kann der Owner nicht, Übergabe der Ownership gibt es
  noch nicht). Runden bleiben dabei verwaist liegen, sind aber für niemanden mehr lesbar.
- **Einladen nur per Code**: der Owner legt `inviteCodes/{code}` an (8 Zeichen aus
  `A-HJ-NP-Z2-9`, höchstens 7 Tage gültig, nicht änderbar, nur löschbar). Wer den Code kennt, darf
  ihn lesen (Vorschau mit `groupName`) und trägt sich selbst mit `inviteCode` als MEMBER ein; die
  Regeln prüfen Gruppe und Ablauf. Kein Cloud-Function-Zwang, solange es kein Mitgliederlimit gibt.
- **Alle Gruppen eines Users**: Collection-Group-Query `members where userId == uid` (Index-Ausnahme
  in `firestore.indexes.json`).
- **Runde**: eine Callable Cloud Function `startRound` (Mitglied prüfen, Fragen nach Schwierigkeit
  `EASY|MEDIUM|HARD|MIXED` aus `groupQuestions` ziehen, Round + `content/*` schreiben) ist nötig, weil
  der Pool mit Lösungen nie zum Client darf. Jede:r spielt bis `endsAt` im eigenen Tempo:
  `participants/{uid}` anlegen startet den Timer und gibt `content/questions` frei. Antworten werden
  der Reihe nach angehängt (`answers.<n>`), eine echte Antwort muss innerhalb von
  `secondsPerQuestion` (+3 s Netz-Puffer) nach der vorigen kommen, `choice = -1` markiert Zeit
  abgelaufen. Wer fertig ist oder nach `endsAt` sieht `content/solution` und alle Teilnehmenden,
  das Ergebnis "wer lag richtig" rechnet der Client daraus aus. Für die Auswertung braucht es also
  keine Function. Grenze: Weil `content/questions` beim Start komplett lesbar ist, kann man spätere
  Fragen schon während früherer Timer lesen; strenger ginge es nur mit einer Function pro Frage.
- Cloud Functions brauchen den Blaze-Tarif von Firebase.

## Navigation

Navigation Compose mit typsicheren Routen: alle Ziele als `@Serializable`-Typen in
`core/navigation/Route.kt` (`sealed interface Route`), Argumente als typisierte Felder
(`Route.Level(categoryId, level)`, gelesen per `entry.toRoute<…>()`) — keine Route-Strings.
Braucht das Kotlin-Serialization-Plugin plus `kotlinx-serialization-core` (Navigation bringt die
Library nur intern mit). Flows: Auth (Login) → Main (Home, Lernen, Groups, später Favorites) plus Profil (`Route.Profile`) und Detail-Routen (Thema `Route.Category`, Level `Route.Level`). Tabs wechseln über `navigateToTab()` mit Home als Wurzel.

## UI & Theming

Eigenes `WissliTheme` mit zentralen Design Tokens (Farben, Typography, Shapes, Spacing) unter
`ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`). Bewusst ohne Dynamic Color, damit
die Markenfarben (warmes Papier `#FBF8F3`, Wissli-Blau `#2F74BB`) überall gleich aussehen.
Schriften gebündelt unter `res/font/` (beide SIL OFL, Lizenzen in `licenses/fonts/`):
Instrument Serif für große Überschriften (`display*`/`headline*`), Plus Jakarta Sans für alles
andere. Die Jakarta-Gewichte sind statische, auf Latein + Latin Extended reduzierte Instanzen der
Variable Font (fontTools `varLib.instancer` + `pyftsubset`). Layout-Abstände über `Spacing.*`
statt frei gewählter dp-Werte.
Anspruch: moderne, hochwertige Consumer-App-Optik — viel Weißraum, große verständliche
Cards, klare Typografie, dezente Animationen, große Touch-Targets — bewusst *kein*
klassischer "Kinder-Lern-App"-Look. Noch kein umfangreiches Design-System bauen.

## Testing

JUnit, `kotlinx-coroutines-test`, Turbine, Compose UI Tests. Fokus auf ViewModel-State,
User-Events, Repository-Verhalten, Fortschritts-/Level-State und kritische Navigation-Flows — lieber
wenige sinnvolle Tests als künstliche Coverage.

## Premium & KI (noch nicht implementiert)

Architektur muss dafür vorbereitet sein, aber nichts vorzeitig bauen:

- Premium-Status wird niemals ausschließlich clientseitig geprüft (später Play Billing +
  serverseitige Prüfung).
- KI-Aufrufe laufen ausschließlich über Firebase Cloud Functions
  (`Android → Cloud Function → AI API → Cloud Function → Android`) — niemals ein AI-API-Key
  in der Android-App.

## Admin Web App (separates zukünftiges Projekt)

Geplant als eigenständiges Projekt unter `admin/` (React, TypeScript, Vite, Firebase Web
SDK, Tailwind, optional shadcn/ui) zur Verwaltung von Questions/Categories — nicht Teil
des aktuellen Android-Scopes, nur bei der Repo-/Doku-Struktur mitdenken.

## Entwicklungsstrategie

Nicht die ganze App auf einmal bauen. Reihenfolge: Projekt/Gradle-Setup → Compose-Theme →
Navigation-Grundgerüst → Firebase-Setup → Models → Repository-Interfaces → Firebase-
Datenquellen → Auth → Solo-Fortschritt (Questions) → Favorites → Groups → Gruppenmodus → Push
Notifications →
Tests. **Nach jedem Schritt muss das Projekt weiterhin kompilieren.**

## Commands

Für den Google-Sign-In-Flow lokal testbar zu machen: `GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com`
in die (gitignorte) `local.properties` eintragen (Firebase-Konsole → Authentication →
Sign-in method → Google → Web-Client-ID). Ohne den Eintrag baut das Projekt trotzdem (Wert
fällt auf einen leeren String zurück), aber `LoginScreen` kann sich nicht anmelden.

Gradle Wrapper vom Repo-Root (`gradlew.bat` unter Windows, `./gradlew` in POSIX-Shells):

- Debug-APK bauen: `gradlew.bat assembleDebug`
- Unit-Tests (`app/src/test`): `gradlew.bat testDebugUnitTest`
- Einzelne Testklasse: `gradlew.bat testDebugUnitTest --tests "com.wissli.app.PfadZurKlasse"`
- Instrumented Tests (`app/src/androidTest`, benötigt Gerät/Emulator): `gradlew.bat connectedDebugAndroidTest`
- Auf verbundenes Gerät installieren: `gradlew.bat installDebug`
- Build-Outputs bereinigen: `gradlew.bat clean`
- Alles, was die CI prüft (ktlint, Detekt, Android Lint, Unit-Tests): `gradlew.bat check --continue`

CI (`.github/workflows/ci.yml`, JDK 21) führt bei Push/PR auf `main` genau `./gradlew check
--continue` aus — vor einem Commit lokal `check` laufen lassen, damit die CI nicht rot wird.

`firebase.json` enthält den Rules-Pfad und eine Emulator-Konfiguration für Firestore (Port 8080).
Rules-Tests gegen den Firestore-Emulator (Node 22 + Java 11+ nötig, läuft auch als eigener
CI-Job `firestore-rules`): `cd firestore-tests && npm ci && npm test` (startet den Emulator
selbst). Bei jeder Änderung an `firestore.rules` die Tests in `firestore-tests/rules.test.js`
mitziehen. Auth-/Functions-Emulator sind noch nicht eingerichtet.

### Statische Analyse: Detekt & ktlint

Zwei unabhängige, sich ergänzende Tools — Formatierung/Code-Style läuft bewusst ausschließlich
über ktlint, Detekt deckt Code Smells/Komplexität/potenzielle Bugs ab (kein
`detekt-rules-ktlint-wrapper`, um doppelte/widersprüchliche Meldungen zu vermeiden). Beide
Tasks hängen an `check`, laufen also automatisch bei `gradlew.bat check` mit.

- Detekt-Check: `gradlew.bat detekt` (Reports unter `app/build/reports/detekt/`)
- ktlint-Check: `gradlew.bat ktlintCheck`
- ktlint Auto-Format: `gradlew.bat ktlintFormat` (behebt die meisten Style-Verstöße automatisch;
  Ausnahmen wie Wildcard-Imports müssen manuell gefixt werden)
- Detekt-Konfiguration: `config/detekt/detekt.yml` (nur gezielte Abweichungen vom Default,
  z. B. `ForbiddenComment` deaktiviert wegen der TODO-basierten Entwicklungsstrategie oben,
  `FunctionNaming` ignoriert `@Composable`-Funktionen); `buildUponDefaultConfig = true` im
  `detekt {}`-Block in `app/build.gradle.kts` kombiniert das mit Detekts Standardregeln.
- `.editorconfig` (Repo-Root) setzt `ktlint_function_naming_ignore_when_annotated_with = Composable`
  für dieselbe Compose-Ausnahme auf ktlint-Seite.

**Wichtig für Kotlin-Versions-Bumps**: Detekt 1.23.x liefert ab Kotlin 2.3+ False Positives
(defekte Typauflösung). Deshalb wird bewusst die neue `dev.detekt`-Plugin-Linie ab 2.0.0-alpha.x
verwendet, die gegen aktuelle Kotlin-Versionen gebaut ist — noch Alpha-Status, vor jedem
Kotlin-Bump `gradlew.bat detekt` gegenprüfen und ggf. die Detekt-Version mit hochziehen.

### Gradle-Stolperstein: AGP 9 Built-in Kotlin vs. KSP

AGP 9.3 kompiliert Kotlin standardmäßig über eine eingebaute Kotlin-Unterstützung
(`android.builtInKotlin`, Default `true`) statt über ein separates `org.jetbrains.kotlin.android`-
Plugin. **KSP läuft nur mit diesem eingebauten Modus** — das explizite `kotlin("android")`-Plugin
zusätzlich anzuwenden (z. B. um `android.builtInKotlin=false` zu setzen) bricht mit AGP 9.3.2 den
Build (Cast-Fehler zwischen internen AGP-Extension-Klassen), weil die dafür nötige KGP-Kompatibilität
für AGP 9 erst mit neueren Kotlin-Releases kam. Deshalb: **kein** `kotlin-android`-Plugin anwenden,
`android.builtInKotlin` nicht setzen, und für KSP eine aktuelle, von der Kotlin-Version entkoppelte
KSP2-Version verwenden (aktuell `2.3.12`, s. `gradle/libs.versions.toml`) statt einer alten
`{kotlinVersion}-{kspVersion}`-gekoppelten Version. Vor jedem Kotlin-Versions-Bump `gradlew.bat
assembleDebug` laufen lassen, um diese Kombination erneut zu verifizieren.
