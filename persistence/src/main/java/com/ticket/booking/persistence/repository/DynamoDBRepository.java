package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DynamoDBRepository {

    private final DynamoDBMapper dynamoDBMapper;

    <T> void save(T allocation) {
        dynamoDBMapper.save(allocation);
    }

    List<BookingEntity> findByShowId(String showId) {
        DynamoDBQueryExpression<BookingEntity> dynamoDBQueryExpression = new DynamoDBQueryExpression<>();
        BookingEntity bookingEntity = BookingEntity.entityWithPartitionKey(showId);
        dynamoDBQueryExpression.setHashKeyValues(bookingEntity);
        return dynamoDBMapper.query(BookingEntity.class, dynamoDBQueryExpression);
    }

    Allocation findByAllocationId(String allocationId) {

        return null;
    }
}
