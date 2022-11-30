package com.ticket.booking.persistence.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String occupancy;

    @DynamoDBAttribute
    private String userId;

    private BookingEntity(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    private BookingEntity(String partitionKey, String sortKey) {
        this.partitionKey = partitionKey;
        this.sortKey = sortKey;
    }

    public static BookingEntity entityWithPartitionKey(String partitionKey) {
        return new BookingEntity(partitionKey);
    }

    public static BookingEntity entityWithPartitionAndSortKey(String partitionKey, String sortKey) {
        return new BookingEntity(partitionKey, sortKey);
    }

    public static BookingEntity entityWithPartitionKeySortKeyOccupancyUserId(String partitionKey, String sortKey,
                                                                             String occupancy, String userId) {
        return new BookingEntity(partitionKey, sortKey, occupancy, userId);
    }

    public void updateDetails(String userId, String occupancy) {
        this.userId = userId;
        this.occupancy = occupancy;
    }
}
