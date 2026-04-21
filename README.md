Smart Campus API - 5COSC022W Coursework

Student Number: w2113683
Module: Client-Server Architectures
Due Date: 24th April 2026


HOW TO RUN

1. Open the project in NetBeans
2. Right click the project and click Clean and Build
3. Right click the project and click Run
4. The server starts at http://localhost:8080/api/v1/

Use Postman to test the endpoints.


ENDPOINTS

GET /api/v1 - API info and links
GET /api/v1/rooms - get all rooms
POST /api/v1/rooms - create a room
GET /api/v1/rooms/roomId - get one room
DELETE /api/v1/rooms/roomId - delete a room
GET /api/v1/sensors - get all sensors, can filter with type=
POST /api/v1/sensors - register a sensor
GET /api/v1/sensors/sensorId - get one sensor
DELETE /api/v1/sensors/sensorId - delete a sensor
GET /api/v1/sensors/sensorId/readings - get reading history
POST /api/v1/sensors/sensorId/readings - add a new reading


PART 1 QUESTIONS

JAX-RS Resource Lifecycle

By default JAX-RS creates a brand new instance of a resource class every time a request comes in. So every time someone hits an endpoint a fresh object gets created and then thrown away after the response is sent. This is called request-scoped lifecycle.

The problem this causes is that you cannot store any data inside the resource class itself because it disappears after every single request. To fix this I created a separate DataStore class that only ever gets created once, which is the singleton pattern. All the resource classes call DataStore.getInstance() to get the same shared copy so the data actually persists between requests.

Thread safety was also something I had to think about. Because multiple requests can come in at the same time, two threads could try writing to the same HashMap simultaneously which can cause data loss or crashes. I made getInstance() synchronized so only one thread can create the DataStore instance at a time. I also check if a sensor reading list already exists before creating a new one so nothing gets accidentally overwritten.


HATEOAS

HATEOAS stands for Hypermedia As The Engine Of Application State. It basically means API responses include links pointing to related resources so clients can navigate the API by following those links rather than having to already know every URL. It is similar to how a website works where you follow links rather than memorising every page address.

The benefit for developers is the API becomes self documenting. If I call GET /api/v1 the response gives me links to /api/v1/rooms and /api/v1/sensors so I know exactly where to go without needing separate documentation. It also means if a URL changes on the server, clients following the links in responses automatically get the updated path rather than breaking. Static documentation goes out of date but links in responses always reflect what is actually there.


PART 2 QUESTIONS

Full Objects vs Just IDs

When returning a list of rooms there are two options. You can send back just the room IDs or send back the full room objects with all their details.

Sending only IDs keeps the response very small which is good for bandwidth. But then the client has to make a separate GET request for every single room just to get any useful information. If there are 200 rooms that ends up being 201 HTTP requests total which is really inefficient and slow.

Returning full objects means the client gets everything it needs in one go. The response payload is bigger but for a facilities dashboard that needs to show all room details at once this is much more practical. I went with full objects in this project for that reason.


Is DELETE Idempotent

Yes it is idempotent. I had to look up what idempotent actually means but it basically means calling the same operation multiple times has the same effect on the server as calling it once.

So in my implementation if you call DELETE on room CG-106 and it exists you get 204 back and the room is removed. If you send the exact same DELETE request again the room is already gone so you get 404. The response code is different but the server state is identical both times, the room simply does not exist either way. Idempotency is about the server state not the response code.

POST behaves differently and is not idempotent. Sending the same POST twice would try to create the same room twice which either creates a duplicate or returns a conflict error, so the server state is different depending on how many times you call it.


PART 3 QUESTIONS

Wrong Content-Type

The POST method for sensors has @Consumes(MediaType.APPLICATION_JSON) on it. This tells JAX-RS it will only accept requests where the Content-Type header is application/json.

If a client sends the request with Content-Type text/plain or application/xml, JAX-RS automatically rejects it with HTTP 415 Unsupported Media Type before my method even runs. This is because JAX-RS looks for a MessageBodyReader that can deserialise the incoming format into a Sensor object. Since no reader is registered for text/plain or xml it just refuses the request straight away. This is useful because bad format requests get rejected early without needing any extra validation code inside the method.


QueryParam vs Path Parameter

I used @QueryParam to filter sensors so the URL looks like /api/v1/sensors?type=CO2 rather than something like /api/v1/sensors/type/CO2 where the filter is part of the path.

Path parameters are meant to identify one specific resource. For example /sensors/TEMP-001 points to one exact sensor. Putting a filter like type into the path makes it look like you are identifying something specific when really you are just narrowing down a collection.

Query parameters are also optional by default which is exactly what you want for filtering. You can call /api/v1/sensors on its own and get everything back, or add ?type=CO2 to filter it. You can even stack multiple filters like ?type=CO2&status=ACTIVE which would get very complicated very quickly if you tried to do it with path parameters instead.


PART 4 QUESTIONS

Sub-Resource Locator

In SensorResource there is a method with @Path on sensorId/readings but no HTTP method annotation like @GET or @POST on it. This is called a sub-resource locator. Instead of handling the request itself it creates and returns a new SensorReadingResource object and JAX-RS then figures out which method on that class to call.

The reason I did it this way is that if I had put all the readings logic directly inside SensorResource that class would have become massive and really difficult to read and maintain. Splitting it into its own class means each class only has one responsibility. SensorResource handles sensor CRUD and SensorReadingResource handles reading history. If the readings logic ever needs changing I only have to look in one place. In a bigger API with lots of nested resources this approach stops you ending up with one huge controller class that nobody can follow.


Historical Data and currentValue

SensorReadingResource has a GET method that returns all past readings for a sensor and a POST method that adds a new reading to the list.

After a successful POST I also update the parent sensor by calling sensor.setCurrentValue with the new reading value. This is important for keeping the data consistent across the API. If someone calls GET on a sensor straight after a new reading is posted they will see the latest value in the currentValue field rather than a stale old number. Without this update the sensor object and the readings list would show different values which would be confusing.


PART 5 QUESTIONS

Why 422 and Not 404

404 means the URL was not found. But in this case the URL is completely valid, POST /api/v1/sensors exists and works fine. The problem is that the roomId inside the request body is referencing a room that does not exist in the system.

422 Unprocessable Entity is designed for exactly this situation where the request itself is syntactically valid JSON and the URL is correct, but the actual content of the request body cannot be processed. Using 422 gives the client a much clearer signal that they need to fix the data in their request body rather than their URL. If you returned 404 the client might think they are hitting the wrong endpoint which would be misleading.


Stack Traces

Sending a raw Java stack trace back to the client is a serious security risk. A stack trace exposes which version of Java is running, which libraries are being used and their exact versions, and internal class and method names from inside the application. An attacker could take all of that information and use it to find known CVEs and vulnerabilities in those specific library versions and then target the application.

The GlobalExceptionMapper catches any exception that none of the other mappers handled and just returns a clean generic 500 message with no technical detail. The full error still gets logged on the server side so developers can diagnose the problem, but the client never sees any of the internal information. This is the correct approach for any production API.


Logging Filter

Rather than putting log statements inside every single resource method I used a JAX-RS filter which automatically runs on every request without me having to modify any resource class. This means if a new endpoint gets added the logging just works for it automatically without having to remember to add anything.

The ContainerResponseFilter also runs after everything else has finished including any exception mappers, which means it always captures the actual final HTTP status code that gets sent to the client. If I had logged inside the resource method itself I would not always know what the final status code was going to be, especially if an exception gets thrown and caught by one of the mappers. Using a filter also keeps the logging logic completely separate from the business logic which makes the code cleaner overall.


PROJECT STRUCTURE

application
    DataStore.java
    Main.java
    SmartCampusApplication.java

model
    Room.java
    Sensor.java
    SensorReading.java

resource
    DiscoveryResource.java
    RoomResource.java
    SensorResource.java
    SensorReadingResource.java

exception
    ErrorResponse.java
    GlobalExceptionMapper.java
    LinkedResourceNotFoundException.java
    LinkedResourceNotFoundMapper.java
    RoomNotEmptyException.java
    RoomNotEmptyMapper.java
    SensorUnavailableException.java
    SensorUnavailableMapper.java

filter
    ApiLoggingFilter.java
