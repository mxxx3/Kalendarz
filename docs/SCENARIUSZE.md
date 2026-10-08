# Scenariusze użytkownika i odbiór MVP

1. **Wielodniowa praca.** Dodaj „Malowanie mieszkania” 12–14 października za 1200 EUR. Jeden pasek obejmuje trzy dni; wartość umowy to 1200, a nie 3600.
2. **Kolizja.** Dodaj „Remont łazienki” 13–16 października za 2600 EUR. Pojawia się ostrzeżenie; po akceptacji oba paski są jednocześnie widoczne w odrębnych torach.
3. **Wiele robót jednego dnia.** Tapnij 14 października. Lista pokazuje obie prace, ich statusy, klientów i ceny.
4. **Zmiana terminu.** Przesuń remont na 15–20 października. Po zapisie pasek natychmiast zmienia pozycję i kontynuuje się w następnym tygodniu.
5. **Zaliczka.** Zapisz wpłatę 500 EUR na remont za 2600 EUR. Saldo wynosi 2100 EUR, nawet gdy status jest „W trakcie”.
6. **Zakończenie.** Ustaw status „Zrobione”. Saldo nadal wynosi 2100 EUR.
7. **Dopłata.** Dodaj wpłatę 2100 EUR. Saldo 0 EUR, praca opłacona.
8. **Filtrowanie.** „Nieopłacone” ukrywa w całości opłacony remont, ale nie usuwa jego historii.
9. **Przejście przez rok.** Robota 30 grudnia–3 stycznia widoczna w obu miesiącach i latach.
10. **Zatłoczony dzień.** Dodaj osiem robót na jeden dzień: miesiąc pokazuje „+N więcej”; tapnięcie ujawnia wszystkie pozycje.
11. **Klient.** Utwórz klienta z numerem, przypisz trzy prace; dotknij numeru, aby uruchomić dialer.
12. **Archiwizacja klienta.** Po archiwizacji wszystkie prace nadal mają poprawną historię.
13. **Backup.** Eksport JSON, import do nowej instalacji, porównanie UUID, dat, klientów i płatności.
14. **Offline.** W trybie samolotowym dodawanie, edycja i raporty działają.
15. **Nieznana wycena.** Dodaj cenę 0 i zmień ją później.
16. **Anulowanie.** Anulowana robota pozostaje w historii, lecz nie w aktywnym planie przychodów.
17. **Waluta EUR.** Zapisz roboty w EUR; raport ma jeden raport w EUR.
18. **Błędne daty.** Data końca przed datą początku daje błąd i nie zapisuje zlecenia.
19. **Wznowienie.** Obróć ekran w trakcie formularza; wartości wprowadzane pozostają.
20. **Dostępność.** Przy powiększonej czcionce i TalkBack paski i detale pozostają używalne.

## Warunki odbioru
Każdy scenariusz posiada test automatyczny w miarę możliwości oraz test ręczny na urządzeniu. Wynik i ewentualny defekt rejestruj w issue. Wymagane są również testy z dokumentu [TESTY.md](TESTY.md).
