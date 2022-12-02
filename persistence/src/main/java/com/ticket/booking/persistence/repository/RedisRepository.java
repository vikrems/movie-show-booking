package com.ticket.booking.persistence.repository;

import com.ticket.booking.domain.entity.Seat;
import com.ticket.booking.domain.entity.state.Allocation;
import com.ticket.booking.domain.entity.state.Blocked;
import com.ticket.booking.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
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

    void delete(Allocation allocation) {
        List<String> keys = extractRedisKeys(allocation);
        redisTemplate.delete(keys);
        redisTemplate.delete(allocation.getAllocationId());
    }

    private List<String> extractRedisKeys(Allocation allocation) {
        return allocation.getSeats().stream()
                .map(eachSeat -> allocation.getShowId() + "_" + eachSeat.getSeatNumber() +
                        "_" + allocation.getUserId())
                .collect(toList());
    }

    boolean save(Blocked blockedAllocation) {
        List<String> keys = extractRedisKeys(blockedAllocation);
        validateKeyPresence(keys);
        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @SneakyThrows
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(keys);
                operations.multi();
                for (String eachKey : keys) {
                    operations.opsForValue().setIfAbsent(eachKey, "0", EXPIRATION); //We insert this specifically to watch the keys to ensure that they are not modified
                    String value = (String) operations.opsForValue().get(eachKey);
                    if (nonNull(value))
                        throw new ConflictException("One or more seats are not available");
                }
                keys.add(blockedAllocation.getUserId());
                String commaSeparatedKeys = keys.stream()
                        .collect(joining(","));
                operations.opsForValue().set(blockedAllocation.getAllocationId(), commaSeparatedKeys);
                operations.expire(blockedAllocation.getAllocationId(), EXPIRATION);
                // This will contain the results of all operations in the transaction
                return operations.exec();
            }
        });
        return !isEmpty(txResults);
    }

    private void validateKeyPresence(List<String> keys) {
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (isNull(values))
            return;
        for (String eachValue : values) {
            if (nonNull(eachValue))
                throw new ConflictException("You have already blocked one or more seats");
        }
    }
}
