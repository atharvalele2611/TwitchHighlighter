package com.twitch.bot.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.daoImpl.RDSChannelDaoImpl;
import com.twitch.bot.handler.ApiHandler;
import com.twitch.bot.utilites.Constants;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.twitch.bot.model.Channel;

@Component
@DependsOn({"RDSChannelDaoImpl"})
public class ChannelService {
    private static final Logger LOG = Logger.getLogger(ChannelService.class.getName());
    private static final HashMap<String, Channel> channels = new HashMap<>();
    private final RDSChannelDaoImpl rdsChannelDao;

    public ChannelService(RDSChannelDaoImpl rdsChannelDao,
                          @Value("${mandatory.channel.names}") String mandatoryChannelNames
    ) throws Exception {
        this.rdsChannelDao = rdsChannelDao;
        LOG.log(Level.INFO, "Mandatory channel names " + mandatoryChannelNames);
        if(channels.isEmpty()){
            List<Channel> channelInDatabase = rdsChannelDao.getAll();
            if(channelInDatabase.isEmpty()){
                String[] mandatoryChannelNamesArray = mandatoryChannelNames.split(",");
                populateMandatoryChannels(Arrays.asList(mandatoryChannelNamesArray));
            }else{
                setChannelDetails(channelInDatabase);
            }
        } 
    }

    public  HashMap<String, Channel> getChannels() {
        return channels;
    }

    public void setChannelDetails(List<Channel> channelsData){
        for (Channel channel : channelsData) {
            channels.put(channel.getChannelName(), channel);
        }
    }

    public  Channel getChannel(Integer channelId) {
        HashMap<String, Channel> channels = getChannels();
        for (String channelName : channels.keySet()) {
            Channel channelInfo = channels.get(channelName);
            if (channelInfo.getId().equals(channelId)) {
                return channelInfo;
            }
        }
        return null;
    }

    public  Channel getChannel(String channel) {
//        LOG.log(Level.INFO, "> channels " + channels);
        return channels.get(channel);
    }

    public Channel addChannel(String channelName) throws Exception{
        return getSingleChannelFromTwitch(channelName);
    }

    public Channel addChannel(String channelName, String channelId) throws Exception {
        Channel channel = rdsChannelDao
                .getChannelDetails(channelName, channelId);
        if(channel == null){
            channel = rdsChannelDao
                    .addChannelDetails(channelName, channelId,false);
        }
        if(channel != null){
            channels.put(channel.getChannelName(), channel);
        }
        return channel;
    }

    public void removeChannel(Channel channel) throws Exception {
        rdsChannelDao.delete(rdsChannelDao.get(channel.getId()));
    }

    public  Channel joinChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage(Constants.JOIN + "#" + channelName + "\r\n");
        LOG.log(Level.INFO, "> JOIN " + channelName);
        channel.setIsListeningToChannel(true, twitch_bot.getTwitchData());
        channels.put(channelName, channel);
        return channel;
    }

    public  void stopListeningToChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage(Constants.PART + "#" + channelName);
        channel.setIsListeningToChannel(false, twitch_bot.getTwitchData());
        channels.put(channelName, channel);
        LOG.log(Level.INFO, "> PART " + channelName);
    }

    private void populateMandatoryChannels(List<String> channelNames) throws Exception{
        for (String channelName : channelNames) {
            addChannel(channelName.toLowerCase());
        }
    }

    public Channel getSingleChannelFromTwitch(String name) throws Exception {
        String response = new ApiHandler.ApiHandlerBuilder()
                .setPath(ApiHandler.DOMAIN.GET_USERS.getDomain())
                .setParams(new JSONObject().put(Constants.LOGIN, name))
                .setHeaders(new JSONObject().put(Constants.SET_CLIENT_ID, Constants.CLIENT_ID_HEADER))
                .build()
                .GET();
        JSONObject responseData = new JSONObject(response);
        JSONObject jsonObject = responseData
                .getJSONArray(Constants.DATA)
                .getJSONObject(0);
        String loginName = jsonObject.getString(Constants.LOGIN);
        String offline_image_url = jsonObject.getString(Constants.OFFLINE_IMAGE_URL);
        String broadcasterId = jsonObject.getString(Constants.ID);
        Channel channel = addChannel(loginName,broadcasterId);
        channel.setOfflineImageURL(offline_image_url);
        channels.put(loginName,channel);
        LOG.log(Level.INFO, "Channel " + channel);
        return channel;
    }

    public List<HashMap<String, Object>> getChannelsFromTwitch(String searchQuery) throws Exception {
        List<HashMap<String, Object>> channelList = new ArrayList<>();
        String response = new ApiHandler.ApiHandlerBuilder()
                .setPath(ApiHandler.DOMAIN.GET_USERS.getDomain())
                .setParams(new JSONObject().put(Constants.LOGIN, searchQuery))
                .setHeaders(new JSONObject().put(Constants.SET_CLIENT_ID, Constants.CLIENT_ID_HEADER))
                .build()
                .GET();
        JSONObject responseData = new JSONObject(response);
        for(Object object : responseData.getJSONArray(Constants.DATA)){
            JSONObject jsonObject = (JSONObject) object;
            String loginName = jsonObject.getString(Constants.LOGIN);
            String offline_image_url = jsonObject.getString(Constants.OFFLINE_IMAGE_URL);
            Channel channel = addChannel(loginName);
            channel.setOfflineImageURL(offline_image_url);
            HashMap<String, Object> channelDtls = new HashMap<>();
            channelDtls.put(Constants.ID, channel.getId());
            channelDtls.put(Constants.CHANNEL_NAME, channel.getChannelName());
            channelDtls.put(Constants.TWITCH_ID, channel.getTwitchId());
            channelDtls.put(Constants.IS_USER_SUBSCRIBED, false);
            channelDtls.put(Constants.OFFLINE_IMAGE_URL, channel.getOfflineImageUrl());
            channelList.add(channelDtls);
        }
        return channelList;
    }
}
