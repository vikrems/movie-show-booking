package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.ticket.booking.persistence.entity.BlockedSeats;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
import static java.util.concurrent.TimeUnit.SECONDS;

@Repository
@Slf4j
public class BookingRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration EXPIRATION = Duration.ofSeconds(30);
    private final Jackson2HashMapper redisMapper = new Jackson2HashMapper(false);

    public BookingRepository(DynamoDBMapper dynamoDBMapper, RedisTemplate<String, String> redisTemplate) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.redisTemplate = redisTemplate;
    }

    public List<BookingEntity> findByShowId(String showId) {
        List<BookingEntity> bookingEntities = queryDynamoDb(showId);
        BlockedSeats blockedSeats = queryRedis(showId);
        if (blockedSeats != null)
            return mergeResults(blockedSeats.getSeats(), bookingEntities);

        return bookingEntities;
    }

    private List<BookingEntity> queryDynamoDb(String showId) {
        DynamoDBQueryExpression<BookingEntity> dynamoDBQueryExpression = new DynamoDBQueryExpression<>();
        BookingEntity bookingEntity = BookingEntity.entityWithPartitionKey(showId);
        dynamoDBQueryExpression.setHashKeyValues(bookingEntity);
        return dynamoDBMapper.query(BookingEntity.class, dynamoDBQueryExpression);
    }

    private BlockedSeats queryRedis(String showId) {
        LinkedHashMap<String, Object> linkedHashMap = (LinkedHashMap) redisTemplate.opsForHash().entries(showId);
        if (linkedHashMap.isEmpty())
            return null;

        return (BlockedSeats) redisMapper.fromHash(linkedHashMap);
    }

    private List<BookingEntity> mergeResults(List<String> listOfSeats, List<BookingEntity> bookingEntities) {
        for (BookingEntity eachBookingEntity : bookingEntities) {
            if (listOfSeats.contains(eachBookingEntity.getSortKey()))
                eachBookingEntity.setOccupancy(BLOCKED.name());
        }
        return bookingEntities;
    }

    public void block(String userId, String showId, List<String> seatNumbers) {
        BlockedSeats blockedSeats = new BlockedSeats(userId, seatNumbers);
        List<String> keys = seatNumbers.stream()
                .map(eachSeat -> showId + "_" + eachSeat)
                .collect(Collectors.toList());
        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(keys);
                operations.multi();
                for (String eachKey : keys) {
                    operations.opsForValue().setIfAbsent(eachKey, "0", EXPIRATION);
                    operations.opsForValue().increment(eachKey);
                }
                operations.opsForHash().putAll(showId, redisMapper.toHash(blockedSeats));
                operations.expire(showId, 30, SECONDS);
                // This will contain the results of all operations in the transaction
                operations.exec();
                return null;
            }
        });
    }
}
