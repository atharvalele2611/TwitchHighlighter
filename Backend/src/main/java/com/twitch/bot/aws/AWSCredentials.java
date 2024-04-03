package com.twitch.bot.aws;

import com.amazonaws.regions.Regions;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

@Component
public class AWSCredentials {
    String accessKey;
    String accessID;

    public String getAccess_key() {
        return accessKey;
    }
    public String getAccess_id() {
        return accessID;
    }

    public AWSCredentials() {}

    public AWSCredentials(String access_key, String access_id){
        this.accessKey = access_key;
        this.accessID = access_id;

        System.setProperty("aws.accessKeyId", access_id);
        System.setProperty("aws.secretAccessKey", access_key);
    }

    public static Region getRegionInRegionForm(){
//        String regionName = System.getenv("AWS_REGION");
        String regionName = "us-east-1";
        return Region.of(regionName);
    }

    public static Regions getRegionInRegionsForm(){
//        String regionName = System.getenv("AWS_REGION");
        String regionName = "us-east-1";
        return Regions.fromName(regionName);
    }
}
