package com.mycomp.exception;

public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' still has sensors assigned. Remove all sensors first.");
    }
}