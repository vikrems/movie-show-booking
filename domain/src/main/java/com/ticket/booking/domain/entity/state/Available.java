package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;

import java.util.List;

public class Available extends Allocation {

    public Available(String allocationId, String showId, List<Seat> seats, String userId) {
        super(allocationId, showId, seats, userId);
    }

    @Override
    public Allocation forwardTransition(String userId) {
        throw new IllegalStateException("Cannot forwardTransition from the 'Available' state");
    }

    @Override
    public Allocation reverseTransition(String userId) {
        throw new IllegalStateException("Cannot reverseTransition from the 'Available' state");
    }
}
