package com.ticket.booking.domain.entity.state;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ticket.booking.domain.entity.enums.Occupancy.*;

@Slf4j
public class Blocked extends Allocation {

    public Blocked(String showId, List<Seat> seats, String userId) {
        super(UUID.randomUUID().toString(), showId, seats, userId);
        List<Seat> unavailableSeats = new ArrayList<>();
        for (Seat eachSeat : seats) {
            enlistSeatsIfUnavailable(unavailableSeats, eachSeat);
            eachSeat.setOccupancy(BLOCKED);
        }
        displayErrorForUnavailableSeats(unavailableSeats);
    }

    private void enlistSeatsIfUnavailable(List<Seat> unavailableSeats, Seat eachSeat) {
        if (eachSeat.getOccupancy() != AVAILABLE)
            unavailableSeats.add(eachSeat);
    }

    private void displayErrorForUnavailableSeats(List<Seat> unavailableSeats) {
        if (!unavailableSeats.isEmpty()) {
            String commaSeparatedSeats = unavailableSeats
                    .stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.joining(","));
            String error = String.format("The following seats %s are not available for this show", commaSeparatedSeats);
            log.error(error);
            throw new ConflictException(error);
        }
    }

    @Override
    Allocation forwardTransition() {
        for (Seat eachSeat : seats)
            eachSeat.setOccupancy(BOOKED);

        return new Booked(allocationId, showId, seats, userId);
    }

    @Override
    Allocation reverseTransition() {
        for (Seat eachSeat : seats)
            eachSeat.setOccupancy(AVAILABLE);

        return new Available(allocationId, showId, seats);
    }
}
