package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.exception.ConflictException;

import java.util.List;

import static com.ticket.booking.constant.Constant.SEATS_NOT_AVAILABLE;

public class Booked extends Allocation {

    public Booked(String allocationId, String showId, List<Seat> seats, String userId) {
        super(allocationId, showId, seats, userId);
    }

    @Override
    public Allocation forwardTransition(String userId) {
        throw new ConflictException(SEATS_NOT_AVAILABLE);
    }

    @Override
    public Allocation reverseTransition(String userId) {
        throw new ConflictException(SEATS_NOT_AVAILABLE);
    }
}
