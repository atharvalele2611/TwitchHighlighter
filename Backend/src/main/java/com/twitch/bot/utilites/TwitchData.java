package com.twitch.bot.utilites;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.twitch.bot.model.Messages;
import com.twitch.bot.model.MessagesCount;
import com.twitch.bot.model.TwitchAnalysis;
import com.twitch.bot.model.TwitchAnalysis.ClipsDetails;
import com.twitch.bot.model.Channel;

import static com.mongodb.client.model.Filters.eq;

@Component
@DependsOn({"RDSDaoProvider", "twitchAWS_DynamoDB"})
public class TwitchData {
    private static final Logger LOG = Logger.getLogger(TwitchData.class.getName());
    TwitchAWS_DynamoDB twitchDynamoDB;
    RDSDaoProvider rdsDaoProvider;
    Integer messageMinsFrequency;
    String hourMinSeperator;

    public TwitchData(@Value("${rolling.message.minutes.frequency}") Integer messageMinsFrequency,
                      @Value("${hour.minutes.seperator}") String hourMinSeperator,
                      RDSDaoProvider twitchRdsDB,
                      TwitchAWS_DynamoDB twitchDynamoDB) {
        if (isAwsEnvironment()) {
            LOG.log(Level.INFO, "inside AWS Environment");
            this.twitchDynamoDB = twitchDynamoDB;
            this.rdsDaoProvider = twitchRdsDB;
        }
        this.messageMinsFrequency = messageMinsFrequency;
        this.hourMinSeperator = hourMinSeperator;
    }

    public static Boolean isAwsEnvironment() {
//        return System.getenv("AWS_ENVIRONMENT") != null ? Boolean.valueOf(System.getenv("AWS_ENVIRONMENT").toString())
//                : false;
        return true;
    }

    public void addTwitchMessage(String user, Channel channel, String message, Long timeStamp) {
        if (null == timeStamp) {
            timeStamp = System.currentTimeMillis();
        }
        twitchDynamoDB.addTwitchMessage(user, channel, message, timeStamp);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, Long fromTimeStamp, Long toTimeStamp) {
        return getTwitchMessageForChannel(channel, null, fromTimeStamp, toTimeStamp);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, String user, Long fromTimeStamp, Long toTimeStamp) {
        Messages message = new Messages();
        message.setId(null);
        message.setChannelName(channel.getChannelName());
        message.setMessage(null);
        message.setUserName(user);
        message.setTimestamp(null);
        return twitchDynamoDB.getTwitchMessageForChannelInJSONFormat(message, fromTimeStamp, toTimeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel) {
        deleteTwitchMessageForChannel(channel, null, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, Long timeStamp) {
        deleteTwitchMessageForChannel(channel, null, timeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user, Long toTimeStamp) {
        Messages message = new Messages();
        message.setId(null);
        message.setChannelName(channel.getChannelName());
        message.setMessage(null);
        message.setUserName(user);
        message.setTimestamp(null);
        twitchDynamoDB.deleteTwitchMessageForChannel(message, null, toTimeStamp);
    }


    public void deleteChannelRelatedInfo(Channel channel, Integer userId) throws Exception{
        deleteTwitchMessageForChannel(channel);
        twitchDynamoDB.deleteChannelRelatedInfo(channel);
        rdsDaoProvider.getRdsSubscriptionsDao().deleteSubscriptionDetailsForAChannel(channel,userId);
//        rdsDaoProvider.getRdsChannelDao().delete(channel);
    }

    public void updateChannelServerListeningData(Boolean isServerListening, Integer channelId) {
        try {
            rdsDaoProvider.getRdsChannelDao().updateChannelListenDetails(channelId, isServerListening);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
    }

    public JSONArray getTwitchAnalysisOfAChannel(Channel channel, Boolean isAscendingOrder) {
        return twitchDynamoDB.getTwitchAnalysisOfAChannelInJSON(channel, isAscendingOrder);
    }

    public void addTwitchAnalysis(Channel channel, String sentimental_result, ClipsDetails clip_details, Long timeStamp) {
        if(timeStamp == null){
            timeStamp = System.currentTimeMillis();
        }
        twitchDynamoDB.addTwitchAnalysisInDynamoDB(channel, sentimental_result, clip_details, timeStamp);
    }

    public JSONObject getCloudCredentials() {
        return getCloudCredentialsFromAWS();
    }

    private JSONObject getCloudCredentialsFromAWS() {
        return twitchDynamoDB.getCloudCredentialsFromAWS();
    }

    public void addMessageCountBasedOnRollingWindow(Channel channel, Long messageCount, Long timestamp) {
        Integer[] hoursAndMinutes = getHoursAndMinutes(timestamp);
        MessagesCount messageCountObj = new MessagesCount();
        messageCountObj.setHourMinutesKey(getHoursAndMinutesKey(hoursAndMinutes));
        messageCountObj.setChannelName(channel.getChannelName());
        List<MessagesCount> messagesData = twitchDynamoDB.getMessageCount(messageCountObj);
        if (messagesData.isEmpty()) {
            messageCountObj = new MessagesCount();
            messageCountObj.setChannelName(channel.getChannelName());
            messagesData = manipulateMessagesDataBasedOnMessageFreq(twitchDynamoDB.getMessageCount(messageCountObj), timestamp);
            twitchDynamoDB.addMessageCountToDynamoDB(channel, messageCount, getHoursAndMinutesKey(hoursAndMinutes));
        } else {
            messageCountObj = messagesData.get(0);
            twitchDynamoDB.updateMessageCountToDynamoDB(messageCountObj, messageCount);
        }

    }

    private List<MessagesCount> manipulateMessagesDataBasedOnMessageFreq(List<MessagesCount> messagesData, Long currentTimestamp){
        Integer[] currentHoursAndMinutes = getHoursAndMinutes(currentTimestamp);
        List<MessagesCount> toBeDeletedMessagesData = new ArrayList<>();
        for (MessagesCount msgCountData : messagesData) {
            String hourMinKey = msgCountData.getHourMinutesKey();
            String[] hourMinKeyArr = hourMinKey.split(hourMinSeperator);
            Integer hourMin[] = new Integer[hourMinKeyArr.length];
            for (int i = 0; i < hourMinKeyArr.length; i++) {
                hourMin[i] = Integer.parseInt(hourMinKeyArr[i]);
            }
            if (hourMin[0] == currentHoursAndMinutes[0]) {
                if (currentHoursAndMinutes[1] - messageMinsFrequency > hourMin[1]) {
                    toBeDeletedMessagesData.add(msgCountData);
                }
            } else if (hourMin[0] == (currentHoursAndMinutes[0] - 1) || (currentHoursAndMinutes[0] == 0 && hourMin[0] == 23)) {
                Integer remainingMins = currentHoursAndMinutes[1] - messageMinsFrequency;
                if (remainingMins >= 0) {
                    toBeDeletedMessagesData.add(msgCountData);
                } else {
                    if ((60 - remainingMins) > hourMin[1]) {
                        toBeDeletedMessagesData.add(msgCountData);
                    }
                }
            } else {
                toBeDeletedMessagesData.add(msgCountData);
            }
        }

        if (!toBeDeletedMessagesData.isEmpty()) {
            twitchDynamoDB.deleteMessageCount(toBeDeletedMessagesData);
            MessagesCount messageCountObj = new MessagesCount();
            messageCountObj.setHourMinutesKey(getHoursAndMinutesKey(currentHoursAndMinutes));
            messageCountObj.setChannelName(toBeDeletedMessagesData.get(0).getChannelName());
            return twitchDynamoDB.getMessageCount(messageCountObj);
        }
        return messagesData;
    }

    public List<TwitchAnalysis> getTwitchAnalysisRawDataOfAChannel(Channel channel, Boolean isAscending) {
        return twitchDynamoDB.getTwitchAnalysisOfAChannel(channel, isAscending);
    }

    public List<MessagesCount> getMessageCountDataOfAChannel(Channel channel){
        return twitchDynamoDB.getMessageCountDataOfAChannel(channel);
    }

    public void clearMessagesCountForAChannel(Channel channel){
        twitchDynamoDB.clearMessagesCountForAChannel(channel);
    }

    public String getHoursAndMinutesKey(Integer[] hours_minutes){
        return hours_minutes[0] + hourMinSeperator + hours_minutes[1];
    }

    public Integer[] getHoursAndMinutes(Long timeStamp){
        Timestamp stamp = new Timestamp(timeStamp);
        Date date = new Date(stamp.getTime());

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        return new Integer[]{hour, minute};
    }

    public RDSDaoProvider getRdsDaoProvider() {
        return rdsDaoProvider;
    }
}
