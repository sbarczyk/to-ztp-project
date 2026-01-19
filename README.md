# to-ztp-project

Project for the **Object-Oriented Technologies (TO)** course  
**AGH University of Science and Technology**  
Group 6

---

## Project Overview

The goal of this project is to build a backend system for querying public transport data
for the city of **Kraków**, based on **GTFS Static** and **GTFS Realtime** feeds.

The system evolves across milestones:
- M1: basic server + GTFS Realtime
- M2: GTFS Static + routing logic
- M3: Model Context Protocol (MCP) server with AI-oriented tooling and E2E validation

---

## Setup / Project Startup

### 1. Starting infrastructure (Docker)

The project root directory contains a `docker-compose.yml` file.

```bash
docker compose up -d
```

This starts:
- PostgreSQL database
- all required infrastructure services

---

### 2. Starting the application

The application is a standard **Spring Boot** project.

You can start it:
- directly from an IDE (run `ToZtpApplication`)
- with the active Spring profile of your choice (e.g. `dev`, `test`)

---

## MCP Server Setup and Usage

This project exposes its functionality via a **Model Context Protocol (MCP) server**
using **stdio communication** (JSON-RPC over stdin/stdout).

The server follows the MCP specification:
https://modelcontextprotocol.io/specification/draft/basic/lifecycle

Supported protocol version:
```
2024-11-05
```

---

### MCP Connection Lifecycle (Step by Step)

Each JSON message **must be sent separately as a single line**.

---

### Step 1: Initialize connection



The client announces protocol version and capabilities.

```json
{ "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": { "protocolVersion": "2024-11-05", "capabilities": {} } }
```

---

### Step 2: Send initialized notification

This confirms the client is ready.

```json
{ "jsonrpc": "2.0", "method": "notifications/initialized" }
```

---

### Step 3: List available tools

This returns all MCP tools exposed by the server.

```json
{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }
```

---

### Step 4: List stop names (pagination)

Lists unique stop names using cursor-based pagination.

```json
{ "jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": { "name": "listStops", "arguments": { "limit": 5 } } }
```

---

### Step 5: Find fastest connections between two stops

Returns **up to 3 direct connections** that depart the soonest,
taking **real-time delays** into account.

```json
{ "jsonrpc": "2.0", "id": 4, "method": "tools/call", "params": { "name": "findFastestConnections", "arguments": { "fromStop": "Teatr Bagatela", "toStop": "Bronowice Małe" } } }
```

Optional parameters:
- `date` (yyyy-MM-dd)
- `time` (HH:mm or HH:mm:ss)

---

### Step 6: List next departures for a stop and line

Returns **next 5 departures** for a given stop and line,
including real-time delays.

```json
{ "jsonrpc": "2.0", "id": 5, "method": "tools/call", "params": { "name": "listNextDepartures", "arguments": { "stopName": "Teatr Bagatela", "lineNumber": "4" } } }
```

---

## Implemented Milestones

### Milestone 1 – Completed

- Basic Spring Boot server
- GTFS Realtime (TripUpdates) parsing using Protocol Buffers
- REST endpoint returning:
  - random stop
  - random vehicle
  - random departure time

---

### Milestone 2 – Completed

- Full GTFS Static parsing:
  - stops, routes, trips, stop_times
  - calendar and calendar_dates
- Database persistence for fast querying
- Automatic GTFS Static refresh
- Routing logic for:
  - direct connections only
  - calendar rules and exceptions
  - GTFS Realtime delays

---

### Milestone 3 – Completed

- MCP server based on Spring AI MCP
- MCP tools providing:
  - listing all unique stop names (cursor-based pagination)
  - finding 3 fastest connections between two stops
  - listing next 5 departures for a given stop and line
- Real-time delay handling integrated into MCP responses
- Clean separation between:
  - domain logic
  - MCP layer
  - DTOs
- External **E2E MCP client** communicating via stdio
- E2E testing approach:
  - application started as an external process
  - MCP client sends JSON-RPC commands
  - no mocking of repositories or services

---

## Authors

- **Szymon Barczyk**
- **Jan Dyląg**
- **Wojciech Dąbek**
