package com.ticket.booking.persistence.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.domain.entity.state.Booked;
import com.ticket.booking.exception.ConflictException;
import com.ticket.booking.persistence.Mapper;
import com.ticket.booking.persistence.entity.BookingEntity;
import com.ticket.booking.persistence.entity.SeatBooking;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
import static com.ticket.booking.domain.entity.enums.Occupancy.BOOKED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Repository
@Slf4j
public class BookingRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final Mapper mapper;
    private static final Duration EXPIRATION = Duration.ofSeconds(300);
    private final Jackson2HashMapper redisMapper = new Jackson2HashMapper(false);
    private AtomicBoolean flag = new AtomicBoolean(false);

    public BookingRepository(DynamoDBMapper dynamoDBMapper, RedisTemplate<String, String> redisTemplate,
                             Mapper mapper) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    //TODO Remove
    public List<BookingEntity> findByShowId(String showId, String userId) {
        List<BookingEntity> bookingEntities = queryDynamoDb(showId);
        SeatBooking seatBooking = queryRedis(showId);
        if (seatBooking != null)
            return mergeResults(seatBooking.extractSeats(), bookingEntities, userId);

        return bookingEntities;
    }

    private List<BookingEntity> queryDynamoDb(String showId) {
        DynamoDBQueryExpression<BookingEntity> dynamoDBQueryExpression = new DynamoDBQueryExpression<>();
        BookingEntity bookingEntity = BookingEntity.entityWithPartitionKey(showId);
        dynamoDBQueryExpression.setHashKeyValues(bookingEntity);
        return dynamoDBMapper.query(BookingEntity.class, dynamoDBQueryExpression);
    }

    private SeatBooking queryRedis(String showId) {
        AbstractMap<String, Object> map = (AbstractMap) redisTemplate.opsForHash().entries(showId);
        if (map.isEmpty())
            return null;

        return (SeatBooking) redisMapper.fromHash(map);
    }

    private List<BookingEntity> mergeResults(List<String> listOfSeats, List<BookingEntity> bookingEntities,
                                             String userId) {
        for (BookingEntity eachBookingEntity : bookingEntities) {
            if (listOfSeats.contains(eachBookingEntity.getSortKey())) {
                eachBookingEntity.setOccupancy(BLOCKED.name());
                eachBookingEntity.setUserId(userId);
            }
        }
        return bookingEntities;
    }

    public boolean block(String userId, String showId, List<String> seatNumbers) {
        List<String> keys = extractRedisKeys(showId, seatNumbers);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @SneakyThrows
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                SeatBooking seatBooking = queryRedis(showId);
                if (seatBooking == null)
                    seatBooking = new SeatBooking();

                seatBooking.addToBooking(userId, seatNumbers);
                operations.watch(keys);
                operations.multi();
                for (String eachKey : keys) {
                    operations.opsForValue().setIfAbsent(eachKey, "0", EXPIRATION); //We insert this specifically to watch the keys to ensure that they are not modified
                    operations.opsForValue().increment(eachKey);
                    String value = (String) operations.opsForValue().get(eachKey);
                    if (nonNull(value))
                        throw new ConflictException("One or more seats are not available");
                }
                operations.opsForHash().putAll(showId, redisMapper.toHash(seatBooking));
                operations.expire(showId, EXPIRATION);
//                if (flag.compareAndSet(false, true)) {
//                    System.out.println("Locking");
//                    Thread.sleep(5000);
//                }
                // This will contain the results of all operations in the transaction
                return operations.exec();
            }
        });
        return !isEmpty(txResults);
    }

    private List<String> extractRedisKeys(String showId, List<String> seatNumbers) {
        return seatNumbers.stream()
                .map(eachSeat -> showId + "_" + eachSeat)
                .collect(toList());
    }

    public void unblock(String showId, List<String> seatNumbers) {
        List<String> keys = extractRedisKeys(showId, seatNumbers);
        redisTemplate.delete(showId);
        redisTemplate.delete(keys);
    }

    public void book(String userId, String showId, List<String> seats, List<BookingEntity> bookingEntities) {
        unblock(showId, seats);
        List<BookingEntity> updatedEntities = bookingEntities
                .stream()
                .filter(eachEntity -> seats.contains(eachEntity.getSortKey()))
                .map(eachEntity -> {
                    eachEntity.setUserId(userId);
                    eachEntity.setOccupancy(BOOKED.name());
                    return eachEntity;
                })
                .collect(toList());
        dynamoDBMapper.batchSave(updatedEntities);
    }

    public String save(Allocation allocation) {
        if (allocation instanceof Blocked)
            addToRedis((Blocked) allocation);
        else if (allocation instanceof Booked)
            dynamoDBMapper.save(allocation);
        else
            //unblock
            ;
        return allocation.getAllocationId();
    }

    public boolean addToRedis(Blocked blockedAllocation) {
        String showId = blockedAllocation.getShowId();
        List<String> seatNumbers = extractSeatNumbers(blockedAllocation.getSeats());
        List<String> keys = extractRedisKeys(showId, seatNumbers);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @SneakyThrows
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                SeatBooking seatBooking = queryRedis(showId);
                if (seatBooking == null)
                    seatBooking = new SeatBooking();

                seatBooking.addToBooking(blockedAllocation.getUserId(), seatNumbers);
                operations.watch(keys);
                operations.multi();
                for (String eachKey : keys) {
                    operations.opsForValue().setIfAbsent(eachKey, "0", EXPIRATION); //We insert this specifically to watch the keys to ensure that they are not modified
                    operations.opsForValue().increment(eachKey);
                    String value = (String) operations.opsForValue().get(eachKey);
                    if (nonNull(value))
                        throw new ConflictException("One or more seats are not available");
                }
                operations.opsForHash().putAll(showId, redisMapper.toHash(seatBooking));
                keys.add(blockedAllocation.getUserId());
                String commaSeparatedKeys = keys.stream()
                        .collect(joining(","));
                operations.opsForValue().set(blockedAllocation.getAllocationId(), commaSeparatedKeys);
                operations.expire(blockedAllocation.getAllocationId(), EXPIRATION);
                operations.expire(showId, EXPIRATION);
                // This will contain the results of all operations in the transaction
                return operations.exec();
            }
        });
        return !isEmpty(txResults);
    }

    private List<String> extractSeatNumbers(List<Seat> seats) {
        return seats
                .stream()
                .map(Seat::getSeatNumber)
                .collect(toList());
    }

    public Optional<Allocation> findByAllocationId(String allocationId) {
        String allocationValue = redisTemplate.opsForValue().get(allocationId);
        if (isNull(allocationValue))
            return Optional.empty();

        String[] values = allocationValue.split(",");
        int indexOfUserId = values.length - 1; //Last item stored is the userId
        String userId = values[indexOfUserId];
        List<Seat> seats = createSeatVo(values, indexOfUserId);
        String showId = values[0].split("_")[0];
        return Optional.of(new Blocked(allocationId, showId, seats, userId));
    }

    private List<Seat> createSeatVo(String[] values, int indexOfUserId) {
        return IntStream.range(0, indexOfUserId)
                .mapToObj(i -> values[i].split("_")[1])
                .map(eachSeat -> new Seat(eachSeat, BLOCKED))
                .collect(toList());
    }
}
