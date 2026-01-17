# to-ztp-project

Project for the **Object-Oriented Technologies (TO)** course at **AGH University of Science and Technology**.  
Group 6.

---

## Setup / Project Startup

1. **Starting the infrastructure (Docker)**  
   The project root directory contains a `docker-compose.yml` file.

   ```bash
   docker compose up -d
   ```

   This command starts the database and all other required services.

2. **Starting the application**

   The application is started as a standard Spring Boot application:
   - from an IDE (by running the `main` class)

3. **Using the MCP Server**

   Follow the MCP standard for proper connection with stdio communication as described [here](https://modelcontextprotocol.io/specification/draft/basic/lifecycle).
   Our server supports the 2024-11-05 protocol version. Here is an example of good order of requests - remember to send each one of them separately:

   ```json
   { "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": { "protocolVersion": "2024-11-05", "capabilities": { "roots": { "listChanged": true }, "sampling": {}, "elicitation": { "form": {}, "url": {} }, "tasks": { "requests": { "elicitation": { "create": {} }, "sampling": { "createMessage": {} } } } }, "clientInfo": { "name": "ExampleClient", "title": "Example Client Display Name", "version": "1.0.0", "description": "An example MCP client application", "icons": [ { "src": "https://example.com/icon.png", "mimeType": "image/png", "sizes": ["48x48"] } ], "websiteUrl": "https://example.com" } } }
   { "jsonrpc": "2.0", "method": "notifications/initialized" }
   { "jsonrpc":"2.0","id":2,"method":"tools/list","params":{} }
   { "jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"getAllStops","arguments":{}} }
   ```

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

### Milestone 3 – In progress


(...)

## Authors

- **Szymon Barczyk**
- **Jan Dyląg**
- **Wojciech Dąbek**
