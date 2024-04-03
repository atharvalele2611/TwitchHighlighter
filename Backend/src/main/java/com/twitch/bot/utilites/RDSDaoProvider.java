package com.twitch.bot.utilites;

import java.util.logging.Logger;

import com.twitch.bot.aws.AWSRelationalDatabaseSystem;
import com.twitch.bot.daoImpl.RDSChannelDaoImpl;
import com.twitch.bot.daoImpl.RDSSubscriptionsDaoImpl;
import com.twitch.bot.daoImpl.RDSUserDaoImpl;
import org.springframework.stereotype.Component;

@Component
public class RDSDaoProvider {
    private final RDSUserDaoImpl rdsUserDao;
    private final RDSChannelDaoImpl rdsChannelDao;
    private final RDSSubscriptionsDaoImpl rdsSubscriptionsDao;

    public RDSDaoProvider(AWSRelationalDatabaseSystem rdsConnection){
        rdsUserDao = new RDSUserDaoImpl(rdsConnection);
        rdsChannelDao = new RDSChannelDaoImpl(rdsConnection);
        rdsSubscriptionsDao = new RDSSubscriptionsDaoImpl(rdsConnection);
    }

    public RDSChannelDaoImpl getRdsChannelDao() {
        return rdsChannelDao;
    }

    public RDSUserDaoImpl getRdsUserDao() {
        return rdsUserDao;
    }

    public RDSSubscriptionsDaoImpl getRdsSubscriptionsDao() {
        return rdsSubscriptionsDao;
    }
}
