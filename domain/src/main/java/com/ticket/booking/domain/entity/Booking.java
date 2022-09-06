package com.ticket.booking.domain.entity;

import com.ticket.booking.domain.entity.enums.Occupancy;

import java.util.LinkedList;
import java.util.List;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
import static com.ticket.booking.domain.entity.enums.Occupancy.BOOKED;
import static java.util.UUID.randomUUID;

public class Booking {

    private static final int MAX_SEAT_LIMIT = 20;
    private final String bookingId;
    private final List<Seat> seats = new LinkedList<>();
    private Occupancy occupancy;

    public Booking() {
        this.bookingId = randomUUID().toString();
        this.occupancy = BLOCKED;
    }

    public void addToAllocations(Seat seat) {
        if (seat.isAvailable() && seats.size() < MAX_SEAT_LIMIT) {
            seat.transition();
            seats.add(seat);
        }
        throw new IllegalStateException("Max limit exceeded for the bookings");
    }

    public void removeAllocation(Seat seat) {
        seat.release();
        seats.remove(seat);
    }

    public void confirmBooking() {
        for (Seat eachSeat : seats) {
            eachSeat.transition();
        }
        occupancy = BOOKED;
    }
}
