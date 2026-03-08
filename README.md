# ⚽ AllFootball – Real-Time Football Data Platform

A full-stack real-time football data platform that aggregates live match events, team data, player statistics, and football news.

This system integrates an external football API with a self-built backend architecture to provide **real-time match updates**, **historical data queries**, and **interactive dashboards**.

The project was developed as a Final Year Project and demonstrates the design and implementation of a **real-time sports data system** using modern backend technologies.

---

# 🚀 Key Features

### Real-Time Match Updates

- Scheduled polling of external football API
- Live match monitoring
- Delta detection algorithm for event changes
- WebSocket push to frontend clients

### Football Data Query System

- Fixtures
- Teams
- Players
- Player statistics
- Match statistics
- Lineups

### Intelligent Caching Layer

- Redis-based query cache
- Historical season caching
- Cache TTL strategy

### Backend Management System

- Admin CRUD for teams, players, fixtures
- News publishing system
- User comments and follow system

---

# 🏗 System Architecture

```
                External Football API
                        │
                        ▼
               Scheduler (Polling)
                        │
                        ▼
                Delta Detection
                        │
                        ▼
                Redis Snapshot
                        │
                        ▼
                MySQL Persistence
                        │
                        ▼
               WebSocket Push
                        │
                        ▼
                 React Frontend
```

The system combines **API polling, caching, and event-driven updates** to efficiently deliver live football data.

---

# 🧱 Backend Architecture

```
Controller
   │
Service Layer
   │
Query Services
   │
MyBatis Mapper
   │
MySQL Database

           │
           ▼

       Redis Cache

           │
           ▼

External Football API
```

Backend is implemented using **Spring Boot with a layered architecture**.

---

# 🛠 Tech Stack

## Backend

- Spring Boot
- MyBatis
- MySQL
- Redis
- WebSocket
- Jackson JSON Parser
- Scheduled Tasks

## Frontend

- React
- Material UI
- Apache ECharts

## Deployment

- Docker
- AWS EC2
- Vercel (Frontend)

---

# 🗄 Database Schema

Main entities:

```
users
teams
players
fixtures
player_stats
match_events
match_statistics
lineups
lineup_players
news
comments
follows
```

Relationships:

```
teams → players
teams → fixtures
fixtures → match_events
fixtures → match_statistics
fixtures → lineups → lineup_players
players → player_stats
```

---

# ⚡ Real-Time Match Update System

The real-time module continuously polls the football API and detects changes in match events.

### Workflow

```
Scheduler
   │
Fetch live fixtures
   │
Compare with Redis snapshot
   │
Detect changes (Delta detection)
   │
Update Redis snapshot
   │
Push updates via WebSocket
   │
Persist final match data to MySQL
```

### Delta Detection Optimization

The original approach required **O(n²)** comparisons between match events.

This project optimizes the algorithm to **O(n)** using a **hash-based comparison strategy**.

---

# 🧠 Design Patterns

The project applies the **Strategy Pattern** in the real-time delta detection module.

```
DeltaDetectionStrategy
   │
   ├── ScoreChangeStrategy
   ├── EventChangeStrategy
   ├── StatusChangeStrategy
   └── TimeChangeStrategy
```

This design allows new event detection logic to be added without modifying the core detection engine.

---

# ⚡ Redis Caching Strategy

Redis is used as a **query acceleration layer**.

## Cache Keys

Fixture List

```
fixtures:{leagueId}:{season}:{page}:{size}
```

Fixture Detail

```
fixture:detail:{fixtureId}
```

Team List

```
teams:{leagueId}:{season}:{page}:{size}
```

Team Detail

```
team:detail:{teamId}
```

Player List

```
players:{teamId}:{season}:{page}:{size}
```

Player Detail

```
player:detail:{playerId}
```

## Cache TTL

```
6 hours
```

## Cache Workflow

```
1. Request
2. Redis lookup
3. Cache miss
4. Query MySQL / External API
5. Serialize result
6. Store in Redis
7. Return response
```

---

# 📡 API Examples

### Get Fixtures

```
GET /fixtures?page=1&size=10&leagueId=39&season=2024
```

### Fixture Detail

```
GET /fixtures/{fixtureId}?season=2023
```

### Team List

```
GET /teams?page=1&size=20&leagueId=39&season=2023
```

### Team Detail

```
GET /teams/{teamId}?season=2023
```

### Player Detail

```
GET /players/{playerId}?season=2023
```

---

# 📦 Project Structure

```
backend
 ├── controller
 │
 ├── service
 │    ├── impl
 │    ├── query
 │
 ├── mapper
 │
 ├── model
 │    ├── entity
 │    ├── vo
 │
 ├── config
 │
 ├── scheduler
 │
 └── websocket
```

---

# 🧪 Running the Project

## 1 Install Dependencies

```
Java 17+
MySQL
Redis
Maven
```

---

## 2 Configure Environment

Edit `application.yml`

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/allfootball
    username: root
    password: yourpassword

redis:
  host: localhost
  port: 6379
```

---

## 3 Run Backend

```
mvn spring-boot:run
```

Server starts at

```
http://localhost:8080
```

---

# 📊 Future Improvements

- Message queue for event streaming
- GraphQL API layer
- Distributed caching
- Kubernetes deployment

---

# 👨‍💻 Author

Sicheng Mu  
BSc Computing – Software Systems Development  

Final Year Project – Real-Time Football Data Platform
