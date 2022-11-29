package com.ticket.booking.domain.entity;

import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Seat {

    private final String seatNumber;

    @Setter
    private Occupancy occupancy;

}
