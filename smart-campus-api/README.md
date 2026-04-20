# Smart Campus API — 5COSC022W Coursework

**Student number:** w2113683  
**Module:** Client-Server Architectures  
**Due:** 24th April 2026

---

## How to run

1. Open the project in NetBeans
2. Right click the project → Clean and Build
3. Right click → Run (or F6)
4. The server starts at `http://localhost:8080/api/v1`

---

## Endpoints

| Method | URL | What it does |
|--------|-----|-------------|
| GET | /api/v1 | API info and links |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{roomId} | Get one room |
| DELETE | /api/v1/rooms/{roomId} | Delete a room |
| GET | /api/v1/sensors | Get all sensors (optional ?type= filter) |
| POST | /api/v1/sensors | Register a sensor |
| GET | /api/v1/sensors/{sensorId} | Get one sensor |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history |
| POST | /api/v1/sensors/{sensorId}/readings | Add a new reading |

---

## Part 1 — Questions

### JAX-RS Resource Lifecycle

By default JAX-RS creates a new instance of a resource class for every single HTTP request that comes in. So if two people hit `/rooms` at the same time, two separate `RoomResource` objects get created. Once the response is sent the object gets garbage collected.

This matters a lot for how you store data. Because each request gets a fresh resource object, you cannot store data inside the resource class itself — it would just disappear after the request ends. That is why I created a separate `DataStore` class that only gets created once (a singleton). All the resource classes grab the same `DataStore` instance using `getInstance()`, so the data persists between requests.

The other issue is thread safety. Because multiple requests can run at the same time, two threads could try to write to the same `HashMap` simultaneously which can cause bugs or even crash the app. I handled this by making `getInstance()` synchronized, and for the readings list I check if the key exists before adding a new list, which avoids accidentally overwriting data.

### HATEOAS

HATEOAS stands for Hypermedia As The Engine Of Application State. The idea is that instead of a client having to already know all the URLs of an API, the responses themselves include links pointing to related resources. It is similar to how a website works — you start at a homepage and follow links rather than having to know every URL in advance.

The main benefit for developers is that the API becomes self-documenting. If I call `GET /api/v1` and the response gives me links to `/api/v1/rooms` and `/api/v1/sensors`, I do not need to read separate documentation to know where to go next. It also means that if a URL changes on the server side, clients that follow the links in responses will automatically pick up the new path without needing to update their code. Static docs go out of date but a HATEOAS response is always current.

---

## Part 2 — Questions

### Returning full objects vs just IDs

When a client calls `GET /api/v1/rooms` there are basically two options: send back just a list of IDs, or send back the full room objects including name, capacity and sensor IDs.

If you only send IDs, the payload is tiny which is good for bandwidth, but the client then has to make a separate GET request for every single room to get any useful information. If there are 200 rooms that is 201 HTTP requests which is very wasteful.

Returning the full objects means one request gets everything the client needs in one go. The downside is that the payload is bigger, but for most use cases like loading a dashboard showing all rooms this is worth it. I went with full objects in this project for that reason.

### Is DELETE idempotent?

Yes DELETE is idempotent in my implementation. Idempotent means calling the same operation multiple times has the same effect on the server as calling it once.

If you call `DELETE /api/v1/rooms/CG-104` the first time, the room gets removed and you get back a 204. If you call the exact same request again, the room is already gone so you get a 404. The response code is different but the actual state of the server is the same both times — the room does not exist. That is what idempotency means, it is about the server state not the response code.

This is different from POST which is not idempotent. If you POST the same room twice you either create a duplicate or get a conflict error.

---

## Part 3 — Questions

### What happens if a client sends the wrong Content-Type

The `POST /api/v1/sensors` method has `@Consumes(MediaType.APPLICATION_JSON)` on it. This tells JAX-RS that it will only accept requests where the Content-Type header is `application/json`.

If a client sends the request with `Content-Type: text/plain` or `application/xml`, JAX-RS will reject it before my method even runs. It returns an HTTP 415 Unsupported Media Type error automatically. This happens because JAX-RS looks for a MessageBodyReader that can convert the incoming body format into a Sensor object, and since no reader for text/plain or xml is registered it cannot do it and rejects the request.

### @QueryParam vs putting the filter in the path

I used `@QueryParam("type")` to filter sensors so the URL looks like `/api/v1/sensors?type=CO2`.

The alternative would be something like `/api/v1/sensors/type/CO2` where the filter is part of the path.

The query parameter approach is better because path parameters are meant to identify a specific resource like `/sensors/TEMP-001` which identifies one specific sensor. Putting a filter like type in the path makes it look like you are identifying a resource when really you are just filtering a list.

Also query parameters are optional by design. You can call `/api/v1/sensors` with no filter and get everything, or add `?type=CO2` to narrow it down. With path parameters you would need a completely separate endpoint for the unfiltered version which is messy.

---

## Part 4 — Questions

### Sub-resource locator pattern

In `SensorResource` I have a method called `getReadings` that has `@Path("/{sensorId}/readings")` but no HTTP method annotation like `@GET` or `@POST`. This is called a sub-resource locator. Instead of handling the request itself it returns a new instance of `SensorReadingResource` and JAX-RS then uses that class to handle whichever HTTP method was called.

The main benefit is keeping the code organised. If I put all the reading logic directly inside `SensorResource` that class would end up being massive and hard to read. By splitting it out, `SensorResource` only deals with sensors and `SensorReadingResource` only deals with readings. Each class has one job which makes it much easier to maintain.

### Historical data and the side effect on currentValue

`SensorReadingResource` has two methods. `GET /` returns the full list of past readings for that sensor. `POST /` adds a new reading to the list.

After saving the new reading I also update `sensor.setCurrentValue(reading.getValue())` on the parent sensor object. This keeps the data consistent so if someone calls `GET /api/v1/sensors/TEMP-001` they will always see the most recent reading in the `currentValue` field.

---

## Part 5 — Questions

### Why 422 and not 404 for a missing roomId

When a sensor is posted with a `roomId` that does not exist I return 422 Unprocessable Entity rather than 404.

404 means the URL the client requested was not found. But in this case the URL was fine. The problem is that the content of the request body references a room that does not exist. The request was understood perfectly it just cannot be processed because the data is invalid. 422 is specifically for this situation where the syntax is fine but the semantics are wrong.

### Why you should never send stack traces to the client

The `GlobalExceptionMapper` catches any exception that none of the other mappers handled and returns a plain 500 response without any technical detail.

Sending a raw stack trace to a client is a security risk. It reveals which Java version is running, which libraries are being used, class names and method names from inside the application. An attacker could use this to look up known vulnerabilities and craft an attack. The right approach is to log the full error on the server and send back a vague generic message to the client.

### Logging filter advantages

Using a JAX-RS filter for logging is much better than manually adding log statements to every method. You write it once and it automatically applies to every single endpoint. If I add a new resource class the logging happens automatically without touching that new class at all.

The other advantage is that the `ContainerResponseFilter` runs after the response has been built so it can log the actual final HTTP status code which you cannot always know from inside the resource method itself.

---

## Project structure

```
src/main/java/com/smartcampus/
├── application/
│   ├── DataStore.java
│   ├── Main.java
│   └── SmartCampusApplication.java
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorResource.java
│   └── SensorReadingResource.java
├── exception/
│   ├── ErrorResponse.java
│   ├── GlobalExceptionMapper.java
│   ├── LinkedResourceNotFoundException.java
│   ├── LinkedResourceNotFoundMapper.java
│   ├── RoomNotEmptyException.java
│   ├── RoomNotEmptyMapper.java
│   ├── SensorUnavailableException.java
│   └── SensorUnavailableMapper.java
└── filter/
    └── ApiLoggingFilter.java
```