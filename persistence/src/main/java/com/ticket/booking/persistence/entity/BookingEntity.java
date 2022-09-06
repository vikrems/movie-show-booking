package com.ticket.booking.persistence.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DynamoDBTable(tableName = "test_booking")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingEntity {

    @DynamoDBHashKey
    private String partitionKey;

    @DynamoDBRangeKey
    private String sortKey;

    @DynamoDBAttribute
    private String category;

    @DynamoDBAttribute
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

    public static BookingEntity entityWithPartitionAndSortKey(String partitionKey, String sortKey){
        return new BookingEntity(partitionKey, sortKey);
    }
}
