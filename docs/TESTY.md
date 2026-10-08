# TESTY I KRYTERIA ODBIORU
Powiązanie: [specyfikacja](SPECYFIKACJA.md), [plan](PLAN_REALIZACJI.md).

## Testy jednostkowe / domenowe
- [ ] Zlecenie 10–12 X przypada na wszystkie trzy dni i nie przypada na 9/13 X.
- [ ] Zlecenie 30 XII–3 I widoczne w obu miesiącach i obu latach.
- [ ] Przecinanie przedziałów włącznie: wspólny tylko dzień graniczny = kolizja.
- [ ] Zlecenie jednodniowe zajmuje jedną komórkę, nie zero.
- [ ] Segment dłuższy niż 7 dni jest łamany na granicy tygodni; zachowuje tożsamość zlecenia.
- [ ] W tym samym tygodniu pasy o przecinających się dniach nie mają tego samego toru.
- [ ] >3 roboty na dzień: miesięczny `+N więcej` jest zgodny z rzeczywistą liczbą ukrytych pozycji.
- [ ] Rok przestępny: 29 lutego poprawny.
- [ ] Przejście czasu letni/zimowy nie przesuwa całodniowych dat.
- [ ] `endDate < startDate` odrzucane, tytuł whitespace odrzucany.
- [ ] Wartości pieniężne: 0, 0.01, duże wartości, zaokrąglanie i przecinek polski.
- [ ] 260000 groszy - 50000 - 210000 = 0; status DONE nie zmienia salda.
- [ ] PLN i EUR nigdy nie są sumowane ze sobą.
- [ ] Kwota umówiona całego zlecenia zliczana tylko raz, wg daty startu.
- [ ] Płatność przypisana do daty zapłaty, nie początku zlecenia.
- [ ] CANCELLED wykluczone z aktywnej kwoty planu, zachowane w historii.
- [ ] FK: klient archiwizowany bez utraty robót; usunięcie klienta nie niszczy zleceń.
- [ ] Wyszukiwanie frazy bez rozróżnienia wielkości liter i odpowiednio polskich znaków.

## Testy integracyjne
- [ ] Create/edit/delete/undo i restart aplikacji.
- [ ] Aktualizacja jednej roboty zmienia jednocześnie listę, kalendarz i finanse.
- [ ] 1000 zleceń w Room, 10 000 płatności — bez ANR (pomiar na urządzeniu).
- [ ] Eksport/import roundtrip: liczba i wszystkie pola, referencje, grosze, waluty, UUID.
- [ ] Uszkodzony/importowany częściowo plik -> brak częściowego zapisu (rollback).
- [ ] Nieobsługiwana przyszła schemaVersion -> czytelny błąd bez utraty obecnych danych.
- [ ] Migracje schematu Room działają bez destrukcyjnego fallbacku.
- [ ] Offline / tryb samolotowy: pełne tworzenie i edycja.

## Testy UI / ręczne
- [ ] Scenariusze S01–S20 z SPECYFIKACJA.md.
- [ ] Ekran 360dp szerokości, duża czcionka 200%, kontrast i TalkBack.
- [ ] Jasny/ciemny motyw; statusy nie tylko kolorami.
- [ ] Odmowa uprawnienia do powiadomień nie blokuje aplikacji (V1).
- [ ] Obrót urządzenia i odtworzenie procesu z częściowo wypełnionym formularzem.
- [ ] Brak klientów, brak robót, brak wyników wyszukiwania, brak wpłat.
- [ ] Otwórz zlecenie przez pasek w miesiącu, tygodniu i karcie dnia.
- [ ] Klik w `+N więcej` pokazuje dokładnie wszystkie roboty wybranego dnia.
- [ ] Nakładające się zlecenia nie zasłaniają się wzajemnie.
- [ ] Koniec miesiąca i początek roku: kontynuacje pasków są spójne.
- [ ] Wyjście z edycji z niezapisanymi zmianami pyta o potwierdzenie.
- [ ] Nie ma nieuzasadnionych zgód runtime ani przesyłania danych klientów do sieci.

## Definition of Done całej aplikacji MVP
- [ ] Debug APK instaluje się na fizycznym telefonie; aplikacja działa offline.
- [ ] Kalendarz miesiąc/tydzień/dzień jest czytelny i stabilny.
- [ ] Wielodniowe zlecenia, konflikty, płatności i raporty działają zgodnie ze specyfikacją.
- [ ] Eksport/import danych przetestowany na osobnym urządzeniu/emulatorze.
- [ ] Testy jednostkowe i integracyjne są zielone w CI, a testy ręczne odnotowane.
- [ ] Nie ma otwartych błędów krytycznych utraty danych lub niewłaściwych kwot.
- [ ] Wersja build, changelog, licencja, dokumentacja instalacji i prywatności gotowe.
