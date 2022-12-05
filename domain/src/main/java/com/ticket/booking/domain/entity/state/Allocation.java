package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Allocation {

    final String allocationId;
    final String showId;
    final List<Seat> seats;
    final String userId;

    public Allocation(String allocationId, String showId, List<Seat> seats, String userId) {
        this.allocationId = allocationId;
        this.showId = showId;
        this.seats = seats;
        this.userId = userId;
    }

    public abstract Allocation forwardTransition(String userId);

    public abstract Allocation reverseTransition(String userId);
}
