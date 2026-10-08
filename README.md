# Moje Roboty — aplikacja Android w euro (€) i synchronizacja ekipy

Natywna aplikacja w języku polskim do planowania robót remontowych, usług i prac wielodniowych. Jedno zlecenie obejmuje dowolny zakres dat i ma **jedną cenę za całość w EUR**, niezależnie od liczby dni.

**Stan wersji 1.1:** Kod synchronizacji Firebase wymaga własnego projektu w Firebase i konfiguracji `FIREBASE_*` podczas budowania APK. Bez tych danych wersja działa tylko lokalnie. Szczegóły: [Wspólna ekipa i konfiguracja](docs/SYNCHRONIZACJA_FIREBASE.md).

**Stan na 8 października 2026:** APK debug został zbudowany, a testy jednostkowe zaliczone w [GitHub Actions](https://github.com/mxxx3/Kalendarz/actions/runs/37772445338). Test instalacji na fizycznym Androidzie i testy UI pozostają do wykonania. Szczegóły w [statusie implementacji](docs/STATUS_IMPLEMENTACJI.md).

## Funkcje w kodzie

- Kalendarz miesiąca, tygodnia i dnia z wielodniowymi paskami oraz osobnymi torami dla nakładających się robót.
- Tworzenie, edycja, duplikowanie, anulowanie i logiczne usuwanie zleceń; ostrzeganie o kolizjach.
- Nazwa, zakres dat, cena całej pracy (€), adres, notatki, klient i status.
- Kartoteka klientów z numerem i adresem, archiwizacja i historia przypisanych robót.
- Zaliczki i wpłaty; automatyczne sumowanie otrzymanych kwot i salda.
- Panel finansowy: wartość umów rozpoczętych w miesiącu, wpłaty w miesiącu, zaległości.
- Lokalne przechowywanie w SQLite, praca bez internetu i konta.
- Opcjonalny tryb współdzielony: osobne konta Firebase Auth, przestrzenie ekip, nadawanie uprawnień przez UID, wspólne dane Firestore z aktualizacjami na bieżąco i buforowaniem zmian offline.
- Eksport i import kopii JSON z walidacją i atomowym przywracaniem danych.
- Testy jednostkowe logiki dat i kwot, workflow GitHub Actions budujący debug APK.

## Kompilacja i instalacja

1. Otwórz repozytorium jako projekt w aktualnym Android Studio z JDK 17.
2. Zainstaluj Android SDK Platform **37** i Build Tools **36.0.0**.
3. Gradle **9.4.1**, Android Gradle Plugin **9.2.0**, Compose BOM **2026.09.00**.
   Aby uruchomić synchronizację, skonfiguruj projekt Firebase zgodnie z [instrukcją](docs/SYNCHRONIZACJA_FIREBASE.md).
4. Ponieważ binarna paczka Gradle Wrapper nie znajduje się w repozytorium, użyj zainstalowanego Gradle albo wygeneruj wrapper poleceniem `gradle wrapper --gradle-version 9.4.1`.
5. Uruchom `gradle :app:testDebugUnitTest :app:assembleDebug`.
6. APK debug (gdy kompilacja się powiedzie): `app/build/outputs/apk/debug/app-debug.apk`.

Potwierdzony sukces: [GitHub Actions 37772445338](https://github.com/mxxx3/Kalendarz/actions/runs/37772445338) → artefakt **MojeRoboty-debug-apk** zawierający `app-debug.apk`. Jest to wersja debug do samodzielnej instalacji, nie podpisane wydanie sklepowe. Przetestowany kod jest scalony z gałęzią `main`.

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
9. [Synchronizacja pracowników przez Firebase](docs/SYNCHRONIZACJA_FIREBASE.md)

Wszędzie domyślne rozliczenia to wyłącznie **EUR (€)**. Wartości przykładowe w poprzednich makietach służą tylko pokazaniu wyglądu, nie są wprowadzonymi danymi.
