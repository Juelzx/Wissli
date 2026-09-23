# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Produkt

Wissli ist eine Android-App für Kinder und Familien: Kinder entdecken Wissensfragen
("Warum ist der Himmel blau?") und bekommen altersgerechte Erklärungen. Kernfeature:
Eltern favorisieren Fragen und schicken sie als **Challenge** an ein Kind; das Kind
beantwortet die Frage und erhält Feedback/Erklärung. Ziel ist eine veröffentlichbare
Play-Store-App, langfristig mit KI- und Premium-Funktionen.

## Scope

Aktuell **ausschließlich natives Android** (kein iOS, kein KMP/Compose Multiplatform –
der Entwickler hat keinen Mac). Die Architektur muss trotzdem so sauber getrennt sein
(Repository-Abstraktionen, keine UI-Kopplung an Datenquellen), dass eine spätere
Portierung nicht durch Android-spezifische Kurzschlüsse blockiert wird.

Repo-Realität: Alle Ziel-Libraries (Koin, Room, Navigation Compose, Firebase, Coroutines,
Turbine) sind im Version Catalog gepflegt und im `app`-Modul als Dependencies eingebunden.
Implementiert ist bisher der **Auth-Flow** (Google Sign-In) als Referenz-Feature für alle
folgenden Features: `WissliNavHost` → `AuthGateViewModel` (beobachtet
`AuthRepository.observeCurrentUser()` und entscheidet die Start-Destination — `Routes.LOGIN`
oder `Routes.HOME` — statt beim App-Start immer hart bei Login zu starten) → `LoginScreen` →
`LoginViewModel` → `AuthRepository`/`GoogleAuthDataSource`. `Routes.HOME` ist aktuell nur ein
Platzhalter-Screen (`HomePlaceholder` in `WissliNavHost.kt`), bis das echte Home-Feature ansteht.
`FirebaseAuthRepository` ist die einzige Stelle, die `FirebaseAuth`/`FirebaseFirestore` anfasst;
`CredentialManagerGoogleAuthDataSource` holt das Google-ID-Token über die Android Credential
Manager API und ist bewusst von `AuthRepository` getrennt (reine Plattform-API, kein
Firebase-Zugriff). Koin ist initialisiert (`WissliApplication` startet `startKoin { ... }`) mit
`di/AppModule.kt` (`appModule`), das Feature-Module per `includes()` einhängt — bisher nur
`di/AuthModule.kt` (`authModule`).
Noch **nicht implementiert**: alle Screens/Repositories außer Auth (echtes Home, Questions,
Favorites, Family, Challenges), Room-Entities/DAOs, Rollen-Onboarding nach dem Login (jede
Erstanmeldung bekommt aktuell hart `UserRole.PARENT`, siehe TODO in `FirebaseAuthRepository`).
Beim Lesen des Codes den tatsächlichen Stand prüfen statt dies als vollständig anzunehmen.

Konvention für neue Features (siehe `feature/auth/login/*` als Vorlage): pro Screen ein
eigenes Paket `feature/<bereich>/<screen>/` mit `<Screen>Contract.kt` (immutable State +
sealed `Intent`/`Effect`), `<Screen>ViewModel.kt` (nimmt Intents via `onIntent()` entgegen,
emittiert State über `StateFlow` und einmalige Effekte über einen `SharedFlow`) und
`<Screen>Screen.kt` (Composable, holt das ViewModel per `koinViewModel()`, sammelt Effects in
einem `LaunchedEffect`). Pro fachlichem Bereich ein eigenes `di/<Bereich>Module.kt`, das in
`appModule` inkludiert wird — kein monolithisches DI-Modul.

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
Room 2.x, obwohl noch keine Entities/DAOs existieren — bewusst früh gewechselt, um eine spätere
Migration zu vermeiden. Wichtige Breaking Changes ggü. Room 2.x, die beim ersten Anlegen von
Entities/DAOs/Database zu beachten sind:
- Nur noch KSP, kein KAPT/Java-Codegen (KSP ist bereits eingerichtet).
- DAO-Funktionen müssen `suspend` sein (außer beobachtbare Rückgabetypen wie `Flow`).
- `Room.databaseBuilder(...)` braucht zwingend `.setDriver(BundledSQLiteDriver())`
  (`androidx.sqlite:sqlite-bundled`) — bewusst der bundled statt der Android-Framework-Treiber,
  passend zum Portierbarkeits-Anspruch oben (siehe "Scope").
- Package-Imports sind `androidx.room3.*` statt `androidx.room.*`; `@TypeConverter` heißt jetzt
  `@ColumnTypeConverter`.

## Architektur

```
UI (Compose) → ViewModel → Repository → Data Sources (Firebase remote / Room local)
```

- MVI-orientiert: **immutable UI State** + **sealed Events/Intents** pro Screen, State-Fluss
  unidirektional über `StateFlow`. Für einfache Screens (wenige Intents, State-Übergänge ohne
  Überschneidungen — siehe `feature/auth/login/*`) reicht `_state.update { ... }` direkt inline
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

## Domain-Modelle (Kern)

- **User**: `id, displayName, role, familyId, createdAt`; `UserRole = PARENT | CHILD | ADMIN`.
  So wenig personenbezogene Daten wie möglich, besonders bei Kindern (kein voller Name,
  keine Adresse, kein Geburtsdatum).
- **Family**: `id, name, createdAt`; Mitgliedschaft läuft über `User.familyId`.
- **Question**: globaler, familienunabhängiger Content — `id, title, explanation,
  categoryId, ageFrom, ageTo, difficulty, answerType, imageUrl?, published, premium,
  createdAt, updatedAt`.
- **Favorite**: gehört einem User, referenziert eine Question per ID — verändert nie die
  Question selbst.
- **Challenge**: verbindet Question, Family, `createdBy` (Parent) und `assignedTo` (Child) —
  `id, questionId, familyId, createdBy, assignedTo, status, answer, result?, createdAt,
  completedAt?`; `status = PENDING | IN_PROGRESS | COMPLETED`.

## Firebase & Firestore

Collections: `users, families, questions, categories, challenges, favorites` (Subcollections
erlaubt, wenn fachlich sinnvoller). Repository-Abstraktionen pro fachlichem Bereich, z. B.
`QuestionRepository.observeQuestions()/getQuestion()/observeFavorites()/addFavorite()`,
`ChallengeRepository.observeChallenges()/createChallenge()/startChallenge()/completeChallenge()`,
`FamilyRepository`, `AuthRepository.observeCurrentUser()/signIn()/signOut()`.

Security-Regeln sind Pflicht, nicht optional — Sicherheitslogik nie ausschließlich im Client:

- User lesen nur eigene private Daten; Parent verwaltet nur die eigene Family; Child editiert
  nur eigene Challenges.
- Questions öffentlich lesbar nur wenn `published == true`; Erstellen/Editieren/Löschen nur
  durch Admins.
- Niemand darf die eigene Rolle selbst auf `ADMIN` setzen.

Umgesetzt in `firestore.rules` (Repo-Root), referenziert über `firebase.json`. Deploy erst
möglich, sobald ein Firebase-Projekt existiert und lokal verknüpft ist (`firebase use --add`
bzw. `.firebaserc`, noch nicht angelegt), dann: `firebase deploy --only firestore:rules`. Die
Regeln für `families/questions/categories/challenges/favorites` sind vorbereitet, aber noch
nicht gegen echten Schreibcode geprüft (diese Repositories existieren noch nicht) — bei der
jeweiligen Erstimplementierung die Feldnamen in den Rules gegen den tatsächlichen Code abgleichen.

## Navigation

Navigation Compose; Routen zentral definiert (in `core/navigation`), niemals verstreute
String-Literale in Screens. Flows: Auth (Login) → Main (Home, Questions, Favorites,
Challenges, Family, Profile) plus Detail-Routen (Question Detail, Challenge Detail).

## UI & Theming

Eigenes `WissliTheme` mit zentralen Design Tokens (Farben, Typography, Shapes, Spacing).
Anspruch: moderne, hochwertige Consumer-App-Optik — viel Weißraum, große verständliche
Cards, klare Typografie, dezente Animationen, große Touch-Targets — bewusst *kein*
klassischer "Kinder-Lern-App"-Look. Noch kein umfangreiches Design-System bauen.

## Testing

JUnit, `kotlinx-coroutines-test`, Turbine, Compose UI Tests. Fokus auf ViewModel-State,
User-Events, Repository-Verhalten, Challenge-State und kritische Navigation-Flows — lieber
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
Datenquellen → Auth → Questions → Favorites → Family → Challenges → Push Notifications →
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

`firebase.json` existiert bisher nur mit dem `firestore`-Key (Rules-Pfad, siehe "Firebase &
Firestore" oben) — noch keine Emulator-Konfiguration. Sobald die eingerichtet ist, hier die
Startbefehle für die Firebase Emulator Suite (Auth/Firestore/ggf. Functions) ergänzen.

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
