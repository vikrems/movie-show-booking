package com.ticket.booking.persistence.repository;

import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BookingRepository {

    private final DynamoDBRepository dynamoDBRepository;
    private final RedisRepository redisRepository;

    public List<BookingEntity> findByShowId(String showId) {
        List<BookingEntity> bookingEntities = dynamoDBRepository.findByShowId(showId);
        redisRepository.updateEntityDetailsIfBlockedByTheUser(bookingEntities);

        return bookingEntities;
    }

    public Optional<Allocation> findByAllocationId(String allocationId) {
        Allocation allocation = redisRepository.findByAllocationId(allocationId);
        if (allocation == null)
            allocation = dynamoDBRepository.findByAllocationId(allocationId);

        return Optional.ofNullable(allocation);
    }

    public String save(Allocation allocation) {
        if (allocation instanceof Blocked)
            redisRepository.save((Blocked) allocation);
        else {
            if (allocation instanceof Booked)
                dynamoDBRepository.save(allocation);
            redisRepository.delete(allocation);
        }

        return allocation.getAllocationId();
    }
}
