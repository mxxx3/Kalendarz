# Synchronizacja Moje Roboty — Firebase / wspólna ekipa

Wersja: 1.1 (2026-10-08).

## Co otrzymujesz
- Dwa lub więcej telefonów może używać **jednej wspólnej listy** robót, klientów i wpłat.
- Wszyscy członkowie ekipy widzą te same kalendarze i aktualizacje na bieżąco dzięki listenerom Cloud Firestore.
- Konta są osobne (email + hasło); członkostwo przypisane do Firebase Auth **UID**, bez udostępniania haseł.
- Android Firestore automatycznie buforuje ostatnie dane i kolejkuje zmiany offline. Po powrocie internetu synchronizuje je. **Jeżeli dwie osoby edytują ten sam dokument równocześnie, obowiązuje ostatni zapis**.
- Osobisty, istniejący kalendarz SQLite pozostaje nienaruszony. Po aktywacji ekipy źródłem prawdy staje się Firestore; po odłączeniu wracasz do osobistych, lokalnych danych. **Dane lokalne i współdzielone nie są automatycznie łączone**.

## Co MUSI zrobić administrator, zanim synchronizacja zadziała

**Konfiguracja Androida Firebase jest już przygotowana** dla projektu `kalendarz-8f0cb` i pakietu `pl.mojeroboty.app`. Publiczne identyfikatory zostały zapisane w `firebase-project.properties`. Samo przygotowanie APK nie włącza Authentication ani nie tworzy bazy Firestore — te usługi i reguły zabezpieczeń należy skonfigurować w Firebase Console.

1. Wejdź na https://console.firebase.google.com i utwórz projekt (np. \`moje-roboty-ekipa\`). Poznaj ewentualne limity i koszty Cloud Firestore.
2. Dodaj w Firebase aplikację Android o **package name** \`pl.mojeroboty.app\`.
3. W **Authentication → Sign-in method** włącz **Email/Password**. Użytkownicy powinni używać silnych, unikatowych haseł.
4. W **Firestore Database** utwórz bazę w wybranym regionie (najlepiej blisko użytkowników, zgodnie z wymaganiami prywatności). Wybierz zabezpieczone reguły, nie „public test mode”.
5. Otwórz Firestore → **Rules** i **opublikuj dokładnie zawartość** [../firestore.rules](../firestore.rules). Reguły ograniczają dostęp do UID dodanych do ekipy.
6. Plik przesłany przez właściciela został wykorzystany do przygotowania **publicznych identyfikatorów klienta Firebase** w `firebase-project.properties`. Build korzysta z tych wartości automatycznie, chyba że ustawisz własne niepuste `FIREBASE_*` w środowisku lub w lokalnym `local.properties`.
7. Nie umieszczaj w GitHub żadnych haseł, kluczy administracyjnych, plików kont usługowych ani danych klientów. Według [oficjalnej dokumentacji Firebase](https://firebase.google.com/docs/projects/api-keys) klucz klienta Firebase jest publicznym identyfikatorem i nie nadaje dostępu do bazy. Dostęp chronią reguły Firestore i Firebase Auth; w Google Cloud sprawdź ograniczenia klucza do usług Firebase.
8. Skompiluj i zainstaluj **tę samą wersję APK** na obu telefonach. Każdy build udostępniany ekipie musi wskazywać ten sam projekt Firebase.
9. Zadbaj o ograniczenia dostępu klucza Android API w Google Cloud, prawidłową konfigurację Authentication (w tym wymagane przez aktualną wersję SDK zabezpieczenia), a przed publiczną dystrybucją włącz App Check. Same klucze API nie zastępują reguł bezpieczeństwa.

## Jak połączyć 2 osoby (pierwsze uruchomienie)

**Telefon właściciela:**
1. Ustawienia (zębatka) → **Wspólna ekipa — synchronizacja**.
2. Zarejestruj konto email + hasło lub zaloguj się.
3. Wpisz nazwę, np. „Ekipa Remontowa”, naciśnij **Utwórz wspólną ekipę**.
4. Opcjonalnie: jeśli masz wcześniej zapisane lokalne roboty i chmura jest pusta, użyj **Wyślij lokalne dane do ekipy**. Potwierdź operację. Import ma limit 450 widocznych rekordów (roboty + klienci + wpłaty) i jest atomowy.
5. Współpracownik zakłada konto u siebie i wysyła Ci **swój UID**. Wklej UID w polu „Dodaj współpracownika” i wybierz „Nadaj dostęp”.
6. Skopiuj **identyfikator ekipy** i przekaż go współpracownikowi.

**Telefon współpracownika:**
1. Zainstaluj APK wskazujący **ten sam projekt Firebase**, wejdź w Ustawienia → synchronizacja.
2. Załóż własne konto lub zaloguj się. Skopiuj UID i podaj go właścicielowi.
3. Kiedy właściciel doda UID, wklej identyfikator ekipy w pole „Dołącz” i naciśnij **Dołącz**.
4. Teraz obaj widzicie i edytujecie **wspólne** zlecenia. Nie trzeba ręcznie eksportować plików.

## Zasady danych

- Osobiste rekordy SQLite nie są kasowane po przełączeniu w tryb ekipy. Przejście z ekipy do trybu lokalnego **nie pobiera zmian z chmury** do SQLite — to dwa odrębne zbiory danych.
- Eksport/import JSON w bieżącym interfejsie dotyczy tylko lokalnej SQLite. W trybie ekipy przyciski są wyłączone, aby nie wyeksportować nieprawidłowej kopii. Kopie Firestore trzeba zapewnić osobno (np. zarządzana usługa backup/eksport projektu, zależnie od planu).
- **Usunięcie** roboty w chmurze ustawia pole \`deleted=true\`, nie usuwa fizycznego dokumentu. Wpłaty można usuwać (hard delete); nie ma mechanizmu cofania ani audytu dla wpłat.
- Nie przełączaj kont na współdzielonym telefonie bez zrozumienia, że Firestore przechowuje lokalny cache na urządzeniu. Telefon powinien mieć blokadę ekranu i osobny profil urządzenia dla użytkownika.
- Dane klientów mogą zawierać dane osobowe; dostęp uzyskują wszyscy członkowie ekipy dodani przez właściciela. Nie publikuj UID i identyfikatora ekipy jako „otwartych zaproszeń”; UID nie jest hasłem, ale to identyfikator konta.
- Firestore Security Rules wdraża się w **panelu Firebase**, nie przez sam commit pliku do GitHub.
- **Brak rozwiązywania konfliktów per pole:** ostatnia zmiana tego samego dokumentu wygrywa. Dotyczy nawet dwóch różnych pól edytowanych jednocześnie; należy unikać jednoczesnej edycji tego samego zlecenia.
- Status „offline” może sygnalizować dane z lokalnego cache i nie gwarantuje, że są najświeższe.

## Bezpieczeństwo reguł

\`firestore.rules\`:
- uprawnienia do \`workspaces/{id}\` ma właściciel i członkowie;
- tylko właściciel może dodać/cofnąć członkostwo (lista \`memberUids\`);
- członkowie mogą odczytywać i edytować powiązane \`jobs\`, \`clients\`, \`payments\`;
- niezalogowani i UID spoza ekipy nie mogą czytać danych;
- żadna inna kolekcja nie jest publicznie udostępniona.

UID nie może być zastąpione dowolnym „kodem ekipy” bez autoryzacji; sama znajomość identyfikatora zespołu nie daje dostępu.

## Testy przed użytkowaniem z prawdziwymi danymi

- [ ] Uruchom **konfigurowany Firebase** build (APK bez \`FIREBASE_*\` to tylko tryb lokalny).
- [ ] Utwórz dwóch odrębnych użytkowników Firebase Auth.
- [ ] Stwórz ekipę, dodaj UID użytkownika B, dołącz z B.
- [ ] Z A dodaj trzydniową robotę za 1200,00 €; pojawia się w kalendarzu B.
- [ ] Z B popraw status; aktualizacja widoczna w A.
- [ ] Z A dodaj klienta, z B zaliczkę 500,00 €; oba salda wynoszą 700,00 €.
- [ ] Wyłącz internet na B, zapisz nową pracę, włącz internet, sprawdź synchronizację.
- [ ] Spróbuj czytać ekipę z trzeciego konta bez członkostwa — reguły **odmawiają**.
- [ ] Usuń UID B ze \`memberUids\`, sprawdź brak dostępu po odświeżeniu.
- [ ] Sprawdź zachowanie w razie równoczesnych zmian (last-write-wins).
- [ ] Sprawdź, że przejście w tryb lokalny nie wymazuje jego oryginalnych danych.
- [ ] Uruchom testy reguł w Firebase Emulator Suite (zalecane przed wdrożeniem do produkcji).

## Odnośniki

- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/firestore/query-data/listen
- https://firebase.google.com/docs/firestore/manage-data/enable-offline
- https://firebase.google.com/docs/firestore/security/get-started
