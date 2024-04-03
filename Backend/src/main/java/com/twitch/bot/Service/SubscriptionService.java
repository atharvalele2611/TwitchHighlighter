package com.twitch.bot.Service;

import com.twitch.bot.daoImpl.RDSSubscriptionsDaoImpl;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@DependsOn({ "RDSSubscriptionsDaoImpl"})
public class SubscriptionService {
    private final UserService userService;
    private final ChannelService channelService;
    private final RDSSubscriptionsDaoImpl rdsSubscriptionsDao;

    public SubscriptionService(UserService userService,
                               ChannelService channelService,
                               RDSSubscriptionsDaoImpl rdsSubscriptionsDao){
        this.userService = userService;
        this.channelService = channelService;
        this.rdsSubscriptionsDao = rdsSubscriptionsDao;
    }

    public List<Channel> getUserSubscribedChannels(User user) throws Exception{
        List<Subscriptions> subscriptions = rdsSubscriptionsDao
                .getSubscriptionDetailsBasedOnUserOrSubscriptionId(user.getUserId(), true);
        Iterator<Subscriptions> subscriptionsIter = subscriptions.iterator();
        List<Channel> subscribedChannels = new ArrayList<>();
        while(subscriptionsIter.hasNext()){
            Subscriptions subscription = subscriptionsIter.next();
            Channel channel = channelService.getChannel(subscription.getChannelId());
            subscribedChannels.add(channel);
        }
        return subscribedChannels;
    }

    public Subscriptions checkAndAddUserSubscriptions(Integer userId, Integer channelId) throws Exception{
        if(userService.authenticateUser(userId)){
            Channel channel = channelService.getChannel(channelId);
            if(channel != null){
                return addUserSubscriptions(userId, channel);
            }
        }
        return null;
    }

    public Boolean checkAndDeleteUserSubscriptions(Integer userId, Integer channelId) throws Exception{
        if(userService.authenticateUser(userId)){
            Channel channel = channelService.getChannel(channelId);
            User user = userService.getUserDetails(userId);
            if(channel != null && user != null){
                return deleteUserSubscriptions(userId, channel);
            }
        }
        return false;
    }

    public Boolean isUserSubscribedToChannel(Integer userId, Channel channel) throws Exception{
        return rdsSubscriptionsDao.checkIfSubscriptionExists(userId, channel.getId());
    }

    public Subscriptions addUserSubscriptions(Integer userId, Channel channel) throws Exception{
        if(isUserSubscribedToChannel(userId, channel)){
            throw new Exception("Subscription Already Present");
        }else{
            return rdsSubscriptionsDao.addSubscriptionDetails(userId, channel);
        }
    }

    public Boolean deleteUserSubscriptions(Integer userId, Channel channel) throws Exception{
        if(!isUserSubscribedToChannel(userId, channel)){
            throw new Exception("Subscription Not Present");
        }else{
            Subscriptions subscriptions = new Subscriptions(userId, channel.getId());
            return rdsSubscriptionsDao.delete(subscriptions);
        }
    }

    public List<Integer> getAllSubscribedChannelIds(User user) throws Exception{
        List<Subscriptions> subscriptions = rdsSubscriptionsDao
                .getSubscriptionDetailsBasedOnUserOrSubscriptionId(user.getUserId(), true);
        List<Integer> channelIds = new ArrayList<>();
        for (Subscriptions subscription : subscriptions) {
            channelIds.add(subscription.getChannelId());
        }
        return channelIds;
    }
}
