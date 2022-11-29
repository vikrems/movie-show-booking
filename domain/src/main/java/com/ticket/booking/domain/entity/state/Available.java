package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;

import java.util.List;

public class Available extends Allocation {

    public Available(String showId, String allocationId, List<Seat> seats) {
        super(allocationId, showId, seats, null);
    }

    @Override
    Allocation forwardTransition() {

        return null;
    }

    @Override
    Allocation reverseTransition() {
        throw new IllegalStateException("Cannot reverseTransition from the 'Available' state");
    }
}
