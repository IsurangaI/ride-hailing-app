package com.ridehailing.matchingservice.exception;

public class NoDriversAvailableException extends RuntimeException{
    public NoDriversAvailableException(){
        super("Cannot find a driver.");
    }
}
