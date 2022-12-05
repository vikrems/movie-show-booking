package com.ticket.booking.persistence.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@DynamoDBTable(tableName = "test_booking")
@Getter
@Setter
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor
public class BookingEntity {

    @DynamoDBHashKey
    private String partitionKey;

    @DynamoDBRangeKey
    private String sortKey;

    @DynamoDBAttribute
    @DynamoDBTypeConvertedEnum
    private Occupancy occupancy;

    @DynamoDBAttribute
    private String userId;

    @DynamoDBAttribute
    private List<String> allocatedSeats;

    @DynamoDBVersionAttribute
    private Long version;

    private BookingEntity(String partitionKey) {
        this.partitionKey = partitionKey;
    }


    public static BookingEntity entityWithPartitionKey(String partitionKey) {
        return new BookingEntity(partitionKey);
    }


    public static BookingEntity entityWithAllocationIdOccupancyUserIdAllocatedSeats(String allocationId,
                                                                                    Occupancy occupancy, String userId,
                                                                                    List<String> allocatedSeats) {
        return new BookingEntity(allocationId, allocationId, occupancy, userId, allocatedSeats, null);
    }

    public static BookingEntity entityWithPartitionKeySortKeyOccupancyUserIdAndVersion(String partitionKey, String sortKey,
                                                                             Occupancy occupancy, String userId, Long version) {
        return new BookingEntity(partitionKey, sortKey, occupancy, userId, null, version);
    }


    public void updateDetails(String userId, Occupancy occupancy) {
        this.userId = userId;
        this.occupancy = occupancy;
    }
}
