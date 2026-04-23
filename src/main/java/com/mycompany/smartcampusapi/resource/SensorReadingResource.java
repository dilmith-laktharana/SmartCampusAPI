package com.mycomp.resource;

import com.mycomp.exception.SensorUnavailableException;
import com.mycomp.model.SensorReading;
import com.mycomp.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        return Response.ok(store.getReadingsForSensor(sensorId)).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        var sensor = store.getSensors().get(sensorId);

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        SensorReading newReading = new SensorReading(reading.getValue());
        store.getReadingsForSensor(sensorId).add(newReading);

        
        sensor.setCurrentValue(reading.getValue());

        return Response.status(201).entity(newReading).build();
    }
}