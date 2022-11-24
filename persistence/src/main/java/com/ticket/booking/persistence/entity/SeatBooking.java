package com.ticket.booking.persistence.entity;

import lombok.Getter;

import java.util.*;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Getter
public class SeatBooking {

    private final Map<String, List<String>> userIdToSeats = new LinkedHashMap<>();

    public void addToBooking(String userId, List<String> seats) {
        List<String> existingSeats = userIdToSeats.get(userId);
        if (isNull(existingSeats)) {
            existingSeats = new ArrayList<>();
            userIdToSeats.put(userId, existingSeats);
        }
        existingSeats.addAll(seats);
    }

    public List<String> extractSeats() {
        return userIdToSeats
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(toList());
    }
}
