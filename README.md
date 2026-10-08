# Moje Roboty — kalendarz zleceń na Android

Repozytorium projektu aplikacji do planowania robót jednodniowych i wielodniowych, klientów oraz rozliczeń. **Stan obecny: dokumentacja projektowa; kod aplikacji i APK nie zostały jeszcze utworzone.**

## Dokumentacja — zacznij tutaj

1. [Pełna specyfikacja produktu](docs/SPECYFIKACJA.md) — funkcje, ekrany, logika biznesowa, podział MVP/V1.
2. [Architektura i algorytm kalendarza](docs/ARCHITEKTURA.md) — model bazy, paski wielodniowe, finanse i komponenty.
3. [Scenariusze użytkownika](docs/SCENARIUSZE.md) — 20 przykładów od dodania pracy po backup.
4. [Plan realizacji krok po kroku](docs/PLAN_REALIZACJI.md) — fazy 0–6 i checklisty programistyczne.
5. [Testy i kryteria odbioru](docs/TESTY.md) — kontrola poprawności, offline, walidacja, pieniądze i UI.

## Pierwszy krok implementacji

Rozpocznij od Fazy 0 z [planu realizacji](docs/PLAN_REALIZACJI.md): utwórz projekt Kotlin + Jetpack Compose w Android Studio, uruchom debug APK na emulatorze, dodaj Room, a następnie zbuduj bazę i formularz pracy. Przed implementacją sprawdź bieżące zgodne wersje bibliotek w oficjalnych dokumentach Android Developers.

## Filary aplikacji

- Kalendarz miesięczny i tygodniowy pokazujący roboty jako ciągłe paski przez wiele dni.
- Wiele nakładających się zleceń, szybkie dodawanie i zmiana terminów.
- Kwota za **całą** robotę, zaliczki, wpłaty i pozostała należność.
- Klienci, wyszukiwanie, raporty, kopie zapasowe i praca offline.

Dokumentacja jest podstawą implementacji i testów; funkcji niezrealizowanych w kodzie nie należy oznaczać jako gotowe.
