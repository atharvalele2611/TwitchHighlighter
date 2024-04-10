package com.twitch.bot.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.utilites.Constants;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitch.bot.handler.ApiHandler;
import com.twitch.bot.utilites.TwitchAWS_DynamoDB;
import com.twitch.bot.utilites.TwitchData;
import com.twitch.bot.model.TwitchAnalysis.SentimentalData;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.User;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn({"userService", "twitchData", "apiHandler","channelService","subscriptionService"})
public class Connection {
    private static final Logger LOG = Logger.getLogger(Connection.class.getName());
    private final ApiHandler apiHandler;
    private Boolean isConnectionRunning = false;
    private Boolean isStartReadingMessagesStarted = false;
    private BufferedReader twitch_reader;
    private BufferedWriter twitch_writer;

    private final TwitchData twitchData;
    private final UserService userService;
    private final ChannelService channelService;
    private final SubscriptionService subscriptionService;

    public TwitchData getTwitchData() {
        return twitchData;
    }

    public ChannelService getChannelService() {return channelService;}

    public UserService getUserService() {return userService;}

    public SubscriptionService getSubscriptionService() {return subscriptionService;}

    public Connection(ApiHandler apiHandler,
                      TwitchData twitchData,
                      UserService userService,
                      TwitchAWS_DynamoDB dynamo,
                      ChannelService channelService,
                      SubscriptionService subscriptionService) throws Exception {
        this.apiHandler = apiHandler;
        this.twitchData = twitchData;
        this.userService = userService;
        this.channelService = channelService;
        this.subscriptionService = subscriptionService;
        this.connect();

        dynamo.PrePopulateDataInDB();
    }

    public void sendCommandMessage(Object message) {
        try {
            this.twitch_writer.write(message + " \r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
    }
    
    public void removeAndDeleteChannelData(String channelName, Integer userId) throws Exception{
        Channel channel = channelService.getChannel(channelName);
        if(channel != null){
            removeChannel(channel.getChannelName());
            twitchData.deleteChannelRelatedInfo(channel,userId);
        }
    }

    public void removeChannel(String channelName){
        channelService.stopListeningToChannel(channelName, this);
    }

    public Boolean connect() throws Exception {
        //Boolean isFirstTimeConnect = !isConnectionRunning;
        if (!isConnectionRunning) {
            isConnectionRunning = apiHandler.CONNECT();
        }
        this.twitch_writer = apiHandler.getTwitch_writer();
        this.twitch_reader = apiHandler.getTwitch_reader();
        LOG.log(Level.INFO, "Log Check");

        startReadingMessages();
        return isConnectionRunning;
    }

    private void startReadingMessages() {
        if (isStartReadingMessagesStarted) {
            return;
        }
        isStartReadingMessagesStarted = true;
        new Thread(() -> {
            readTwitchMessage();
        }).start();
    }

    public void readTwitchMessage(){
        String currentLine = "";
        try {
            while ((currentLine = this.twitch_reader.readLine()) != null) {
                if (currentLine.toLowerCase().startsWith(Constants.PING)) {
                    processPingMessage(currentLine);
                } else if (currentLine.contains(Constants.PRIVMSG)) {
                    processMessage(currentLine);
                } else if (currentLine.toLowerCase().contains(Constants.DISCONNECTED)) {
                    //LOG.log(Level.INFO, currentLine);
                    apiHandler.CONNECT();
                } else {
                    //LOG.log(Level.INFO, currentLine);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception is " + ex);
            ex.printStackTrace();
            isStartReadingMessagesStarted= false;
        }
    }

    private void processPingMessage(String currentLine) throws Exception {
        this.twitch_writer.write(Constants.PONG + currentLine.substring(5) + "\r\n");
        this.twitch_writer.flush();
    }

    private void processMessage(String currentLine) {
        String[] str = currentLine.split("!");
        String msg_user = str[0].substring(1);
        str = currentLine.split(" ");
        Channel msg_channel = channelService.getChannel(str[2].startsWith("#") ?
                str[2].substring(1).toLowerCase() :
                str[2].toLowerCase());
        String msg_msg = currentLine.substring((str[0].length() + str[1].length() + str[2].length() + 4));
        processMessage(msg_user, msg_channel, msg_msg);
    }

    private void processMessage(String user, Channel channel, String message) {
        twitchData.addTwitchMessage(user, channel, message, System.currentTimeMillis());
	}

    public List<HashMap<String,Object>> getTwitchAnalysisOfAChannelInListOfHashmap(String channelName){
        JSONArray data = twitchData
                .getTwitchAnalysisOfAChannel(channelService
                        .getChannel(channelName), true);
        Iterator<Object> dataIter = data.iterator();
        List<HashMap<String,Object>> result = new ArrayList<>();
        while(dataIter.hasNext()){
            SentimentalData value = (SentimentalData)dataIter.next();
            HashMap<String,Object> sentimentalData = new ObjectMapper()
                    .convertValue(value, new TypeReference<>() {
                    });
            result.add(sentimentalData);
        }
        return result;
    }

    public List<HashMap<String, Object>> getAllChannels(User user) throws Exception{
        HashMap<String, Channel> channels = channelService.getChannels();
        Iterator<String> channelsIter = channels.keySet().iterator();
        List<HashMap<String, Object>> result = new ArrayList<>();
        List<Integer> subscribedChannelIds = subscriptionService.getAllSubscribedChannelIds(user);
        while(channelsIter.hasNext()){
            String channelName = channelsIter.next();
            Channel channel = channels.get(channelName);
            HashMap<String, Object> channelDtls = new HashMap<>();
            channelDtls.put(Constants.ID, channel.getId());
            channelDtls.put(Constants.CHANNEL_NAME, channel.getChannelName());
            channelDtls.put(Constants.TWITCH_ID, channel.getTwitchId());
            channelDtls.put(Constants.IS_USER_SUBSCRIBED, subscribedChannelIds.contains(channel.getId()));
            channelDtls.put(Constants.OFFLINE_IMAGE_URL, channel.getOfflineImageUrl());
            result.add(channelDtls);
        }
        LOG.log(Level.INFO, "channels list ::: "+ result);
        return result;
    }
}
