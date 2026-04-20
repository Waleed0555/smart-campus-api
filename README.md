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

By default JAX-RS creates a brand new instance of a resource class every time a request comes in. So every time someone calls an endpoint a fresh object gets created and then thrown away once the response is sent back.

This caused a problem because you cannot store any data inside the resource class itself since it disappears after every request. To get around this I made a separate DataStore class that only ever gets created once. All the resource classes call DataStore.getInstance() to get the same shared copy so the data actually stays between requests.

I also had to think about what happens when two requests come in at the same time. Two threads writing to the same HashMap at the same time can cause all sorts of bugs. I made getInstance() synchronized so only one thread can create the DataStore at a time and I also check if a reading list already exists before creating a new one so nothing gets overwritten by accident.

HATEOAS

HATEOAS means the API responses include links to other parts of the API. So instead of a client needing to already know every URL they can just follow the links that come back in the response. I think of it a bit like browsing a website where you follow links rather than having to type in every address manually.

The main benefit is the API becomes a lot easier to use. If a URL ever changes on the server the client just follows the new link instead of breaking. Documentation can go out of date pretty quickly but links in the actual responses always show what is really there.


PART 2 QUESTIONS

Full Objects vs Just IDs

When returning a list of rooms you basically have two choices. You can send back just the room IDs or you can send back the full room objects with all the details in them.

Sending only IDs keeps the response nice and small which saves bandwidth. But then the client has to make another separate request for every single room just to get any useful information out of it. If there are 200 rooms that ends up being 201 requests total which is really inefficient.

Returning the full objects means the client gets everything it needs in one go. The response is bigger but for something like a dashboard that needs to display all the room details at once it makes a lot more sense. I went with full objects for this reason.

Is DELETE Idempotent

Yes DELETE is idempotent in my implementation. Idempotent basically means you can call the same operation as many times as you want and the end result on the server is always the same.

So if you call DELETE on a room that exists you get 204 back and the room is gone. If you then call the exact same DELETE again the room is already gone so you get 404 back. The response code is different but the actual state of the server is identical both times, the room simply does not exist. That is what idempotent means, it is about what state the server ends up in not about what response code comes back.

POST works differently and is not idempotent. Sending the same POST twice would try to create the same room twice which either creates a duplicate or throws a conflict error.


PART 3 QUESTIONS

Wrong Content-Type

The POST method for sensors has @Consumes(MediaType.APPLICATION_JSON) on it. This tells JAX-RS that it will only accept requests where the Content-Type header says application/json.

If a client sends the request with text/plain or application/xml instead, JAX-RS will just reject it automatically with a 415 Unsupported Media Type error before my method even gets a chance to run. This happens because JAX-RS looks for something called a MessageBodyReader that knows how to turn the incoming format into a Sensor object. Since nothing is registered for text/plain or xml it just refuses the request straight away. I found this really useful because it means you do not have to write any extra validation code yourself.

QueryParam vs Path Parameter

I used @QueryParam to filter sensors so the URL ends up looking like /api/v1/sensors?type=CO2 rather than something like /api/v1/sensors/type/CO2 where the filter is baked into the path.

The reason path parameters are not great for this is that they are really meant for identifying one specific resource. Like /sensors/TEMP-001 points to one exact sensor. Putting a filter like type into the path makes it look like you are pointing at something specific when really you are just trying to narrow down a list.

Query parameters are also optional by default which is exactly what you want for filtering. You can call /api/v1/sensors on its own and get everything back, or add ?type=CO2 to narrow it down. You can even combine multiple filters like ?type=CO2&status=ACTIVE which would get very messy very quickly if you tried to do it with path parameters.


PART 4 QUESTIONS

Sub-Resource Locator

In SensorResource there is a method that has @Path with sensorId/readings on it but no HTTP method annotation like @GET or @POST. This is called a sub-resource locator. Rather than handling the request itself it just creates and returns a new SensorReadingResource object and then JAX-RS takes over and figures out which method on that class to actually call.

I did it this way because if I had put all the readings logic directly inside SensorResource that class would have ended up massive and really hard to read through. By splitting it into its own separate class each class only has to worry about one thing. SensorResource deals with sensors and SensorReadingResource deals with readings. If anything about the readings logic needs to change later I only have to look in one place which makes it a lot easier to maintain.

Historical Data and currentValue

SensorReadingResource has a GET method that returns all the past readings stored for a sensor and a POST method that adds a new reading to that list.

The important thing about the POST is that after saving the new reading I also update the parent sensor by calling sensor.setCurrentValue with the new reading value. This keeps everything consistent. If someone calls GET on a sensor straight after a new reading has been posted they will see the latest value straight away rather than whatever stale number was there before.


PART 5 QUESTIONS

Why 422 and Not 404

404 is for when the URL itself was not found. But in this case the URL was completely fine, POST /api/v1/sensors is a valid endpoint that exists. The actual problem is that the roomId inside the request body is pointing to a room that does not exist in the system.

422 Unprocessable Entity is specifically designed for situations where the request is valid JSON and the URL is correct but the actual data inside the request body cannot be processed for some reason. It gives the client a much clearer signal that the problem is with their data not with their URL.

Stack Traces

Sending a raw Java stack trace back to the client is a security risk and also just looks really unprofessional. A stack trace exposes which version of Java is running, which libraries are being used and their exact versions, and even internal class and method names from inside the application. An attacker could take all of that and use it to look up known vulnerabilities in those specific versions.

The GlobalExceptionMapper catches anything that none of the other mappers handled and just sends back a plain generic 500 message. The full error still gets logged on the server so developers can see exactly what went wrong but the client never gets any of that technical detail.

Logging Filter

Rather than adding log statements inside every single resource method I used a JAX-RS filter which automatically runs on every request without me having to touch any of the resource classes. If I add a completely new endpoint tomorrow the logging just works for it automatically.

The ContainerResponseFilter part also runs after everything else has finished including any exception mappers, which means it always captures the actual final HTTP status code that gets sent back. If I had logged inside the resource method itself I would not always know what the final status code was going to be, especially if an exception gets thrown somewhere and caught by a mapper.


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
