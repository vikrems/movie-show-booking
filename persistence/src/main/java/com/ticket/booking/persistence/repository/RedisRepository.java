package com.ticket.booking.persistence.repository;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.exception.ConflictException;
import com.ticket.booking.persistence.entity.BookingEntity;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration EXPIRATION = Duration.ofSeconds(300);

    Allocation findByAllocationId(String allocationId) {
        String allocationValue = redisTemplate.opsForValue().get(allocationId);
        if (isNull(allocationValue))
            return null;

        String[] values = allocationValue.split(",");
        int indexOfUserId = values.length - 1; //Last item stored is the userId
        String userId = values[indexOfUserId];
        List<Seat> seats = createSeatVo(values, indexOfUserId);
        String showId = values[0].split("_")[0];
        return new Blocked(allocationId, showId, seats, userId);
    }

    List<Seat> createSeatVo(String[] values, int indexOfUserId) {
        return IntStream.range(0, indexOfUserId)
                .mapToObj(i -> values[i].split("_")[1])
                .map(eachSeat -> new Seat(eachSeat, BLOCKED))
                .collect(toList());
    }

    void updateEntityDetailsIfBlockedByTheUser(List<BookingEntity> bookingEntities) {
        String keyPrefix = bookingEntities.get(0).getPartitionKey() + "_";
        for (BookingEntity bookingEntity : bookingEntities) {
            String userId = redisTemplate.opsForValue().get(keyPrefix + bookingEntity.getSortKey());
            if (nonNull(userId))
                bookingEntity.updateDetails(userId, BLOCKED.name());
        }
    }

    private List<String> extractRedisKeys(String showId, List<String> seatNumbers) {
        return seatNumbers.stream()
                .map(eachSeat -> showId + "_" + eachSeat)
                .collect(toList());
    }

    void delete(Allocation allocation) {
        List<String> seats = extractSeatNumbers(allocation.getSeats());
        List<String> keys = extractRedisKeys(allocation.getShowId(), seats);
        redisTemplate.delete(keys);
        redisTemplate.delete(allocation.getAllocationId());
    }

    boolean save(Blocked blockedAllocation) {
        String showId = blockedAllocation.getShowId();
        List<String> seatNumbers = extractSeatNumbers(blockedAllocation.getSeats());
        List<String> keys = extractRedisKeys(showId, seatNumbers);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @SneakyThrows
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(keys);
                operations.multi();
                for (String eachKey : keys) {
                    operations.opsForValue().setIfAbsent(eachKey, "0", EXPIRATION); //We insert this specifically to watch the keys to ensure that they are not modified
                    operations.opsForValue().increment(eachKey);
                    String value = (String) operations.opsForValue().get(eachKey);
                    if (nonNull(value))
                        throw new ConflictException("One or more seats are not available");
                }
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
}
