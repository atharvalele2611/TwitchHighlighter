package com.twitch.bot.model;

public class Subscriptions {
    private final Integer userId;
    private final Integer channelId;

    public Subscriptions(Integer userId, Integer channelId){
        this.userId = userId;
        this.channelId = channelId;
    }
    
    public Integer getUserId() {
        return userId;
    }

    public Integer getChannelId() {
        return channelId;
    }
}
