package com.ticket.booking.exception;

import lombok.Getter;

import static com.ticket.booking.constant.Constant.NOT_FOUND_MESSAGE;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(){
        super(NOT_FOUND_MESSAGE);
    }
}
