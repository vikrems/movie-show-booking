package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.exception.ConflictException;

import java.util.List;

public class Booked extends Allocation {

    public Booked(String allocationId, String showId, List<Seat> seats, String userId) {
        super(allocationId, showId, seats, userId);
    }

    @Override
    public Allocation forwardTransition(String userId) {
        throw new ConflictException("The selected seats are not available");
    }

    @Override
    public Allocation reverseTransition(String userId) {
        throw new ConflictException("The selected seats are not available");
    }
}
