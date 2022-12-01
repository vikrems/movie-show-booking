package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.persistence.Mapper;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.util.Objects.isNull;

@Repository
@RequiredArgsConstructor
public class DynamoDBRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final Mapper mapper;

    void save(Allocation allocation) {
        List<BookingEntity> bookingEntities = mapper.allocationToDynamoDbEntity(allocation);
        for(BookingEntity eachEntity : bookingEntities)
            dynamoDBMapper.save(eachEntity);
    }

    List<BookingEntity> findByShowId(String showId) {
        DynamoDBQueryExpression<BookingEntity> dynamoDBQueryExpression = new DynamoDBQueryExpression<>();
        BookingEntity bookingEntity = BookingEntity.entityWithPartitionKey(showId);
        dynamoDBQueryExpression.setHashKeyValues(bookingEntity);
        return dynamoDBMapper.query(BookingEntity.class, dynamoDBQueryExpression);
    }

    Allocation findByAllocationId(String allocationId) {
        BookingEntity bookingEntity = dynamoDBMapper.load(BookingEntity.class, allocationId, allocationId);
        if(isNull(bookingEntity))
            return null;

        return mapper.entityToAllocation(bookingEntity);
    }
}
