# KONTRAKT IMPLEMENTACYJNY — Moje Roboty
Wersja 1.0 • 2026-10-08

Ten dokument doprecyzowuje [specyfikację](SPECYFIKACJA.md), [UX/UI](UX_UI.md), [architekturę](ARCHITEKTURA.md), [scenariusze](SCENARIUSZE.md), [plan](PLAN_REALIZACJI.md) i [testy](TESTY.md). Nie zastępuje ich. **Stan repozytorium na dzień sporządzenia: dokumentacja, nie gotowa aplikacja.** Kod wolno oznaczyć jako ukończony dopiero po przejściu testów.

## 1. Priorytet produktu i zasady niepodlegające zmianie bez decyzji użytkownika
1. Po otwarciu programu widać kalendarz; bez ekranu rejestracji i bez obowiązkowego internetu.
2. Zlecenie trwające od poniedziałku do piątku jest **jedną robotą**, a nie pięcioma rekordami. W kalendarzu może mieć wiele fragmentów graficznych, ale jeden identyfikator i jedną cenę.
3. Wszystkie daty początku i końca są **włączne**. Wyświetlanie i wyszukiwanie po datach nie stosuje konwersji przez UTC.
4. Cena podawana w formularzu to cena całej roboty; nie mnożyć jej przez dni.
5. Nakładanie terminów jest dozwolone po wyświetleniu ostrzeżenia z nazwami kolidujących robót.
6. „Zrobione” nie znaczy „zapłacone”. Każda wpłata jest osobnym wpisem.
7. Wszystkie kwoty zapisuj w euro (EUR) i obliczaj w centach.

## 2. Minimalny, szybki przebieg
Po kliknięciu **+ Dodaj robotę**:
- fokus na nazwie („Remont łazienki”);
- data od i do ustawione na wybrany dzień;
- pole „Kwota za całą robotę” i waluta (EUR, bez wyboru innej waluty);
- przycisk **Zapisz** zawsze dostępny po spełnieniu walidacji;
- klient, adres, godzina, notatka, kolor i status pod „Więcej szczegółów”.

**Decyzja o nieustalonej wycenie:** pole ceny w MVP jest obowiązkowe; wartość 0 jest dozwolona i w interfejsie znaczy „Wycena do ustalenia”, a nie uzyskane 0 zł. Po zapisie pokazuje się natychmiast odpowiadający pasek. Powrót do formularza bez utraty wpisanych danych przy obrocie ekranu. Przy niezapisanych zmianach Back wymaga potwierdzenia.

## 3. Zachowanie kalendarza
**Miesiąc:** 7 kolumn (poniedziałek–niedziela), tygodnie jako osobne rzędy, każdy dzień ma numer i możliwość wyboru. Roboty rysować nad wspólną siatką, nie jako niezależne karty powtórzone w komórkach. Pasek przecinający granicę tygodnia dzielić na fragmenty wizualne. Początek i koniec poza widocznym tygodniem oznaczać jako kontynuację. Gdy miejsca jest za mało, pokaż `+N więcej` liczone dla **danego dnia**, a po kliknięciu pełną listę tego dnia.

**Tydzień:** siedem pionowych kolumn i poziome pasy zajmujące odpowiednie daty. Każdy kolidujący na tych samych dniach pasek w osobnym torze. Dopuszczalne przewijanie pionowe przy wielu pracach; nie ukrywaj ich bez sygnału. Nie wpisuj na wąski pasek niemożliwej do odczytania całej nazwy i kwoty — pokaż skrót i pełną treść po dotknięciu.

**Dzień:** zawsze pokazuj także roboty rozpoczęte wcześniej, lecz nadal trwające. Karta wyświetla np. „dzień 3 z 5”, nazwę, status, klienta, pełną kwotę umowy i saldo; kwota jest opisana „za całą robotę”, nie jako dzienny zarobek.

**Wybór:** dotknij paska → szczegóły; dotknij numeru daty → lista dnia; naciśnij + → formularz z odpowiednią datą. Przy nawigacji po okresach zachowaj ostatni wybrany tryb (miesiąc/tydzień/dzień), a „Dzisiaj” wraca do aktualnej daty lokalnej.

## 4. Obliczenia i dokładny model danych
- `Job(id UUID, title, startDate LocalDate, endDate LocalDate, startTime?, clientId?, address?, notes?, agreedAmountMinor Long, currency, status, labelColor, createdAt, updatedAt, deletedAt?)`.
- `Client(id UUID, name, phone?, address?, notes?, archived, createdAt, updatedAt)`.
- `Payment(id UUID, jobId, amountMinor Long, currency, paidAt LocalDate, method?, notes?, createdAt)`.
- Trwała baza w obecnym kodzie: SQLiteOpenHelper; docelowo może zostać przeniesiona do Room z bezstratną migracją. Preferencje ekranów: rememberSaveable; UI: Kotlin, Jetpack Compose i Material 3.
- Kwoty zapisywać jako całkowitą liczbę jednostek minor (np. 2600,00 EUR = 260000 centów), **nigdy Float/Double**. Parsować polski przecinek bez błędów precyzji.
- `paid = sum(payments for job)`; `remaining = agreed - paid`; ujemna kwota pozostała to **nadpłata**, oznaczona jednoznacznie.
- Dla przedziału widocznego `[a,b]`: `startDate <= b && endDate >= a` oraz brak usunięcia. Przy edycji konfliktów pomijaj własny identyfikator.
- Polityka płatności: dodatnia kwota, waluta identyczna jak zlecenia; wpłaty większe od kwoty mogą powodować nadpłatę, ale należy pokazać ostrzeżenie. Waluta nie podlega zmianie: wszystkie roboty i płatności są w EUR.
- Archiwizacja klienta nie rozłącza jego historii. Usuwanie pracy z wpłatami musi zachować spójność: wybierz logiczne usunięcie (deletedAt), ukryj je ze zwykłych list i raportów, a eksport świadomie zachowa dane wraz z płatnościami; możliwość przywrócenia przed trwałym usunięciem.
- W raportach rozdziel: *wartość umów aktywnych według daty rozpoczęcia*, *wpłaty według faktycznej daty otrzymania*, *bieżące należności ze wszystkich nieusuniętych nieanulowanych robót*. Nie opisuj sumy wartości umów jako gotówki otrzymanej.

## 5. Detale i reguły interakcji
**Szczegóły roboty:** nazwa, od/do, liczba dni kalendarzowych, status, klient, telefon (akcja otwarcia dialera), adres, notatka, cena, suma wpłat, saldo, historia wpłat. Akcje: edytuj, zakończ, anuluj, dodaj wpłatę, duplikuj z nowymi datami, usuń po potwierdzeniu.

**Statusy:** Zaplanowane / W trakcie / Zrobione / Anulowane. Status ma etykietę tekstową/ikonę, nie jedynie kolor. Kolor paska identyfikuje pracę, a nie automatycznie status. Zmiana statusu nie zmienia żadnej wpłaty.

**Klienci:** imię/nazwa, opcjonalnie numer, adres i notatki; lista powiązanych robót i saldo. Usunięcie/archiwizacja klienta nie może bez pytania usunąć jego zleceń.

**Listy i wyszukiwanie:** wyszukiwanie nazwy, klienta, adresu, notatek; sortowanie, filtrowanie po stanie i płatnościach, przejście do konkretnego miesiąca i szczegółów.

## 6. Kopia zapasowa — wymogi
Eksport/import przez Android Storage Access Framework. Eksport JSON zawiera `schemaVersion`, identyfikatory, wszystkie pola robót, klientów i płatności, waluty, daty ISO i kwoty w minor units. Import w MVP: **zastąp lokalne dane** dopiero po odczycie i walidacji całego pliku oraz wyraźnym potwierdzeniu z podglądem liczby rekordów. Wczytanie atomowe, a przy błędzie pełne wycofanie bez naruszenia starej bazy. Nie używać szerokich uprawnień pamięci.

## 7. Sytuacje graniczne, które programista musi przewidzieć
- Praca 30.12–03.01 pojawia się w obu latach; roboty zaczęte przed pierwszym widocznym dniem miesiąca nadal się pokazują.
- 29 lutego, tygodnie 4–6-rzędowe, lokalna zmiana czasu letniego i zimowego.
- Wielodniowe zlecenia w tym samym terminie; 15 zleceń jednego dnia; 1000 robót / 10000 wpłat.
- Szerokość 360 dp, font 200%, TalkBack, tryb ciemny, układ poziomy i przerwane działanie aplikacji.
- Brak sieci, anulowany wybór pliku, błędny JSON, nieznany schemaVersion, przerwanie importu.
- Zerowa wycena, bardzo duża kwota, niepoprawny separator dziesiętny, płatność częściowa i nadpłata.
- Przypadkowe dotknięcia/gesty nie mogą zmieniać terminu. Drag-and-drop dopiero po MVP.

## 8. Kolejność pracy dla agenta/kodera
1. Sprawdź oficjalną, aktualną dokumentację Android (wersje AGP/Kotlin/Compose), skonfiguruj projekt, CI i uruchom debug na emulatorze.
2. Utwórz modele, lokalne tabele SQLite, bezpieczny sposób aktualizacji schematu, obsługę dat i testy domenowe.
3. Zrób formularz tworzenia/edycji i szczegóły — zapis ma być trwały offline.
4. Zaimplementuj najpierw *czysty*, testowalny algorytm segmentacji kalendarza, a potem miesiąc, tydzień i dzień w Compose.
5. Dodaj klientów, wpłaty, raporty, filtry i wyszukiwarkę.
6. Dodaj eksport/import, obsługę stanów błędnych i dostępność.
7. Uruchom testy, popraw regresje, wygeneruj instalowalny APK i spisz wynik testów.
8. Dopiero potem rozszerzenia: powiadomienia, przeciąganie, fotografie, integracje, synchronizacja.

Każdy etap ma zawierać kod źródłowy, testy, aktualizację dokumentacji i wynik kompilacji. Nie uznawaj makiety, opisu lub pustego ekranu za wdrożoną funkcję.

## 9. Checklist odbiorowa (nie odznaczać przed testem)
- [ ] Dodano zlecenie trwające 5 dni — jedna cena, jeden rekord, ciągłe fragmenty paska przez tygodnie.
- [ ] Zapisano dwie nakładające się prace, po ostrzeżeniu obie widać.
- [ ] Zmieniono zakres — kwoty i powiązane wpłaty pozostają.
- [ ] Zamknięto i wznowiono aplikację offline, dane się zachowały.
- [ ] Zaznaczono „Zrobione”, a zaległość pozostała bez zmian.
- [ ] Dodano zaliczkę i dopłatę, saldo jest poprawne.
- [ ] Raport rozlicza poprawnie wszystkie kwoty w euro.
- [ ] Widok dnia i wyszukiwarka znajdują robotę zaczętą w poprzednim miesiącu.
- [ ] Kalendarz jest dostępny na małym ekranie i przy większej czcionce.
- [ ] Import poprawnej kopii odtwarza całość; uszkodzony import nie usuwa danych.
- [ ] Testy jednostkowe, SQLite, UI i smoke-test APK są zaliczone.

## 10. Granice MVP
**MVP:** lokalny kalendarz miesiąca/tygodnia/dnia, roboty wielodniowe, klienci, płatności częściowe, raporty, wyszukiwanie, backup, używalny UI, testy.

**Po MVP:** przypomnienia, drag-and-drop, zdjęcia, święta.

**Później:** konta, synchronizacja, współdzielenie z ekipą, faktury.

## 11. Wskazówki do przyszłych sesji programistycznych
Przed napisaniem kodu przeczytaj wszystkie pliki docs/*.md. Nie wybieraj samodzielnie innych zasad obsługi kwot, dat i konfliktów. Jeśli implementacja wymaga odejścia od specyfikacji, zapisz powód i zaktualizuj dokumentację oraz testy w tym samym pull requeście. Nie mów „aplikacja gotowa”, dopóki kod da się zbudować i przejść przez checklistę.
