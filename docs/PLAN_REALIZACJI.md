# PLAN REALIZACJI — zadania deweloperskie i Definition of Done
Dokument bazowy: [SPECYFIKACJA](SPECYFIKACJA.md). Implementować iteracyjnie: każda faza buduje się i przechodzi testy.

## Faza 0 — projekt i fundament
- [ ] Utwórz projekt Android Kotlin, Gradle Kotlin DSL, wersje zależności przez version catalog.
- [ ] Ustal namespace np. `pl.mojeroboty.app` (przed publikacją zweryfikuj unikalność), minSdk 26, compile/target SDK 36.
- [ ] Konfiguruj Jetpack Compose + Material 3, Navigation Compose, ViewModel, Lifecycle, Kotlin coroutines/Flow, Hilt (lub prosty DI), Room (wersja docelowa) lub SQLiteOpenHelper (aktualny fundament), DataStore.
- [ ] Sprawdź zgodność wersji Gradle/AGP/Kotlin/KSP na oficjalnych stronach; nie zamrażaj tu arbitralnych numerów.
- [ ] Ustaw formatowanie, lint, statyczną analizę, unit/instrumented tests, GitHub Actions z testami na PR.
- [ ] Zaprojektuj paletę, typografię, light/dark, dostępność, ekrany puste/błędów/loading.
**DoD:** czysty build debug, uruchomienie na emulatorze, przejście między 4 zakładkami bez błędów.

## Faza 1 — model i repozytoria
- [ ] Encje Job, Client, Payment oraz DAO, FK, konwertery LocalDate/Instant, migracje Room i indeksy.
- [ ] Operacje CRUD transakcyjne; obserwacja Flow.
- [ ] Zapytanie zleceń przecinających zakres: start <= rangeEnd AND end >= rangeStart (bez deleted).
- [ ] Repository + use cases: SaveJob, JobsForRange, JobsForDay, AddPayment, GetFinancialSummary.
- [ ] Walidacje, kalkulator salda w Long minor units, niezależność status/płatność.
- [ ] Testy DAO (przecinanie dat, indeksy, referencje, usunięcia, migracje).
**DoD:** testy bazy zielone, 2 nakładające się roboty zapisują się i poprawnie wracają w zapytaniach.

## Faza 2 — formularz i detale
- [ ] Formularz z pickerem zakresu dat i przyciskiem zapisu.
- [ ] Daty inclusive; edycja istniejących rekordów, ochrona przed utratą edycji.
- [ ] Formularz klienta i wybór klienta, łatwe szybkie tworzenie.
- [ ] Szczegóły roboty + akcje statusu, duplikuj, usuń z potwierdzeniem.
- [ ] Kontrola konfliktów terminów (nie blokuje).
**DoD:** wykonaj S01, S02, S04, S11, S18.

## Faza 3 — kalendarz [najważniejsza]
- [ ] Własny layout siatki miesiąca Pon–Nd.
- [ ] Segmenty wielodniowych pasów per tydzień, stabilny przydział torów, `+N więcej`.
- [ ] Przewijanie miesięcy i tygodni, Today, wybór dnia, responsywne skalowanie.
- [ ] Widok tygodnia z nieograniczoną pionową listą pasów.
- [ ] Widok dnia i przejścia do szczegółów; tapnięcie dnia dodaje zlecenie na wybrany dzień.
- [ ] Kolory etykiet oddzielne od statusu; treść nie znika przy dużej liczbie robót.
- [ ] Testy algorytmu pasów i golden/screenshot tests.
**DoD:** S01–S04, S09, S10 i S20 w pełni działają na małym ekranie.

## Faza 4 — finanse, listy, klienci
- [ ] Płatności częściowe, edycja i usuwanie wpłat.
- [ ] Raporty miesiąc/zakres z semantyką daty startu i daty płatności.
- [ ] Rozliczenia wyłącznie w EUR.
- [ ] Wyszukiwanie, filtrowanie, sortowanie robót; archiwizacja klientów.
**DoD:** S05–S08, S12, S15–S17.

## Faza 5 — bezpieczeństwo danych / release MVP
- [ ] Eksport JSON z `schemaVersion`, UUID, datami i pieniędzmi w minor units.
- [ ] Import przez SAF z walidacją, preview i transakcyjnym rollback przy błędzie.
- [ ] Odtworzenie stanu ekranu, obsługa orientacji/dużej czcionki, ciemny motyw.
- [ ] Testy offline, import/export roundtrip, uszkodzony JSON, duplikaty, migracje.
- [ ] Polityka prywatności / bezpieczne logi / release signing przechowywany poza repo.
- [ ] Build debug APK i release AAB (jeśli publikacja); smoke test na realnym urządzeniu.
**DoD:** czysty rebuild i komplet testów w TESTY.md, brak crashy w kluczowych ścieżkach.

## Faza 6 — V1 (po MVP)
- [ ] Powiadomienia; od Android 13 poproś o POST_NOTIFICATIONS we właściwym momencie.
- [ ] Konfigurowalne powiadomienia przed pracą, WorkManager i wznowienie po restarcie.
- [ ] Przesuwanie/przeciąganie zlecenia po kalendarzu z potwierdzeniem.
- [ ] Święta/dni wolne, załączniki przez picker, szybkość obsługi.
- [ ] Testy regresji, wydajności, dostępności.

## Sugerowana struktura katalogów (przy implementacji)
```text
app/src/main/java/pl/mojeroboty/app/
  MainActivity.kt
  navigation/
  core/ui/ core/model/ core/money/ core/time/
  data/local/entity/ data/local/dao/ data/local/migration/
  data/repository/ data/settings/ data/backup/
  domain/usecase/
  feature/calendar/ feature/jobs/ feature/clients/
  feature/finance/ feature/settings/
app/src/test/
app/src/androidTest/
docs/SPECYFIKACJA.md
docs/PLAN_REALIZACJI.md
docs/TESTY.md
```

## Reguły wykonania
1. Nie deklaruj „gotowe”, jeśli brak testów akceptacyjnych danej fazy.
2. Nie przechowuj pieniędzy w Double i nie sumuj dni wielodniowych zleceń jako oddzielnych umów.
3. Jedna lokalna baza jest źródłem prawdy; UI tylko odczytuje stan z repozytorium.
4. Nie dodawaj uprawnień Android bez uzasadnionej funkcji.
5. Aktualizuj changelog, zadania i dokumentację przy zmianie zachowania.
