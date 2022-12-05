package com.ticket.booking.api.service;

import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.dto.SeatDto;
import com.ticket.booking.api.dto.UserBasedAllocation;
import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.enums.Occupancy;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.exception.ConflictException;
import com.ticket.booking.exception.ResourceNotFoundException;
import com.ticket.booking.persistence.entity.BookingEntity;
import com.ticket.booking.persistence.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.ticket.booking.domain.entity.enums.Occupancy.*;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowsService {

    private final BookingRepository bookingRepository;

    public SeatAllocation listAllSeats(String showId) {
        List<BookingEntity> bookingEntities = bookingRepository.findByShowId(showId);
        if (isEmpty(bookingEntities)) {
            log.error("Show ID {} is not available", showId);
            throw new ResourceNotFoundException();
        }
        return convertToSeatAllocation(bookingEntities);
    }

    private SeatAllocation convertToSeatAllocation(List<BookingEntity> bookingEntities) {
        List<SeatDto> seatDtos = bookingEntities
                .stream()
                .map(bookingEntity -> new SeatDto(bookingEntity.getSortKey(),
                        bookingEntity.getOccupancy()))
                .collect(toList());

        return new SeatAllocation(seatDtos);
    }

    public String blockSeats(String userId, String showId, List<String> requestedSeats) {
        List<BookingEntity> bookingEntities = bookingRepository.findByShowId(showId);
        validateShowAndSeatExistence(showId, requestedSeats, bookingEntities);
        List<Seat> seatVoList = convertToSeatVo(requestedSeats, bookingEntities);
        Blocked blockedAllocation = new Blocked(showId, seatVoList, userId);
        return bookingRepository.save(blockedAllocation);
    }

    public void unblockSeats(String allocationId, String userId) {
        Allocation allocation = bookingRepository.findByAllocationId(allocationId)
                .orElseThrow(ResourceNotFoundException::new);
        Allocation reversedAllocation = allocation.reverseTransition(userId);
        bookingRepository.save(reversedAllocation);
    }

    public void book(String allocationId, String userId) {
        Allocation allocation = bookingRepository.findByAllocationId(allocationId)
                .orElseThrow(ResourceNotFoundException::new);
        Allocation forwardAllocation = allocation.forwardTransition(userId);
        bookingRepository.save(forwardAllocation);
    }


    private List<Seat> convertToSeatVo(List<String> requestedSeats, List<BookingEntity> bookingEntities) {
        Map<String, BookingEntity> idToEntity = bookingEntities.stream()
                .collect(toMap(BookingEntity::getSortKey, Function.identity()));
        return requestedSeats.stream()
                .map(idToEntity::get)
                .map(bookingEntity -> new Seat(bookingEntity.getSortKey(),
                        bookingEntity.getOccupancy(), bookingEntity.getVersion()))
                .collect(toList());
    }

    private void validateShowAndSeatExistence(String showId, List<String> requestedSeats,
                                              List<BookingEntity> bookingEntities) {
        validateShowExistence(showId, bookingEntities);
        Map<String, BookingEntity> idToEntity = bookingEntities.stream()
                .collect(toMap(BookingEntity::getSortKey, Function.identity()));
        List<String> unavailableSeats = new ArrayList<>();
        for (String eachSeat : requestedSeats) {
            validateSeatExistence(showId, idToEntity, eachSeat);
            enlistUnavailableSeats(idToEntity, unavailableSeats, eachSeat);
        }
        displayErrorForUnavailableSeats(unavailableSeats);
    }

    private void validateShowExistence(String showId, List<BookingEntity> bookingEntities) {
        if (isEmpty(bookingEntities)) {
            String error = String.format("Show Id %s not found", showId);
            log.error(error);
            throw new ResourceNotFoundException();
        }
    }

    private void validateSeatExistence(String showId, Map<String, BookingEntity> idToEntity, String eachSeat) {
        BookingEntity bookingEntity = idToEntity.get(eachSeat);
        if (isNull(bookingEntity)) {
            String error = String.format("Seat %s not found in the show with ID %s", eachSeat, showId);
            log.error(error);
            throw new ResourceNotFoundException();
        }
    }

    private void enlistUnavailableSeats(Map<String, BookingEntity> idToEntity, List<String> unavailableSeats,
                                        String eachSeat) {
        if (idToEntity.get(eachSeat).getOccupancy() != AVAILABLE)
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

    public UserBasedAllocation getUserBasedAllocation(String allocationId) {
        Allocation allocation = bookingRepository.findByAllocationId(allocationId)
                .orElseThrow(ResourceNotFoundException::new);
        Occupancy occupancy = findOccupancyBasedOnAllocation(allocation);
        List<String> seats = allocation.getSeats().stream()
                .map(Seat::getSeatNumber)
                .collect(toList());
        return new UserBasedAllocation(allocation.getUserId(), occupancy, seats);

    }

    private Occupancy findOccupancyBasedOnAllocation(Allocation allocation) {
        if (allocation instanceof Blocked)
            return BLOCKED;

        return BOOKED;
    }
}
