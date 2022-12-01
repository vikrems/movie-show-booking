package com.ticket.booking.persistence;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.persistence.entity.BookingEntity;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
import static com.ticket.booking.domain.entity.enums.Occupancy.BOOKED;
import static com.ticket.booking.persistence.entity.BookingEntity.entityWithAllocationIdOccupancyUserIdAllocatedSeats;
import static com.ticket.booking.persistence.entity.BookingEntity.entityWithPartitionKeySortKeyOccupancyUserId;
import static java.util.stream.Collectors.toList;

@Component
public class Mapper {

    public List<BookingEntity> allocationToDynamoDbEntity(Allocation allocation) {
        List<BookingEntity> bookingEntities = createBookedEntities(allocation);
        List<String> allocatedSeats = createAllocatedSeats(allocation);
        BookingEntity allocationEntity = entityWithAllocationIdOccupancyUserIdAllocatedSeats(allocation.getAllocationId(),
                BLOCKED, allocation.getUserId(), allocatedSeats);
        bookingEntities.add(allocationEntity);

        return bookingEntities;
    }

    private List<String> createAllocatedSeats(Allocation allocation) {
        return allocation.getSeats().stream()
                .map(eachSeat -> allocation.getShowId() + "_" + eachSeat.getSeatNumber())
                .collect(toList());
    }

    private List<BookingEntity> createBookedEntities(Allocation allocation) {
        return allocation.getSeats().stream()
                .map(eachSeat -> entityWithPartitionKeySortKeyOccupancyUserId(allocation.getShowId(),
                        eachSeat.getSeatNumber(), BOOKED, allocation.getUserId()))
                .collect(toList());
    }


    public Allocation entityToAllocation(BookingEntity bookingEntity) {
        List<Seat> seats = bookingEntity.getAllocatedSeats()
                .stream()
                .map(showSeat -> showSeat.split("_")[1])
                .map(eachSeat -> new Seat(eachSeat, bookingEntity.getOccupancy()))
                .collect(toList());
        String showId = bookingEntity.getAllocatedSeats().get(0).split("_")[0];

        return new Booked(bookingEntity.getPartitionKey(), showId, seats, bookingEntity.getUserId());
    }
}
