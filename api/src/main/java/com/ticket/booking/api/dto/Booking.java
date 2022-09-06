package com.ticket.booking.api.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class Booking {

    private String bookingId;
    private String emailId;
    private String movieName;
    private String screenName;
    private Instant showTime;
    private List<String> seats;
}
