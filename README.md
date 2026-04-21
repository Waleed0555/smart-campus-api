es DELETE is idempotent in my implementation. Idempotent basically means you can call the same operation as many times as you want and the end result on the server is always the same.

So if you call DELETE on a room that exists you get 204 back and the room is gone. If you then call the exact same DELETE again the room is already gone so you get 404 back. The response code is different but the actual state of the server is identical both times, the room simply does not exist. That is what idempotent means, it is about what state the server ends up in not about what response code comes back.

POST works differently and is not idempotent. Sending the same POST twice would try to create the same room twice which either creates a duplicate or throws a conflict error.

PART 3 QUESTIONS

Wrong Content-Type

The POST method for sensors has @Consumes(MediaType.APPLICATION_JSON) on it. This tells JAX-RS that it will only accept requests where the Content type header says application/json.

If a client sends the request with text/plain or application/xml instead, JAX-RS will just reject it automatically with a 415 Unsupported Media Type error before my method even gets a chance to run. This happens because JAX-RS looks for something called a MessageBodyReader that knows how to turn the incoming format into a Sensor object. Since nothing is registered for text/plain or xml it just refuses the request straight away. I found this really useful because it means you do not have to write any extra validation code yourself.

QueryParam vs Path Parameter

I used @QueryParam to filter sensors so the URL ends up looking like /api/v1/sensors?type=CO2 rather than something like apiv1sensorstypeCO2 where the filter is baked into the path.

The reason path parameters are not great for this is that they are really meant for identifying one specific resource. Like /sensors/TEMP-001 points to one exact sensor. Putting a filter like type into the path makes it look like you are pointing at something specific when really you are just trying to narrow down a list.

Query parameters are also optional by default which is exactly what you want for filtering. You can call /api/v1/sensors on its own and get everything back, or add ?type=CO2 to narrow it down. You can even combine multiple filters like ?type=CO2&status=ACTIVE which would get very messy very quickly if you tried to do it with path parameters.

PART 4 QUESTIONS

Sub Resource Locator

In SensorResource there is a method that has @Path with sensorId/readings on it but no HTTP method annotation like @GET or @POST. This is called a sub resource locator. Rather than handling the request itself it just creates and returns a new SensorReadingResource object and then JAX-RS takes over and figures out which method on that class to actually call.

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

application DataStore.java Main.java SmartCampusApplication.java

model Room.java Sensor.java SensorReading.java

resource DiscoveryResource.java RoomResource.java SensorResource.java SensorReadingResource.java

exception ErrorResponse.java GlobalExceptionMapper.java LinkedResourceNotFoundException.java LinkedResourceNotFoundMapper.java RoomNotEmptyException.java RoomNotEmptyMapper.java SensorUnavailableException.java SensorUnavailableMapper.java

filter ApiLoggingFilter.java
