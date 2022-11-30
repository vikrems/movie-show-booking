package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public abstract class Allocation {

    final String allocationId;
    final String showId;
    List<Seat> seats;
    String userId;

    public abstract Allocation forwardTransition(String userId);

    public abstract Allocation reverseTransition(String userId);
}
