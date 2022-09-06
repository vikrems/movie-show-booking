package com.ticket.booking.api.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class CardDetails {

    private String ccNumber;
    private Instant expiry;
    private String cvv;
}
