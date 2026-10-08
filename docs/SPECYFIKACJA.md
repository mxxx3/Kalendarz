# Moje Roboty — specyfikacja aplikacji Android
Wersja 1.0, 8 października 2026.

## Cel
Osobisty organizer zleceń budowlanych, remontowych i serwisowych. Najważniejsze pytania: co mam robić, którego dnia, u kogo i za jaką kwotę? Podstawą jest **czytelny kalendarz miesiąca i tygodnia** z paskami robót obejmujących wiele kolejnych dni. Aplikacja działa bez internetu i konta.

## Zasady
- Dodanie roboty wymaga tylko nazwy, daty startu, daty końca i ceny. Dodatkowe pola opcjonalne.
- Każde zlecenie ma jeden rekord, nawet jeśli trwa kilkanaście dni; cena to kwota za całość, a nie za dzień.
- Roboty mogą się nakładać. Konflikt pokazuje ostrzeżenie, lecz nie blokuje zapisu.
- Status wykonania i status zapłaty są odrębne.
- Obsługa po polsku; domyślna waluta PLN, możliwa EUR; kalendarz od poniedziałku.
- Brak przesyłania danych klientów w sieci; ręczny eksport kopii jest świadomą czynnością użytkownika.

## Nawigacja
Zakładki: **Kalendarz / Roboty / Klienci / Finanse**. Ustawienia w nagłówku. Dostępny duży przycisk „+ Dodaj robotę”.

## Kalendarz — ekran główny
Wybór **Miesiąc / Tydzień / Dzień**, przyciski poprzedni/następny okres oraz „Dzisiaj”.
- Miesiąc: siatka 7 kolumn Pon–Nd, numery dat i poziome paski rozpięte przez kolejne dni. Pasek wykraczający poza koniec tygodnia ma kontynuację w następnym. Kolor identyfikuje robotę; status pokazany też tekstowo. Jeśli brak miejsca, przycisk „+N więcej” odsłania wszystkie roboty dnia.
- Tydzień: siedem kolumn, osobne poziome pasy dla nakładających się zleceń, przewijana lista gdy ich dużo. Paski pokazują nazwę i kwotę, o ile jest miejsce.
- Dzień: wszystkie prace przypisane do daty, z klientem, adresem, statusem, kwotą i zaległością płatniczą.
Dotknięcie dnia otwiera listę robót; dotknięcie paska szczegóły. Dotknięcie pustego dnia pozwala dodać robotę ze wstępną datą.

## Formularz roboty
Nazwa (wymagana), data od/do (wymagane, końcowa >= początkowej, obie daty włącznie), opcjonalna godzina, uzgodniona kwota >= 0 i waluta, status, klient, telefon przez kartę klienta, adres, notatki i kolor zlecenia. Domyślny status: Zaplanowane. Przy konflikcie terminów wyświetl nazwy przecinających się robót oraz opcję zapisania mimo kolizji.
Przyciski Zapisz/Anuluj; przy próbie wyjścia z niezapisanymi zmianami wyświetl potwierdzenie.

## Statusy i szczegóły
Statusy: **Zaplanowane, W trakcie, Zrobione, Anulowane**. Szczegóły pokazują daty, liczbę dni, klienta, lokalizację, opis, kwotę umówioną, sumę wpłat i saldo. Akcje: edytuj, zmień status, zarejestruj wpłatę, duplikuj, usuń z potwierdzeniem i możliwością cofnięcia. Anulowane pozostają w historii.

## Klienci
Imię/nazwa (wymagane), telefon, adres i notatki. Powiązane roboty i saldo należności. Archiwizacja klienta bez usuwania historii; otwieranie systemowego dialera bez dostępu do książki kontaktów.

## Roboty — lista
Zakładki Nadchodzące/Wszystkie/Zakończone/Anulowane; filtry zakresu dat, statusu, klienta i niezapłaconych; wyszukiwanie w nazwie, adresie, kliencie oraz opisie. Sortuj datą i wartością. Stan pusty zawiera możliwość dodania zlecenia.

## Finanse
Kwota umówiona każdej roboty liczona raz według daty rozpoczęcia. Zapłaty liczone według daty otrzymania. Saldo = umówiona kwota minus wpłaty. Zrobienie roboty nie oznacza zapłaty. Raport okresowy pokazuje wartość aktywnych zleceń, zakończonych robót, otrzymane pieniądze i zaległości. Nie dodawaj PLN do EUR; raporty osobno per waluta. Nadpłaty oznacz osobno.

## Ustawienia, dane, bezpieczeństwo
Motyw systemowy/jasny/ciemny, domyślna waluta, eksport/import danych i kopie zapasowe. Kopia JSON ma numer wersji schematu i zawiera roboty, klientów, płatności oraz relacje. Import: podgląd, walidacja i zapis transakcyjny albo rollback. Preferowany import „zastąp obecną bazę” z wyraźnym potwierdzeniem. Wybór pliku przez Android Storage Access Framework. Żadnych niepotrzebnych uprawnień pamięci.

## Zakres MVP i później
MVP: trzy kalendarze, CRUD zleceń, klienci, finanse, zapłaty częściowe, wyszukiwanie, filtry, backup, testy. Po MVP: powiadomienia, drag-and-drop, zdjęcia, dni wolne. Odległa przyszłość: synchronizacja, faktury, konta i zespoły.

## Ustalenia techniczne
Kotlin, Jetpack Compose, Material 3, Room, ViewModel, Flow, DataStore, Gradle Kotlin DSL. minSdk 26, compileSdk/targetSdk 36 na początek. Podczas implementacji sprawdź aktualne zgodne wersje AGP/Kotlin/KSP/Compose BOM. Android Developers: https://developer.android.com/jetpack/compose oraz https://developer.android.com/google/play/requirements/target-sdk .

Zobacz też [architekturę i dane](ARCHITEKTURA.md), [scenariusze](SCENARIUSZE.md), [plan realizacji](PLAN_REALIZACJI.md) i [testy](TESTY.md).
