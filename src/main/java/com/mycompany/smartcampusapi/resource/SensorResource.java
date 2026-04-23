package com.mycomp.resource;

import com.mycomp.exception.LinkedResourceNotFoundException;
import com.mycomp.model.Sensor;
import com.mycomp.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = store.getSensors().values().stream()
                .filter(s -> type == null || type.isEmpty() 
                          || s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (!store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room with ID '" + sensor.getRoomId() + "' does not exist. " +
                    "Please create the room first.");
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Conflict");
            err.put("message", "Sensor '" + sensor.getId() + "' already exists.");
            return Response.status(409).entity(err).build();
        }
        store.getSensors().put(sensor.getId(), sensor);
        // Link sensor to its room
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response.status(201).entity(sensor).build();
    }

    // Sub-resource locator
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(
            @PathParam("sensorId") String sensorId) {
        if (!store.getSensors().containsKey(sensorId)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Sensor '" + sensorId + "' does not exist.");
            throw new NotFoundException("Sensor not found: " + sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}