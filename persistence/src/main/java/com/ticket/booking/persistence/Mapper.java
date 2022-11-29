package com.ticket.booking.persistence;

import com.ticket.booking.domain.entity.enums.Occupancy;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.persistence.entity.BookingEntity;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ticket.booking.domain.entity.enums.Occupancy.*;
import static com.ticket.booking.persistence.entity.BookingEntity.entityWithPartitionKeySortKeyOccupancyUserId;
import static java.util.stream.Collectors.toList;

@Component
public class Mapper {

    public List<BookingEntity> allocationToDynamoDbEntity(Allocation allocation) {
        Occupancy occupancy = fetchOccupancy(allocation);
        return allocation.getSeats().stream()
                .map(eachSeat -> entityWithPartitionKeySortKeyOccupancyUserId(allocation.getShowId(),
                        eachSeat.getSeatNumber(), occupancy.name(), allocation.getUserId()))
                .collect(toList());

    }

    private Occupancy fetchOccupancy(Allocation allocation) {
        if (allocation instanceof Booked)
            return BOOKED;
        if (allocation instanceof Blocked)
            return BLOCKED;

        return AVAILABLE;
    }
}
