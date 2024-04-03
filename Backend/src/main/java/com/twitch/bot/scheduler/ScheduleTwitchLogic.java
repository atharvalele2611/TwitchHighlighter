package com.twitch.bot.scheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.aws.AWSRelationalDatabaseSystem;
import com.twitch.bot.utilites.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.twitch.bot.handler.ApiHandler;
import com.twitch.bot.handler.ApiHandler.PATH;
import com.twitch.bot.aws.AWSComprehend;
import com.twitch.bot.aws.AWSCredentials;
import com.twitch.bot.utilites.TwitchData;
import com.twitch.bot.model.MessagesCount;
import com.twitch.bot.model.TwitchAnalysis;
import com.twitch.bot.model.TwitchAnalysis.ClipsDetails;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.Service.ChannelService;

@Lazy(false)
@Component
@EnableScheduling
public class ScheduleTwitchLogic {
    private static final Logger LOG = Logger.getLogger(ScheduleTwitchLogic.class.getName());
    private static final long frequencySeconds = 15000L;
    private final Long coolDownMillis;
    private final Long offsetMillis;
    private final TwitchData twitchData;
    private final AWSComprehend awsComprehend;

    public ScheduleTwitchLogic(TwitchData twitchData,
                               @Value("${twitch.analysis.cooldown.seconds}") Long coolDownSeconds,
                               @Value("${twitch.analysis.start.offset.minutes}") Long offsetMinutes){
        this.twitchData = twitchData;
        this.coolDownMillis = coolDownSeconds * 1000;
        this.offsetMillis = offsetMinutes * 60 * 1000;
        awsComprehend = new AWSComprehend();
    }

    @Scheduled(fixedRate = 15000)
    public void jobRunner() throws Exception {
            Long currentTime = System.currentTimeMillis();
        LOG.log(Level.INFO, "currentTime In Schedule ::: " + currentTime);
        String channelTiming = "";
        List<Channel> allChannelNames = twitchData.getRdsDaoProvider().getRdsChannelDao().getAll();
        for (Channel channel : allChannelNames) {
            long startTime = System.currentTimeMillis();
            if (channel.getIsListeningToChannel()) {
                processChannelMessages(channel, currentTime);
            } else {
                twitchData.deleteTwitchMessageForChannel(channel, currentTime);
            }
            channelTiming += (channelTiming.trim().equals("")) ?
                    channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime) :
                    ", " + channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime);
        }
        LOG.log(Level.INFO, "Scheduler Run Time ::: " + channelTiming);
    }

    public void processChannelMessages(Channel channel, Long tillTimeStamp) throws Exception {
        JSONObject channelDtls = getChannelDetails(channel);
        if (channelDtls.getBoolean(Constants.IS_CHANNEL_LIVE)) {
            Long startedAt = Long.valueOf(channelDtls.get("stream_started_at").toString());
            JSONArray messages = twitchData.getTwitchMessageForChannel(channel,
                    tillTimeStamp - frequencySeconds,
                    tillTimeStamp);
            twitchData.addMessageCountBasedOnRollingWindow(channel, (long) messages.length(), tillTimeStamp);
            Long thresholdValue = getThresholdValueBasedOnChannel(channel);
            if(thresholdValue == -1){
                LOG.log(Level.INFO, "Rolling Window Data not populated for channel {0}",
                        new Object[]{channel.getChannelName()});
            }
            else if(thresholdValue == 0){
                LOG.log(Level.INFO, "No Messages for channel {0}",
                        new Object[]{channel.getChannelName()});
            }
            else if ((startedAt + offsetMillis) >= tillTimeStamp) {
                LOG.log(Level.INFO, "Channel {0} Start Time {1} is under offsetValue {2} for timestamp {3}",
                        new Object[] { channel.getChannelName(), startedAt, offsetMillis, tillTimeStamp });
            }else if (messages.length() >= thresholdValue) {
                List<TwitchAnalysis> twitchAnalysis = twitchData.getTwitchAnalysisRawDataOfAChannel(channel,
                        false);
                if (!twitchAnalysis.isEmpty()
                        && (twitchAnalysis.get(0).getTimestamp() + coolDownMillis) > tillTimeStamp) {
                    LOG.log(Level.INFO,
                            "Last Generated Data Time is {0} which is not exceeds the current cooldown of {1} seconds from current time {2}",
                            new Object[] { twitchAnalysis.get(0).getTimestamp(), coolDownMillis, tillTimeStamp });
                    twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
                    return;
                }
                ClipsDetails clips = awsClipsGeneration(channel);
                if(clips != null){
                    LOG.log(Level.INFO,"clips ::: " + clips);
                    String sentimental_result = awsComprehend.getSentiment(messageMerge(messages));
                    LOG.log(Level.INFO,"sentimental_result ::: " + sentimental_result);
                    twitchData.addTwitchAnalysis(channel, sentimental_result, clips, System.currentTimeMillis());

//                    AWS_Sns sns = new AWS_Sns(awsCredentials);
//                    SnsData data = new SnsData();
//                    data.setUserId(getSubscribedUserIds(channel));
//                    data.setChannelId(channel.getId());
//                    data.setChannelName(channel.getChannelName());
//                    sns.publishSNSMessage(data);
                }              
            }
            twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
        } else {
            twitchData.clearMessagesCountForAChannel(channel);
        }
    }

    private List<Integer> getSubscribedUserIds(Channel channel){
        List<Integer> userIds = new ArrayList<>();
        try{
            List<Subscriptions> data = twitchData.getRdsDaoProvider().getRdsSubscriptionsDao()
                    .getSubscriptionDetailsBasedOnUserOrSubscriptionId(channel.getId(), false);
            for (Subscriptions subs : data) {
                userIds.add(subs.getUserId());
            }
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception in fetching userIds ::: " + ex.getMessage());
        }
       return userIds;
    }

    public JSONObject getChannelDetails(Channel channel) throws Exception{
        String response = new ApiHandler.ApiHandlerBuilder()
                .setPath(PATH.GET_STREAMS.getPath())
                .setParams(new JSONObject()
                        .put("user_login", channel.getChannelName()))
                .setHeaders(new JSONObject().put(Constants.SET_CLIENT_ID, Constants.CLIENT_ID_HEADER))
                .build()
                .GET();
        JSONObject responseData = new JSONObject(response);
        JSONObject channelDtls = new JSONObject();
        if(responseData.isEmpty()){
            channelDtls.put(Constants.IS_CHANNEL_LIVE, false);
        }else if(responseData.getJSONArray(Constants.DATA).isEmpty()){
            channelDtls.put(Constants.IS_CHANNEL_LIVE, false);
        }else{
            JSONObject data = responseData.getJSONArray(Constants.DATA).getJSONObject(0);
            Boolean isChannelLive = data.get("type").toString().equalsIgnoreCase("live");
            channelDtls.put(Constants.IS_CHANNEL_LIVE, isChannelLive);
            if(isChannelLive){
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(data.get("started_at").toString());
                channelDtls.put("stream_started_at", date.getTime());
            }   
        }
        return channelDtls;
    }

    public Long getThresholdValueBasedOnChannel(Channel channel) {
        List<MessagesCount> msgCountData = twitchData.getMessageCountDataOfAChannel(channel);
        Long thresholdValue = -1L;
        if (!msgCountData.isEmpty()) {
            thresholdValue = 0L;
            for (MessagesCount msgData : msgCountData) {
                thresholdValue += msgData.getMessageCount();
            }
            thresholdValue = thresholdValue / (msgCountData.size() * 4L);
        }
        return thresholdValue;
    }

    public String messageMerge(JSONArray messages){
        String messagesStr = "";
        for (Object message : messages) {
            JSONObject messageObj = (JSONObject) message;
            messagesStr += messageObj.get("message").toString();
        }
        return messagesStr;
    }

    public ClipsDetails awsClipsGeneration(Channel channel) throws Exception{
        String response = new ApiHandler.ApiHandlerBuilder()
                .setPath(PATH.CLIPS.getPath())
                .setParams(new JSONObject().put(Constants.BROADCASTER_ID, channel.getTwitchId()))
                .setHeaders(new JSONObject().put(Constants.SET_CLIENT_ID, Constants.CLIENT_ID_HEADER))
                .build()
                .POST();
        JSONObject responseData = new JSONObject(response);
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 1 ::: " + responseData);
        if(!responseData.has(Constants.DATA)){
            LOG.log(Level.SEVERE, "Clips Generation Issue for Channel ::: {0} ::: Response ::: {1}",
                    new Object[]{channel.getChannelName(), responseData});
            return null;
        }
        String clip_id = responseData.getJSONArray(Constants.DATA)
                .getJSONObject(0).getString(Constants.ID);
        LOG.log(Level.INFO,"CLIPS:::clip_id in clips 1.1 ::: " + clip_id);
        Thread.sleep(5000);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = new ApiHandler.ApiHandlerBuilder()
                .setPath(PATH.CLIPS.getPath())
                .setParams(new JSONObject().put(Constants.ID, clip_id))
                .setHeaders(new JSONObject().put(Constants.SET_CLIENT_ID, Constants.CLIENT_ID_HEADER))
                .build()
                .GET();
        responseData = new JSONObject(response);
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 2 ::: " + responseData);
        responseData = responseData.getJSONArray(Constants.DATA).getJSONObject(0);
        return prepareClipData(responseData,clip_id);
    }

    private ClipsDetails prepareClipData(JSONObject responseData,String clip_id){
        ClipsDetails clipsDetails = new ClipsDetails();
        clipsDetails.setClip_id(clip_id);
        clipsDetails.setVideo_url(responseData.get(Constants.URL).toString());
        clipsDetails.setEmbed_url(responseData.get(Constants.EMBED_URL).toString());
        clipsDetails.setThumbnail_url(responseData.get(Constants.THUMBNAIL_URL).toString());
        clipsDetails.setCreated_at(responseData.get(Constants.CREATED_AT).toString());
        LOG.log(Level.INFO,"CLIPS:::data in clips 3 ::: " + clipsDetails);
        return clipsDetails;
    }
}
