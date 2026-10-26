# Edge Tree

An API service that stores a tree as parent → child edges and returns any subtree as JSON. Built with Kotlin, Spring Boot 4, jOOQ and Flyway.

The design choices and trade-offs are written up in [DECISIONS.md](DECISIONS.md).

You can see performance test result in [PERFORMANCE.md](PERFORMANCE.md).

## What you need

- **JDK 21**
- **Docker**, for the database and for running the tests

## Run it locally

**1. Start PostgreSQL**

```bash
docker run -d --name edge-tree-postgres \
  -e POSTGRES_DB=edge_tree -e POSTGRES_USER=edge_tree -e POSTGRES_PASSWORD=edge_tree \
  -p 5433:5432 postgres:18.6
```

**2. Point the app at it.** Copy `.env.example` to `.env` and fill it in:

```properties
DB_URL=jdbc:postgresql://127.0.0.1:5433/edge_tree
DB_USERNAME=edge_tree
DB_PASSWORD=edge_tree
```

**3. Start the app**

```bash
./gradlew bootRun
```

The API runs on `http://localhost:8080`, and the interactive docs are at **http://localhost:8080/swagger-ui.html**.

## The API

| Method   | Path                                | What it does                                                           |
| -------- | ----------------------------------- | ---------------------------------------------------------------------- |
| `POST`   | `/api/v1/edges`                     | Add an edge. Body: `{"fromId": 1, "toId": 2}` (`fromId` is the parent) |
| `DELETE` | `/api/v1/edges?fromId=1&toId=2`     | Remove an edge. The child keeps its own subtree                        |
| `GET`    | `/api/v1/trees/{nodeId}?maxDepth=2` | Get the subtree under a node. `maxDepth` is optional                   |

A quick try, using the example tree from the brief:

```bash
for e in "1 2" "1 3" "2 4" "2 5" "3 6"; do
  set -- $e
  curl -s -X POST localhost:8080/api/v1/edges -H 'Content-Type: application/json' -d "{\"fromId\":$1,\"toId\":$2}"
done

curl -s localhost:8080/api/v1/trees/2
# [{"id":2,"parentId":null,"depth":0},{"id":4,"parentId":2,"depth":1},{"id":5,"parentId":2,"depth":1}]
```

The tree comes back as a flat list, level by level. Rebuild the nesting from `parentId` if you need it. It is streamed, so even very large trees don't have to fit in memory.

## Tests

```bash
./gradlew test
```

The database tests start their own throwaway PostgreSQL with Testcontainers, so they never touch your data. Docker needs to be running.
