package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;

import java.util.List;

public class Booked extends Allocation {

    public Booked(String showId, String allocationId, List<Seat> seats, String userId) {
        super(allocationId, showId, seats, userId);
    }

    @Override
    Allocation forwardTransition() {
        return null;
    }

    @Override
    Allocation reverseTransition() {
        return null;
    }
}
