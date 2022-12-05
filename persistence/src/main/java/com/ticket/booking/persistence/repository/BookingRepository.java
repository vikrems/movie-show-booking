package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.exception.ConflictException;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.ticket.booking.constant.Constant.SEATS_NOT_AVAILABLE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BookingRepository {

    private final DynamoDBRepository dynamoDBRepository;
    private final RedisRepository redisRepository;

    public List<BookingEntity> findByShowId(String showId) {
        return dynamoDBRepository.findByShowId(showId);
    }

    public Optional<Allocation> findByAllocationId(String allocationId) {
        Allocation allocation = redisRepository.findByAllocationId(allocationId);
        if (allocation == null)
            allocation = dynamoDBRepository.findByAllocationId(allocationId);

        return Optional.ofNullable(allocation);
    }

    public String save(Allocation allocation) {
        if (allocation instanceof Blocked)
            redisRepository.saveAllocation((Blocked) allocation);
        else {
            if (allocation instanceof Booked) {
                Allocation existingAllocation = redisRepository.findByAllocationId(allocation.getAllocationId());
                saveToDynamoDb(allocation, existingAllocation);
            }
            redisRepository.delete(allocation);
        }

        return allocation.getAllocationId();
    }

    private void saveToDynamoDb(Allocation allocation, Allocation existingAllocation) {
        try {
            dynamoDBRepository.saveAllocation(existingAllocation, allocation);
        } catch (TransactionCanceledException | ConditionalCheckFailedException ex) {
            log.error("Error saving to DynamodDB", ex);
            redisRepository.delete(allocation);
            throw new ConflictException(SEATS_NOT_AVAILABLE);
        }
    }
}
