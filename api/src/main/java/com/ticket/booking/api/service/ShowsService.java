package com.ticket.booking.api.service;

import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.dto.SeatDto;
import com.ticket.booking.api.exception.ResourceNotFoundException;
import com.ticket.booking.domain.entity.enums.Occupancy;
import com.ticket.booking.persistence.entity.BookingEntity;
import com.ticket.booking.persistence.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());

        return new SeatAllocation(seatDtos);
    }

    public void blockSeats(String userId, String showId, List<String> seats) {
        validateShowAndSeatExistence(showId, seats);
        bookingRepository.block(userId, showId, seats);
    }

    private void validateShowAndSeatExistence(String showId, List<String> seats) {
        List<BookingEntity> bookingEntities = bookingRepository.findByShowId(showId);
        validateShowExistence(showId, bookingEntities);
        Map<String, BookingEntity> idToEntity = bookingEntities.stream()
                .collect(toMap(BookingEntity::getSortKey, Function.identity()));
        for (String eachSeat : seats) {
            validateSeatExistence(showId, idToEntity, eachSeat);
        }
    }

    private void validateSeatExistence(String showId, Map<String, BookingEntity> idToEntity, String eachSeat) {
        if (!idToEntity.containsKey(eachSeat)) {
            String error = String.format("Seat %s not found in the show with ID %s", eachSeat, showId);
            log.error(error);
            throw new ResourceNotFoundException(error);
        }
    }

    private void validateShowExistence(String showId, List<BookingEntity> bookingEntities) {
        if (isEmpty(bookingEntities)) {
            String error = String.format("Show Id %s not found", showId);
            log.error(error);
            throw new ResourceNotFoundException(error);
        }
    }
}
