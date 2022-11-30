package com.ticket.booking.persistence.repository;

import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.persistence.Mapper;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
@Slf4j
public class BookingRepository {

    private final DynamoDBRepository dynamoDBRepository;
    private final RedisRepository redisRepository;
    private final Mapper mapper;
    private AtomicBoolean flag = new AtomicBoolean(false);

    public BookingRepository(DynamoDBRepository dynamoDBRepository, RedisRepository redisRepository,
                             Mapper mapper) {
        this.dynamoDBRepository = dynamoDBRepository;
        this.redisRepository = redisRepository;
        this.mapper = mapper;
    }

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
        else if (allocation instanceof Booked)
            dynamoDBRepository.save(allocation);
        else
            redisRepository.delete(allocation);

        return allocation.getAllocationId();
    }

//    public void book(String userId, String showId, List<String> seats, List<BookingEntity> bookingEntities) {
//        unblock("", showId, seats);
//        List<BookingEntity> updatedEntities = bookingEntities
//                .stream()
//                .filter(eachEntity -> seats.contains(eachEntity.getSortKey()))
//                .map(eachEntity -> {
//                    eachEntity.setUserId(userId);
//                    eachEntity.setOccupancy(BOOKED.name());
//                    return eachEntity;
//                })
//                .collect(toList());
//        dynamoDBMapper.batchSave(updatedEntities);
//    }
}
