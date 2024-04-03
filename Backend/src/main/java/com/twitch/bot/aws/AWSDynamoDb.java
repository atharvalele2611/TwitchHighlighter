package com.twitch.bot.aws;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.springframework.stereotype.Component;

@Component
public class AWSDynamoDb {
    private final AmazonDynamoDB dynamoDb;

    public AWSDynamoDb(){
        dynamoDb = AmazonDynamoDBClientBuilder
                .standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    public AmazonDynamoDB getDynamoDb() {
        return dynamoDb;
    }
}
