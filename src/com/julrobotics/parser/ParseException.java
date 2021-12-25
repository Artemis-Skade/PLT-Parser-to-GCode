package com.julrobotics.parser;

// Credits: https://github.com/ralfstx/minimal-json

public class ParseException extends RuntimeException{
    private final Location location;

    ParseException(String message, Location location){
        super(message + " at " +  location);
        this.location = location;
    }

    /**
     * Returns the location at which the error occurred.
     *
     * @return the error location
     */
    public Location getLocation() {
        return location;
    }

}
