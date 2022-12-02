package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.enums.Occupancy;
import com.ticket.booking.exception.AuthorizationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import static com.ticket.booking.constant.Constant.NOT_AUTHORIZED_MESSAGE;
import static com.ticket.booking.domain.entity.enums.Occupancy.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
public class Blocked extends Allocation {

    public Blocked(String showId, List<Seat> seats, String userId) {
        super(UUID.randomUUID().toString(), showId, seats, userId);
        for (Seat eachSeat : seats)
            eachSeat.setOccupancy(BLOCKED);
    }

    public Blocked(String allocationId, String showId, List<Seat> seats, String userId) {
        super(allocationId, showId, seats, userId);
    }

    @Override
    public Allocation forwardTransition(String userId) {
        authorizationCheck(userId);
        changeSeatStatus(BOOKED);

        return new Booked(allocationId, showId, seats, userId);
    }

    @Override
    public Allocation reverseTransition(String userId) {
        authorizationCheck(userId);
        changeSeatStatus(AVAILABLE);

        return new Available(allocationId, showId, seats, userId);
    }

    private void changeSeatStatus(Occupancy available) {
        for (Seat eachSeat : seats)
            eachSeat.setOccupancy(available);
    }

    private void authorizationCheck(String userId) {
        if (!this.userId.equals(userId))
            throw new AuthorizationException(NOT_AUTHORIZED_MESSAGE, FORBIDDEN);
    }
}
