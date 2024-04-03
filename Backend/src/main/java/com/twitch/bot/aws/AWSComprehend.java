package com.twitch.bot.aws;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.regions.Regions;
import com.twitch.bot.utilites.Constants;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;

@Component
public class AWSComprehend {
    private static final Logger LOG = Logger.getLogger(AWSComprehend.class.getName());
    private final ComprehendClient comClient;

    public AWSComprehend(){
        System.setProperty("aws.accessKeyId", Constants.ACCESS_ID_VALUE);
        System.setProperty("aws.secretKey", Constants.ACCESS_KEY_VALUE);

        comClient = ComprehendClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();
    }

    public String getSentiment(String message) throws Exception{
        System.setProperty("aws.accessKeyId", Constants.ACCESS_ID_VALUE);
        System.setProperty("aws.secretAccessKey", Constants.ACCESS_KEY_VALUE);

        DetectSentimentRequest detectSentimentRequest = DetectSentimentRequest.builder()
                .text(message)
                .languageCode("en")
                .build();

            DetectSentimentResponse detectSentimentResult = comClient.detectSentiment(detectSentimentRequest);
            return detectSentimentResult.sentimentAsString();

    }
}
