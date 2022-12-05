package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.persistence.Mapper;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.ticket.booking.domain.entity.enums.Occupancy.BOOKED;
import static com.ticket.booking.persistence.entity.BookingEntity.entityWithAllocationIdOccupancyUserIdAllocatedSeats;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamoDBRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final Mapper mapper;

    void saveAllocation(Allocation existingAllocation, Allocation allocation) {
        List<BookingEntity> existingEntities = mapper.allocationToDynamoDbEntity(existingAllocation);
        updateUserIdAndMarkAsBooked(allocation, existingEntities);
        BookingEntity allocationEntity = createAllocationEntity(allocation);
        List<BookingEntity> allEntities = makeASingleList(existingEntities, allocationEntity);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        for (BookingEntity eachEntity : allEntities)
            transactionWriteRequest.addUpdate(eachEntity);
        dynamoDBMapper.transactionWrite(transactionWriteRequest);
    }

    private List<BookingEntity> makeASingleList(List<BookingEntity> bookingEntities, BookingEntity allocationEntity) {
        List<BookingEntity> allEntities = new ArrayList<>(bookingEntities);
        allEntities.add(allocationEntity);
        return allEntities;
    }

    private BookingEntity createAllocationEntity(Allocation allocation) {
        List<String> allocatedSeats = createAllocatedSeats(allocation);
        return entityWithAllocationIdOccupancyUserIdAllocatedSeats(allocation.getAllocationId(),
                BOOKED, allocation.getUserId(), allocatedSeats);
    }

    private void updateUserIdAndMarkAsBooked(Allocation allocation, List<BookingEntity> bookingEntities) {
        for (BookingEntity eachEntity : bookingEntities)
            eachEntity.updateDetails(allocation.getUserId(), BOOKED);
    }

    private List<String> createAllocatedSeats(Allocation allocation) {
        return allocation.getSeats().stream()
                .map(eachSeat -> allocation.getShowId() + "_" + eachSeat.getSeatNumber())
                .collect(toList());
    }

    List<BookingEntity> findByShowId(String showId) {
        DynamoDBQueryExpression<BookingEntity> dynamoDBQueryExpression = new DynamoDBQueryExpression<>();
        BookingEntity bookingEntity = BookingEntity.entityWithPartitionKey(showId);
        dynamoDBQueryExpression.setHashKeyValues(bookingEntity);
        return dynamoDBMapper.query(BookingEntity.class, dynamoDBQueryExpression);
    }

    Allocation findByAllocationId(String allocationId) {
        BookingEntity bookingEntity = dynamoDBMapper.load(BookingEntity.class, allocationId, allocationId);
        if (isNull(bookingEntity))
            return null;

        return mapper.entityToAllocation(bookingEntity);
    }
}
