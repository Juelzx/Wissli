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
folgenden Features: `WissliNavHost` (aktuell nur `Routes.LOGIN`) → `LoginScreen` →
`LoginViewModel` → `AuthRepository`/`GoogleAuthDataSource`. `FirebaseAuthRepository` ist die
einzige Stelle, die `FirebaseAuth`/`FirebaseFirestore` anfasst; `CredentialManagerGoogleAuthDataSource`
holt das Google-ID-Token über die Android Credential Manager API und ist bewusst von
`AuthRepository` getrennt (reine Plattform-API, kein Firebase-Zugriff). Koin ist initialisiert
(`WissliApplication` startet `startKoin { ... }`) mit `di/AppModule.kt` (`appModule`), das
Feature-Module per `includes()` einhängt — bisher nur `di/AuthModule.kt` (`authModule`).
Noch **nicht implementiert**: alle Screens/Repositories außer Auth (Home, Questions,
Favorites, Family, Challenges), Room-Entities/DAOs, Main-Graph/Rollen-Onboarding nach dem
Login. Beim Lesen des Codes den tatsächlichen Stand prüfen statt dies als vollständig
anzunehmen.

Konvention für neue Features (siehe `feature/auth/login/*` als Vorlage): pro Screen ein
eigenes Paket `feature/<bereich>/<screen>/` mit `<Screen>Contract.kt` (immutable State +
sealed `Intent`/`Effect`), `<Screen>ViewModel.kt` (nimmt Intents via `onIntent()` entgegen,
emittiert State über `StateFlow` und einmalige Effekte über einen `SharedFlow`) und
`<Screen>Screen.kt` (Composable, holt das ViewModel per `koinViewModel()`, sammelt Effects in
einem `LaunchedEffect`). Pro fachlichem Bereich ein eigenes `di/<Bereich>Module.kt`, das in
`appModule` inkludiert wird — kein monolithisches DI-Modul.

Firebase-Dependencies sind eingebunden, aber die Plugins `google-services` und
`firebase-crashlytics` sind in `app/build.gradle.kts` nur auskommentiert hinterlegt, da sie
eine echte `app/google-services.json` (Firebase-Projekt) voraussetzen — ohne die Datei
bricht der Build beim Anwenden des Plugins sofort ab. Erst nach Anlegen des Firebase-Projekts
die Kommentare entfernen und die Datei ablegen.

## Tech Stack

Kotlin (aktuelle stabile Version) · Jetpack Compose + Material 3 · Coroutines/Flow/StateFlow ·
Navigation Compose · Koin (DI) · Room (lokale Persistenz) · Firebase: Auth, Firestore, Cloud
Messaging, Storage, Crashlytics, Analytics, App Check, Emulator Suite für lokale Entwicklung ·
Gradle Kotlin DSL mit Version Catalog (`gradle/libs.versions.toml`).

Keine unnötigen Zusatzbibliotheken — Jetpack-/Google-Lösungen bevorzugen. Ausnahme ist die
bewusste Entscheidung für Koin statt Hilt als DI-Framework (kein Codegen/KSP nötig,
einfachere Modul-Definition per Kotlin-DSL). SQLDelight nur statt Room, wenn dafür ein
konkreter, im PR/Commit begründeter Grund vorliegt.

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

Sobald `firebase.json` samt Emulator-Konfiguration existiert, hier die Startbefehle für die
Firebase Emulator Suite (Auth/Firestore/ggf. Functions) ergänzen — aktuell noch nicht
eingerichtet. Kein separat konfigurierter Linter vorhanden; `gradlew.bat lint` nutzt die
Android-Lint-Standardregeln.

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
