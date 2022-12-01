package com.ticket.booking.persistence.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static com.ticket.booking.domain.entity.enums.Occupancy.BLOCKED;
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

    @DynamoDBAttribute //TODO Change this to an enum
    @DynamoDBTypeConvertedEnum
    private Occupancy occupancy;

    @DynamoDBAttribute
    private String userId;

    @DynamoDBAttribute
    private List<String> allocatedSeats;

    private BookingEntity(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    private BookingEntity(String partitionKey, String sortKey) {
        this.partitionKey = partitionKey;
        this.sortKey = sortKey;
    }

    private BookingEntity(String partitionKey, String sortKey, Occupancy occupancy, String userId) {
        this.partitionKey = partitionKey;
        this.sortKey = sortKey;
        this.occupancy = occupancy;
        this.userId = userId;
    }

    public static BookingEntity entityWithPartitionKey(String partitionKey) {
        return new BookingEntity(partitionKey);
    }

    public static BookingEntity entityWithPartitionAndSortKey(String partitionKey, String sortKey) {
        return new BookingEntity(partitionKey, sortKey);
    }

    public static BookingEntity entityWithPartitionKeySortKeyOccupancyUserId(String partitionKey, String sortKey,
                                                                             Occupancy occupancy, String userId) {
        return new BookingEntity(partitionKey, sortKey, occupancy, userId);
    }

    public static BookingEntity entityWithAllocationIdOccupancyUserIdAllocatedSeats(String allocationId,
                                                                                    Occupancy occupancy, String userId,
                                                                                    List<String> allocatedSeats){
        return new BookingEntity(allocationId, allocationId, occupancy, userId, allocatedSeats);
    }
    public void updateDetails(String userId, Occupancy occupancy) {
        this.userId = userId;
        this.occupancy = occupancy;
    }
}
