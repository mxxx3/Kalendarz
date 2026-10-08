> **Konfiguracja Android Firebase:** plik klienta projektu `kalendarz-8f0cb` został dostarczony i użyty do ustawienia identyfikatorów klienta w `firebase-project.properties`. Nie dowodzi to włączenia Firebase Authentication, istnienia Firestore ani publikacji reguł. Synchronizację dwóch urządzeń należy przetestować po wykonaniu [instrukcji konfiguracji](SYNCHRONIZACJA_FIREBASE.md).

> **Aktualizacja 1.1:** Dodano opcjonalny kod współdzielonej ekipy (Firebase Auth, Firestore, członkostwo UID, zapisy i odczyt realtime, import lokalnych danych do nowej ekipy). Działa wyłącznie po zewnętrznej konfiguracji Firebase i publikacji [reguł](../firestore.rules). CI potwierdza jedynie kompilację/testy jednostkowe; **nie przeprowadzono jeszcze testu synchronizacji między dwoma prawdziwymi kontami w skonfigurowanym Firebase**. Konfiguracja i wymagane testy: [SYNCHRONIZACJA_FIREBASE.md](SYNCHRONIZACJA_FIREBASE.md).

# Stan implementacji Android (2026-10-08)

Ten plik oddziela **działający kod źródłowy / zaimplementowane ścieżki** od zaplanowanych funkcji i od **potwierdzonych testami** rezultatów. Samo dodanie kodu do repozytorium nie potwierdza, że APK zostało zbudowane.

## Zaimplementowane w kodzie

- Projekt Android Kotlin/Compose Material 3, minimalny SDK 26, target SDK 36, compile SDK 37.
- Ekran główny to kalendarz: miesiąc/tydzień/dzień, widoczne zakresy wielodniowe i osobne tory dla przecinających się robót.
- Dodaj/edytuj/duplikuj/status/anuluj/logicznie usuń robotę; waliduj zakres i potwierdzaj kolizje.
- Waluta **EUR (€)**, zapisana jako `Long` w centach, format polski, bez niejawnego przewalutowania.
- Lokalna baza SQLite (klienci, roboty, płatności), walidacje i zachowanie wpłat podczas edycji.
- Dodawanie/edycja/archiwizowanie klientów, przypisywanie do zleceń.
- Dodawanie i usuwanie wpłat, sumy i salda; zakończenie nie oznacza zapłacenia.
- Raport wartości prac i faktycznych wpłat w wybranym miesiącu.
- Eksport JSON i import z transakcją, walidacją formatu, relacji i wersji schematu. Import wymaga potwierdzenia zastąpienia danych.
- Workflow GitHub Actions do kompilacji APK i uruchamiania testów jednostkowych.
- Testy logiki EUR, dat granicznych i rozmieszczania pasków.

## Status weryfikacji

- [x] Sukces `assembleDebug` w GitHub Actions dla wersji współdzielonej 1.1, 8.10.2026 (run 37776897979).
- [x] Zielony przebieg testów jednostkowych w GitHub Actions dla 1.1.
- [ ] Testy dwóch kont na rzeczywistym projekcie Firebase + weryfikacja reguł Firestore w emulatorze.
- [ ] Instalacja APK i test manualny na fizycznym Androidzie.
- [ ] Testy UI i import/eksport na dwóch instalacjach.
- [ ] Wydajność z 1000 robót i 10 000 wpłat, TalkBack i czcionka 200%.
- [ ] Przegląd działania przy obrocie ekranu i przerywaniu procesu.

## Różnice względem pełnej specyfikacji (do dalszej pracy)

- **Baza:** aktualnie `SQLiteOpenHelper`, a nie planowane `Room`. Wersja produkcyjna powinna mieć przetestowaną migrację bez utraty danych; nie zastępuj bazy po prostu destrukcyjną migracją.
- **Kalendarz miesiąca:** limit widocznych 2 pasów i `+N więcej` (pełne zlecenia na liście wybranego dnia). Nie wdrożono przeciągania pasków.
- **Kwoty:** tylko EUR (jest to zamierzona zmiana względem wczesnych planów z PLN).
- **Klienci:** brak dedykowanego osobnego ekranu historii klienta i pełnych filtrów; podstawowe powiązania i podsumowanie są.
- **Wpłaty:** dodawanie i usuwanie, bez osobnego formularza edycji istniejącej wpłaty.
- **Backup:** import ma potwierdzenie, ale bez szczegółowego podglądu liczby rekordów przed zatwierdzeniem.
- **Widoki:** wymagają dalszych testów responsywności; tryb ciemny, przypomnienia, zdjęcia, święta i synchronizacja nie są gotowe.
- **Wersja release:** brak podpisanej produkcyjnej paczki AAB i polityki publikacji sklepowej.
- **Wersja 1.1:** synchronizacja ma szkielet i UI oraz weryfikację kompilacji w CI, ale nie działa bez backendu i kont; brak testów E2E Cloud Firestore, rozwiązywania konfliktów edycji, audytu zmian i chmurowego eksportu/backup.

## Instrukcja weryfikacji

1. [Potwierdzony udany build i testy jednostkowe](https://github.com/mxxx3/Kalendarz/actions/runs/37772445338); artefakt `MojeRoboty-debug-apk` zawiera `app-debug.apk`.
2. Pobierz `MojeRoboty-debug-apk` z udanego przebiegu GitHub Actions lub uruchom ponownie workflow dla bieżącej gałęzi.
3. Zainstaluj APK debug na Androidzie, dodaj robotę 12–14 października za 1200 EUR oraz drugą 13–16 października za 2600 EUR; zatwierdź kolizję.
4. Sprawdź listę i kalendarz, klienta, zaliczkę 500 EUR, saldo 2100 EUR i eksport/import.
5. Dopiero po zaliczeniu wszystkich kryteriów z [TESTY.md](TESTY.md) oznacz pierwszą wersję jako gotową.

## Ważna zasada bezpieczeństwa danych

Wewnątrz repozytorium nie wolno przechowywać danych prawdziwych klientów, kopii bazy, kluczy podpisu APK ani sekretów. Eksport JSON użytkownik zapisuje wyłącznie do świadomie wybranej lokalizacji.

## Uwaga przy aktualizowaniu wersji testowej

GitHub Actions tworzy domyślny **debug APK** z kluczem podpisu generowanym na runnerze. Kolejne buildy mogą mieć inny certyfikat niż już zainstalowany APK. Android może wtedy odmówić instalacji aktualizacji bez odinstalowania starszej wersji. **Odinstalowanie kasuje lokalną bazę SQLite.** Przed zmianą wydania wykonaj w starej aplikacji Ustawienia → Eksport kopii JSON i zapisz plik poza telefonem, a po instalacji nowej aplikacji zaimportuj kopię. Docelowa dystrybucja wymaga jednego stałego, bezpiecznie przechowywanego klucza podpisu (poza repozytorium). Nie zalecaj odinstalowania bez kopii.
