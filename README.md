# to-ztp-project

Project for the **Object-Oriented Technologies (TO)** course at **AGH University of Science and Technology**.  
Group 6.

---

## Setup / Uruchomienie projektu

1. **Uruchomienie infrastruktury (Docker)**  
   W katalogu głównym projektu znajduje się plik `docker-compose.yml`.

   ```bash
   docker compose up -d
   ```

   Komenda uruchamia bazę danych oraz pozostałe wymagane usługi.

2. **Uruchomienie aplikacji**

   Aplikację uruchamiamy standardowo jako aplikację Spring Boot:
   - z poziomu IDE (uruchomienie klasy `main`)

---

## Implemented Milestone

### Milestone 1 – Completed

The following functionalities required for **Milestone 1** have been successfully implemented:

- A simple web server with at least one working endpoint
- Fetching **GTFS Realtime (TripUpdates)** data for public transport in Kraków
- Parsing GTFS Realtime data using **Protocol Buffers**
- Returning, via HTTP endpoint, information about:
  - a random stop
  - a random vehicle
  - and the departure time of a randomly selected bus or tram
---

### Milestone 2 – Completed

The following functionalities required for **Milestone 2** have been successfully implemented:

- Loading and parsing **GTFS Static** data:
  - `stops.txt`
  - `routes.txt`
  - `trips.txt`
  - `stop_times.txt`
  - `calendar.txt`
  - `calendar_dates.txt`
- Persisting required GTFS Static data in a **database** to speed up queries
- Automatic **verification of GTFS Static data freshness**:
  - on application startup
  - periodically during runtime with automatic refresh if changes are detected
- REST endpoint that:
  - accepts **current stop** and **destination stop** (names compliant with `stop_name` from `stops.txt`)
  - returns the **vehicle that will arrive the fastest**
  - supports **direct connections only**
  - takes into account:
    - days of the week based on `calendar.txt`
    - service exceptions defined in `calendar_dates.txt`
    - real-time delays using **GTFS Realtime**
- Combined processing of **GTFS Static + GTFS Realtime** data to determine the optimal connection

---

## Authors

- **Szymon Barczyk**
- **Jan Dyląg**
- **Wojciech Dąbek**
