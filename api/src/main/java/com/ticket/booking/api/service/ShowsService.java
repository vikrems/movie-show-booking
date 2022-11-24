package com.ticket.booking.api.service;

import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.dto.SeatDto;
import com.ticket.booking.exception.ConflictException;
import com.ticket.booking.exception.ResourceNotFoundException;
import com.ticket.booking.domain.entity.enums.Occupancy;
import com.ticket.booking.persistence.entity.BookingEntity;
import com.ticket.booking.persistence.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.ticket.booking.domain.entity.enums.Occupancy.AVAILABLE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowsService {

    private final BookingRepository bookingRepository;

    public SeatAllocation getSeatAllocation(String showId) {
        List<BookingEntity> bookingEntities = bookingRepository.findByShowId(showId);
        if (isEmpty(bookingEntities)) {
            log.error("Show ID {} is not available", showId);
            throw new ResourceNotFoundException("Requested Resource is not available");
        }

        return convertToSeatAllocation(bookingEntities);
    }

    private SeatAllocation convertToSeatAllocation(List<BookingEntity> bookingEntities) {
        List<SeatDto> seatDtos = bookingEntities
                .stream()
                .map(bookingEntity -> new SeatDto(bookingEntity.getSortKey(),
                        SeatDto.Category.valueOf(bookingEntity.getCategory()),
                        Occupancy.valueOf(bookingEntity.getOccupancy())))
                .collect(toList());

        return new SeatAllocation(seatDtos);
    }

    public void blockSeats(String userId, String showId, List<String> seats) {
        validateShowAndSeatExistence(showId, seats);
        if (!bookingRepository.block(userId, showId, seats))
            throw new ConflictException("One or more seats are not available");
    }

    private void validateShowAndSeatExistence(String showId, List<String> seats) {
        List<BookingEntity> bookingEntities = bookingRepository.findByShowId(showId);
        validateShowExistence(showId, bookingEntities);
        Map<String, BookingEntity> idToEntity = bookingEntities.stream()
                .collect(toMap(BookingEntity::getSortKey, Function.identity()));
        List<String> unavailableSeats = new ArrayList<>();
        for (String eachSeat : seats) {
            validateSeatExistence(showId, idToEntity, eachSeat);
            enlistUnavailableSeats(idToEntity, unavailableSeats, eachSeat);
        }
        displayErrorForUnavailableSeats(unavailableSeats);
    }

    private void validateShowExistence(String showId, List<BookingEntity> bookingEntities) {
        if (isEmpty(bookingEntities)) {
            String error = String.format("Show Id %s not found", showId);
            log.error(error);
            throw new ResourceNotFoundException(error);
        }
    }

    private void validateSeatExistence(String showId, Map<String, BookingEntity> idToEntity, String eachSeat) {
        BookingEntity bookingEntity = idToEntity.get(eachSeat);
        if (bookingEntity == null) {
            String error = String.format("Seat %s not found in the show with ID %s", eachSeat, showId);
            log.error(error);
            throw new ResourceNotFoundException(error);
        }
    }

    private void enlistUnavailableSeats(Map<String, BookingEntity> idToEntity, List<String> unavailableSeats,
                                        String eachSeat) {
        if (Occupancy.valueOf(idToEntity.get(eachSeat).getOccupancy()) != AVAILABLE)
            unavailableSeats.add(eachSeat);
    }

    private void displayErrorForUnavailableSeats(List<String> unavailableSeats) {
        if (!unavailableSeats.isEmpty()) {
            String commaSeparatedSeats = String.join(",", unavailableSeats);
            String error = String.format("The following seats %s are not available for this show", commaSeparatedSeats);
            log.error(error);
            throw new ConflictException(error);
        }
    }
}
