# Moje Roboty — aplikacja Android w euro (€)

Natywna aplikacja w języku polskim do planowania robót remontowych, usług i prac wielodniowych. Jedno zlecenie obejmuje dowolny zakres dat i ma **jedną cenę za całość w EUR**, niezależnie od liczby dni.

**Stan na 8 października 2026:** kod źródłowy aplikacji jest w repozytorium. Nie wykonano jeszcze potwierdzonej kompilacji APK i testów na urządzeniu w tej sesji. Aby nie sugerować gotowego produktu, postęp i ograniczenia są opisane w [statusie implementacji](docs/STATUS_IMPLEMENTACJI.md).

## Funkcje w kodzie

- Kalendarz miesiąca, tygodnia i dnia z wielodniowymi paskami oraz osobnymi torami dla nakładających się robót.
- Tworzenie, edycja, duplikowanie, anulowanie i logiczne usuwanie zleceń; ostrzeganie o kolizjach.
- Nazwa, zakres dat, cena całej pracy (€), adres, notatki, klient i status.
- Kartoteka klientów z numerem i adresem, archiwizacja i historia przypisanych robót.
- Zaliczki i wpłaty; automatyczne sumowanie otrzymanych kwot i salda.
- Panel finansowy: wartość umów rozpoczętych w miesiącu, wpłaty w miesiącu, zaległości.
- Lokalne przechowywanie w SQLite, praca bez internetu i konta.
- Eksport i import kopii JSON z walidacją i atomowym przywracaniem danych.
- Testy jednostkowe logiki dat i kwot, workflow GitHub Actions budujący debug APK.

## Kompilacja i instalacja

1. Otwórz repozytorium jako projekt w aktualnym Android Studio z JDK 17.
2. Zainstaluj Android SDK Platform **37** i Build Tools **36.0.0**.
3. Gradle **9.4.1**, Android Gradle Plugin **9.2.0**, Compose BOM **2026.09.00**.
4. Ponieważ binarna paczka Gradle Wrapper nie znajduje się w repozytorium, użyj zainstalowanego Gradle albo wygeneruj wrapper poleceniem `gradle wrapper --gradle-version 9.4.1`.
5. Uruchom `gradle :app:testDebugUnitTest :app:assembleDebug`.
6. APK debug (gdy kompilacja się powiedzie): `app/build/outputs/apk/debug/app-debug.apk`.

Po pomyślnym przebiegu workflow **Android APK and tests** w zakładce GitHub Actions dostępny będzie artefakt **MojeRoboty-debug-apk**. Artefakt pojawi się wyłącznie przy udanym buildzie; nie jest tu załączony gotowy, sprawdzony APK.

## Gdzie zacząć w kodzie

- `app/src/main/java/pl/mojeroboty/app/MainActivity.kt` — nawigacja, formularze, klienci, finanse i kopie.
- `app/src/main/java/pl/mojeroboty/app/CalendarScreen.kt` — kalendarz z paskami.
- `app/src/main/java/pl/mojeroboty/app/Model.kt` — daty, euro, segmentacja torów.
- `app/src/main/java/pl/mojeroboty/app/Store.kt` — SQLite i import/eksport JSON.
- `app/src/test/` — testy jednostkowe.
- `.github/workflows/android.yml` — automatyczna kompilacja.

## Dokumentacja projektu

1. [Specyfikacja funkcji](docs/SPECYFIKACJA.md)
2. [Architektura i algorytm kalendarza](docs/ARCHITEKTURA.md)
3. [Ekrany i UX](docs/UX_UI.md)
4. [Scenariusze użytkownika](docs/SCENARIUSZE.md)
5. [Plan wdrożenia i lista zadań](docs/PLAN_REALIZACJI.md)
6. [Testy i warunki odbioru](docs/TESTY.md)
7. [Kontrakt implementacyjny](docs/KONTRAKT_IMPLEMENTACYJNY.md)
8. [Status bieżącego kodu i ograniczenia](docs/STATUS_IMPLEMENTACJI.md)

Wszędzie domyślne rozliczenia to wyłącznie **EUR (€)**. Wartości przykładowe w poprzednich makietach służą tylko pokazaniu wyglądu, nie są wprowadzonymi danymi.
