package com.mycomp.resource;

import com.mycomp.exception.RoomNotEmptyException;
import com.mycomp.model.Room;
import com.mycomp.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        return Response.ok(store.getRooms().values()).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Room ID is required.");
            return Response.status(400).entity(err).build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Conflict");
            err.put("message", "A room with ID '" + room.getId() + "' already exists.");
            return Response.status(409).entity(err).build();
        }
        store.getRooms().put(room.getId(), room);
        return Response.status(201).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Room '" + roomId + "' does not exist.");
            return Response.status(404).entity(err).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Room '" + roomId + "' does not exist.");
            return Response.status(404).entity(err).build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        store.getRooms().remove(roomId);
        Map<String, String> msg = new HashMap<>();
        msg.put("message", "Room '" + roomId + "' deleted successfully.");
        return Response.ok(msg).build();
    }
}