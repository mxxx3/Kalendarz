# Architektura i algorytm kalendarza

## Technologie
Android, Kotlin, Jetpack Compose, Material 3, Room (SQLite), Coroutines/Flow, ViewModel, DataStore. Offline first. DataStore przechowuje preferencje; Room jest źródłem prawdy dla prac i płatności.

## Encje
**Job**: id UUID, title String, startDate LocalDate, endDate LocalDate (włącznie), startTime/endTime nullable, clientId nullable, address nullable, notes nullable, agreedAmountMinor Long, currency ISO 4217, status enum PLANNED/IN_PROGRESS/DONE/CANCELLED, labelColor, createdAt/updatedAt Instant, deletedAt nullable.

**Client**: id UUID, name, phone nullable, address nullable, notes nullable, isArchived, createdAt/updatedAt.

**Payment**: id UUID, jobId FK, amountMinor Long dodatnie, currency zgodne z Job, paidAt LocalDate, method enum CASH/TRANSFER/OTHER nullable, notes nullable, createdAt Instant.

Pieniądze zawsze Long w groszach/centach, nie Double. Daty pracy jako daty kalendarzowe (LocalDate, bez UTC); chwile zdarzeń UTC. Indeksy: Job(startDate,endDate), Job(clientId), Job(status), Payment(jobId,paidAt). Migracje Room obowiązkowe i testowane.

## Zapytanie o okres
Wszystkie prace przecinające [rangeStart,rangeEnd] spełniają `job.startDate <= rangeEnd && job.endDate >= rangeStart` i nie są usunięte. Nie szukaj jedynie robót zaczynających się w danym miesiącu. Pobieraj zakres pełnej siatki miesiąca (pierwszy poniedziałek–ostatnia niedziela).

## Segmentacja pasów kalendarza
Dla tygodnia [weekStart,weekEnd] i pracy [start,end] licz:
```text
segmentStart = max(start, weekStart)
segmentEnd   = min(end, weekEnd)
if segmentStart > segmentEnd: brak segmentu
colStart = daysBetween(weekStart, segmentStart)
span = daysBetween(segmentStart, segmentEnd) + 1
leftContinues = start < weekStart
rightContinues = end > weekEnd
```
Każdy segment rysuj jako jeden zaokrąglony pasek o szerokości `span` kolumn, z wyraźnymi zakończeniami dla kontynuacji. Sortowanie deterministyczne: data początku rosnąco, czas trwania malejąco, ID. W obrębie tygodnia przydziel najniższy wolny tor bez pokrywania dni (interval packing). Na ekranie miesiąca ogranicz liczbę widocznych torów do 2–3 zależnie od przestrzeni, a ukryte prace pokaż jako „+N więcej”. W widoku tygodnia wszystkie tory można przewijać. Status musi być odróżnialny bez rozpoznawania kolorów.

## Niezmienniki i transakcje
- endDate >= startDate;
- każda umowa ma jedną cenę całkowitą;
- wpłata nie może mieć innej waluty niż praca;
- saldo = agreedAmountMinor - SUM(payment.amountMinor);
- status DONE nie oznacza opłacenia;
- konflikt nakładających się dat nie blokuje zapisu;
- edycja i import są atomowe; brak destrukcyjnej migracji;
- anulowane roboty pozostają w historii, lecz nie są w aktywnych planach przychodu.

## Warstwy
`feature/calendar`, `feature/jobs`, `feature/clients`, `feature/finance`, `feature/settings`; `data/local`, `data/repository`, `domain/usecase`, `core/model`, `core/time`, `core/money`. StateFlow w ViewModel; transformacje dat i finansów testowane jako czyste funkcje.

## Wydajność i prywatność
Test z 1000 robotami, 10000 wpłatami, mniejszym telefonem i fontem 200%. Zapytania Room i backup poza wątkiem UI. Bez konta i połączeń sieciowych wymaganych do pracy. Nie loguj telefonów i adresów klientów. Export świadomie wywoływany przez użytkownika. Powiadomienia (później) WorkManager best effort, od Android 13 obsłużyć POST_NOTIFICATIONS; nie obiecywać precyzyjnych alarmów.
