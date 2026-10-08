# UX/UI — dokładny scenariusz interfejsu „Moje Roboty”
Wersja 1.0 • 2026-10-08. Uzupełnienie [specyfikacji](SPECYFIKACJA.md) i [algorytmu kalendarza](ARCHITEKTURA.md).

## 1. Główna zasada
Na ekranie startowym użytkownik **od razu widzi kalendarz**. Żadnego logowania, wdrożenia z wieloma slajdami ani obowiązkowego wypełniania formularzy. Najważniejsze: „Kiedy jestem zajęty? Co robię? Za ile?”. Jedno zlecenie trwa od 1 do N dni i ma jedną całkowitą wycenę. Nakładające się roboty są dozwolone i widoczne równocześnie.

## 2. Mapa interfejsu
Dolna nawigacja: **Kalendarz**, **Roboty**, **Klienci**, **Finanse**. Ikona ustawień w górnym pasku. Na Kalendarzu i Robotach widoczny przycisk **+ Dodaj robotę**. Wszystkie etykiety w języku polskim, kwoty i daty lokalizowane.

| Ekran | Co widać natychmiast | Najważniejsze działanie |
|---|---|---|
| Kalendarz | Obecny miesiąc lub tydzień i paski robót | Tap w dzień/pasek albo + |
| Dzień | Pełna lista robót danego dnia, również zaczętych wcześniej | Tap -> szczegóły |
| Dodawanie/edycja | Nazwa, początek, koniec, **kwota za całość** | Zapisz |
| Szczegóły roboty | Termin, klient, adres, status, kwota, suma wpłat, pozostało | Edytuj / Dodaj wpłatę |
| Roboty | Nadchodzące, wszystkie, zakończone, anulowane | Szukaj i filtruj |
| Klienci | Nazwa, telefon, historia zleceń, saldo | Nowy klient |
| Finanse | Umówione, otrzymane, do otrzymania per waluta | Filtr okresu |
| Ustawienia | Motyw, waluta, kopia zapasowa i import | Eksport danych |

## 3. Projekt ekranu kalendarza
### Nagłówek
„Październik 2026” z przyciskami wstecz/dalej; obok „Dzisiaj”. Segment **Miesiąc / Tydzień / Dzień**. Zapamiętaj ostatni tryb w DataStore. Data zaznaczonego dnia ma wyraźną ramkę/wypełnienie, a „dzisiaj” niezależne oznaczenie.

### Widok miesiąca — domyślny
- Siatka 7 kolumn od poniedziałku do niedzieli; 4–6 tygodni i dni sąsiednich miesięcy wyszarzone.
- Numer dnia w każdym polu. Zlecenia to poziome paski przez wszystkie dni, których dotyczą. Każdy pasek ma nazwę, a gdy jest miejsce także kwotę; **nie powtarzaj pełnej ceny w każdym dniu**.
- Przekroczenie niedzieli: pasek kontynuuje się w następnym rzędzie tygodnia. Kontynuację pokazują otwarte/niedomknięte końce lub strzałki.
- Równoległe prace zajmują osobne tory. Na małych ekranach najwyżej 2–3 paski w tygodniu, reszta przez „+N więcej” dla danego dnia.
- Dotknięcie numeru dnia lub „+N więcej” otwiera panel pełnej listy dnia. Dotknięcie paska otwiera detale konkretnego zlecenia. Długie przytrzymanie pustego dnia może wywołać dodawanie, lecz nie jest jedyną drogą.
- Kolor paska identyfikuje zlecenie (paleta wybierana podczas dodawania), **status zawsze również słownie/ikoną**, bez polegania tylko na kolorach.
- Pod kalendarzem może być zwijana lista prac wybranego dnia; na małych ekranach najpierw kalendarz, lista przewijana pod nim.

### Widok tygodnia — szczególnie dla wielu długich robót
- Siedem równych kolumn Pon–Nd z datami.
- Paski rozciągnięte od dnia startu do końca w oddzielnych wierszach (układ jak harmonogram/Gantt). W razie zbyt wielu pasków obszar przewija się pionowo; nie chowaj robót bez wskaźnika.
- Nawet dla dwóch nakładających się prac pasek numer 2 musi pozostać osobnym wierszem; kwota na pasku jest wyświetlana **jeden raz**.
- Przy bardzo krótkim pasku pokaż skróconą nazwę/ikonę, a pełną treść w detalu.
- Przewijanie poziome okresów, zmiana tygodnia i „Dzisiaj” nie mogą przypadkowo edytować zlecenia.
- Akcja przeciągnij-i-upuść **nie jest częścią MVP**.

### Widok dnia
Nagłówek: dzień tygodnia + pełna data. Karty na liście: nazwa, klient, lokalizacja, czas opcjonalny, „dzień X z Y” dla pracy wielodniowej, status, cena całkowita oraz „pozostało do zapłaty”. Gdy brak robót: „Brak robót na ten dzień” i przycisk „Dodaj robotę”.

## 4. Szybki formularz dodawania
1. Tap „+ Dodaj robotę”.
2. Wpisz **nazwę** (autofocus tylko jeśli klawiatura nie zasłoni kalendarza).
3. Wybierz zakres **Od** i **Do** (domyślnie oba = data wybranego dnia albo dziś).
4. Wpisz **kwotę za całą robotę**, np. „2600,00”. Przy kwocie musi być podpis „Za całe zlecenie (nie za dzień)”.
5. Tap „Zapisz”. Zapis jest trwały lokalnie, a kalendarz od razu się aktualizuje.
6. Sekcja „Więcej szczegółów” rozwija pola: klient/nowy klient, adres, notatka, godzina, waluta, status, kolor.
7. Jeśli wybrano okres z innymi robotami, wyświetl nienachalny komunikat „Nakłada się z: …” i akcję „Zapisz mimo to”.

**Walidacje:** niepusta nazwa po trim, data końca >= początku, poprawna kwota >= 0, kontrola przepełnienia Long i precyzji waluty. Pusta kwota musi mieć jawnie ustaloną semantykę: w MVP pole wymagane, ale 0 oznacza jeszcze niewycenione; UI oznacza „Wycena do ustalenia”, a w raporcie nie przedstawia jej jako pewnego przychodu. Wybór waluty z kodów ISO 4217; dla PLN/EUR dwie cyfry groszy/centów. Przypadkowy Back przy zmianach = dialog „Odrzucić zmiany?”.

## 5. Szczegóły zlecenia
Widok „Remont łazienki”: 13–16 października (4 dni kalendarzowe), status „W trakcie”, klient i telefon, adres, notatka, kwota umówiona 2600 PLN, wpłacono 500 PLN, pozostało 2100 PLN. Przyciski: **Edytuj**, **Dodaj wpłatę**, **Zmień status**. Menu dodatkowe: duplikuj z wyborem nowych dat, anuluj, usuń z potwierdzeniem. „Zrobione” **nie zmienia** salda na zero. Kwota ujemnego salda = nadpłata i wymaga czytelnego oznaczenia.

## 6. Przepływy użytkownika
- **Szybkie dodanie:** dziś -> + -> tytuł -> od/do -> kwota -> zapisz -> pasek natychmiast widoczny.
- **Roboty nakładające się:** zapisz A na Pon–Śr -> zapisz B na Wt–Pt -> ostrzeżenie -> zatwierdź -> obie pozycje osobno.
- **Zmiana terminu:** pasek -> szczegóły -> edytuj daty -> zapisz -> ta sama robota, te same wpłaty/klient, nowa pozycja.
- **Częściowa zapłata:** szczegóły -> dodaj wpłatę -> kwota/data/metoda -> zapisz -> odświeżone saldo.
- **Odnalezienie klienta:** Klienci -> szukaj -> karta -> wszystkie zlecenia i należności -> opcjonalnie otwórz telefon w dialerze.
- **Powrót po miesiącu:** uruchom aplikację offline -> wszystkie wpisy pozostają; kalendarz pokazuje aktualny okres, preferowany tryb zachowany.
- **Bezpieczny eksport:** ustawienia -> eksport -> systemowy wybór lokalizacji -> komunikat o wyniku; import: wybór pliku -> podgląd liczby rekordów i walut -> potwierdzenie zastąpienia -> transakcja/rollback.

## 7. Sytuacje trudne (obowiązkowe)
- Praca zaczęta przed początkiem widocznego miesiąca; praca kończąca się po miesiącu; 31 grudnia -> styczeń; 29 lutego.
- Praca jednodniowa; zero robót; 15 robót jednego dnia; 1000 zapisanych robót.
- Zmiana orientacji, zamknięcie procesu podczas formularza, duża czcionka 200%, ekran szerokości 360dp.
- Mało miejsca: bez poziomego obcinania całego kalendarza; skróty nazw, lista dnia i dostępność zamiast mikroskopijnego tekstu.
- Brak internetu, odmowa uprawnień, duża liczba wpłat, uszkodzony import, waluty PLN/EUR.
- Anulowanie nie usuwa historii; archiwizacja klienta nie usuwa jego robót.
- Walidacja zakresów przed zapisem, a nadmierna liczba dni nie może zawieszać UI.

## 8. Style i dostępność
- Material 3, jasny/ciemny/systemowy; białe/tła neutralne, wyraźny granat i błękit, zieleń tylko jako akcent. Kontrast minimum WCAG AA dla istotnego tekstu.
- Pola dotykowe minimum około 48dp; Dynamic Type/fontScale; czytelne etykiety dla TalkBack (np. „Remont łazienki, od wtorku do piątku, w trakcie, dwa tysiące sześćset złotych”).
- Zamiast sama czerwono/zielona kropka użyj nazw „Zaplanowane”, „W trakcie”, „Zrobione”, „Anulowane”.
- Potwierdzanie usuwania i importu; proste komunikaty błędów, bez technicznych stack trace.
- Kalendarz i dane pozostają responsywne na małych ekranach. Nie kopiuj makiety 1:1, jeśli pogarsza czytelność.

## 9. Konkretne kryteria akceptacji UI
- [ ] Po uruchomieniu od razu widać kalendarz i akcję dodania.
- [ ] Zlecenie 12–14 X jest jednym rekordem i wizualnie łączy 3 dni.
- [ ] Dwie nakładające się roboty nie zasłaniają się.
- [ ] Kliknięcie w dzień pokazuje **wszystkie** prace obejmujące dzień, także zaczęte wcześniej.
- [ ] Wyświetlana kwota jest za **całą** robotę, nie mnożona przez liczbę dni.
- [ ] „+N więcej” nie ukrywa bezpowrotnie żadnego zlecenia.
- [ ] Dzień, tydzień i miesiąc pokazują spójne dane z Room.
- [ ] Do zapisania pracy wystarczy nazwa, zakres dat i kwota (zero dopuszczone).
- [ ] Saldo poprawne po wpłacie i oznaczeniu jako „Zrobione”.
- [ ] Obsługa bez internetu, duży tekst i TalkBack sprawdzone na urządzeniu.

## 10. Priorytet implementacji
Pierwsze demo: **Room + dodaj robotę + kalendarz tygodnia z poprawnymi paskami + szczegóły**. Następnie miesiąc, dzień, klienci, finanse, backup i testy. Nie zaczynaj od synchronizacji, faktur czy przeciągania pasków: nie są konieczne do podstawowego celu.

Powiązania: [scenariusze](SCENARIUSZE.md), [plan implementacji](PLAN_REALIZACJI.md), [testy](TESTY.md).