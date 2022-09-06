package com.ticket.booking.domain.entity;

import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.ticket.booking.domain.entity.enums.Occupancy.*;

@Getter
@RequiredArgsConstructor
public class Seat {

    private final String seatNumber;
    private Occupancy occupancy = AVAILABLE;

    void transition() {
        if(occupancy == AVAILABLE)
            occupancy = BLOCKED;
        else if(occupancy == BLOCKED)
            occupancy = BOOKED;
    }

    void release() {
        occupancy = AVAILABLE;
    }

    boolean isAvailable() {
        return occupancy == AVAILABLE;
    }
}
