package com.twitch.bot.utilites;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.aws.AWSDynamoDb;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitch.bot.model.Messages;
import com.twitch.bot.model.MessagesCount;
import com.twitch.bot.model.TwitchAnalysis;
import com.twitch.bot.model.TwitchAnalysis.ClipsDetails;
import com.twitch.bot.model.TwitchAnalysis.SentimentalData;
import com.twitch.bot.model.Channel;

@Component
@DependsOn({"RDSDaoProvider","AWSDynamoDb"})
public class TwitchAWS_DynamoDB {
    private static final Logger LOG = Logger.getLogger(TwitchAWS_DynamoDB.class.getName());
    AmazonDynamoDB amazonDynamoDb;
    ObjectMapper objectMapper;
    RDSDaoProvider rdsConnection;

    public enum DYNAMODB_TABLES {
        MESSAGES("Messages"),
        MESSAGE_COUNT_ROLLING_WINDOW("Message_Count_Rolling_Window"),
        TWITCH_ANALYSIS("Twitch_Analysis");

        String tableName;

        DYNAMODB_TABLES(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String toString() {
            return this.tableName;
        }
    }

    public TwitchAWS_DynamoDB(ObjectMapper objectMapper, RDSDaoProvider rdsConnection){
        this.amazonDynamoDb = new AWSDynamoDb().getDynamoDb();
        this.objectMapper = objectMapper;
        this.rdsConnection = rdsConnection;
        this.makeConnectionToDynamoDB(getDyanmoDbTables());
    }

    public void PrePopulateDataInDB(){
        prePopulateData(objectMapper, rdsConnection);
    }


    private void prePopulateData(ObjectMapper objectMapper, RDSDaoProvider rdsConnection) {
        try {
            String prePopulate = System.getenv("PREPOPULATE_DB");
            Boolean isPrepopulateDb = false;
            if (null != prePopulate) {
                isPrepopulateDb = Boolean.valueOf(prePopulate); 
            }
            if (isPrepopulateDb) {
                List<Object> populatedData = objectMapper.readValue(new File("populationdata/dynamoDb.json"), new TypeReference<List<Object>>() {});
                List<Channel> channels = rdsConnection.getRdsChannelDao().getAll();
                HashMap<String, Channel> channelInfo = new HashMap<>();

                Iterator<Channel> channelsIter = channels.iterator();
                while(channelsIter.hasNext()){
                    Channel channel = channelsIter.next();
                    channelInfo.put(channel.getChannelName(), channel);
                }

                Iterator<Object> populatedDataIter = populatedData.iterator();
                while(populatedDataIter.hasNext()){
                    JSONObject data = new JSONObject((LinkedHashMap)populatedDataIter.next());
                    String channelName = data.get(Constants.CHANNEL_NAME).toString();
                    
                    Long timeStamp = data.getLong(Constants.TIMESTAMP);
                    if(!isTwitchAnalysisOfAChannelPresentAtTimestamp(channelInfo.get(channelName), timeStamp)){
                        JSONObject sentimentalClipsCollection = data.getJSONObject(Constants.SENTIMENTAL_CLIP_COLLECTIONS);
                        JSONObject clipDetails = sentimentalClipsCollection.getJSONObject(Constants.CLIP_DETAILS);
                        ClipsDetails clips = new ClipsDetails();
                        clips.setClip_id(clipDetails.get(Constants.CLIP_ID).toString());
                        clips.setCreated_at(clipDetails.get(Constants.CREATED_AT).toString());
                        clips.setEmbed_url(clipDetails.get(Constants.EMBED_URL).toString());
                        clips.setVideo_url(clipDetails.get(Constants.VIDEO_URL).toString());
                        clips.setThumbnail_url(clipDetails.get(Constants.THUMBNAIL_URL).toString());
                        if(channelInfo.containsKey(channelName)){
                            addTwitchAnalysisInDynamoDB(channelInfo.get(channelName),
                                    sentimentalClipsCollection.get(Constants.SENTIMENTAL_ANALYSIS).toString(), clips, timeStamp);
                        }
                    }
                    
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception is ::: " + ex.getMessage());
        }
    }

    private static List<String> getDyanmoDbTables() {
        DYNAMODB_TABLES[] tables = DYNAMODB_TABLES.values();
        List<String> tableNames = new ArrayList<>();
        for(DYNAMODB_TABLES table: tables){
            tableNames.add(table.toString());
        }
        return tableNames;
    }

    private void makeConnectionToDynamoDB(List<String> dynamoDbNames) {
        ListTablesRequest request;
        JSONObject credentials = this.getCloudCredentialsFromAWS();
        if(!credentials.isEmpty()){
            System.setProperty("aws.accessKeyId", credentials.get("access_id").toString());
            System.setProperty("aws.secretKey", credentials.get("access_key").toString());
        }

        boolean more_tables = true;
        String last_name = null;

        while (more_tables) {
            try {
                if (last_name == null) {
                    request = new ListTablesRequest().withLimit(10);
                } else {
                    request = new ListTablesRequest()
                            .withLimit(10)
                            .withExclusiveStartTableName(last_name);
                }

                ListTablesResult table_list = amazonDynamoDb.listTables(request);
                List<String> table_names = table_list.getTableNames();

                if (table_names.size() > 0) {
                    for (String cur_name : table_names) {
                        if (dynamoDbNames.contains(cur_name)) {
                            LOG.log(Level.INFO, "Table " + cur_name + " Exists");
                            dynamoDbNames.remove(cur_name);
                        }
                    }
                } else {
                    System.out.println("No tables found!");
                }

                last_name = table_list.getLastEvaluatedTableName();
                if (last_name == null) {
                    more_tables = false;
                }

            } catch (AmazonServiceException ex) {
                LOG.log(Level.SEVERE, "Exception in fetching tables ::: "+ ex.getMessage());
                more_tables = false;
            }
        }
        if (!dynamoDbNames.isEmpty()) {
            LOG.log(Level.SEVERE, "Tables Not Found In DynamoDB ::: "+ dynamoDbNames);
            for (String tableName : dynamoDbNames) {
                LOG.log(Level.INFO, "Creating Table " + tableName);
                createTableInDyanmoDB(tableName);
            }
        }
    }

    public void createTableInDyanmoDB(String tableName) {
        CreateTableRequest request = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName(Constants.ID).withKeyType(KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(5L)
                        .withWriteCapacityUnits(5L))
                .withAttributeDefinitions(new AttributeDefinition(Constants.ID, ScalarAttributeType.S));

        Table table = new DynamoDB(amazonDynamoDb).createTable(request);
        try{
            table.waitForActive();
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception :::" + ex.getMessage());
        }   
    }

    protected JSONObject getCloudCredentialsFromAWS() {
//        return new JSONObject().put("access_key", System.getenv("AWS_ACCESS_KEY")).put("access_id", System.getenv("AWS_ACCESS_ID"));
        return new JSONObject()
                .put(Constants.ACCESS_KEY, Constants.ACCESS_KEY_VALUE)
                .put(Constants.ACCESS_ID, Constants.ACCESS_ID_VALUE);
    }

    protected void addTwitchMessage(String user, Channel channel, String message, Long timeStamp) {

        Messages messages = new Messages();
        try {
            messages.setChannelName(channel.getChannelName());
            messages.setMessage(message);
            messages.setTimestamp(timeStamp);
            messages.setUserName(user.toString());

            DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
            mapper.save(messages);
            //LOG.log(Level.INFO, "Twitch Message Added in Dynamo DB");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception In Adding Twitch Message ::: " + ex);
        }
    }

    protected JSONArray getTwitchMessageForChannelInJSONFormat(Messages message, Long fromTimeStamp,
            Long toTimeStamp) {
        return new JSONArray(getTwitchMessageForChannel(message, fromTimeStamp, toTimeStamp));
    }

    protected List<Messages> getTwitchMessageForChannel(Messages message, Long fromTimeStamp,
            Long toTimeStamp) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        Map<String, String> expressionAttrNames = new HashMap<>();
        if (message.getId() != null) {
            expression += "id = :v1";
            expressionValue.put(":v1", new AttributeValue().withN(message.getId().toString()));
        }
        if (message.getChannelName() != null) {
            if (!expression.trim().equals("")) {
                expression += " and ";
            }
            expression += "channel_name = :v2";
            expressionValue.put(":v2", new AttributeValue().withS(message.getChannelName()));
        }
        if (message.getUserName() != null) {
            if (!expression.trim().equals("")) {
                expression += " and ";
            }
            expression += "user_name = :v3";
            expressionValue.put(":v3", new AttributeValue().withS(message.getUserName()));
        }
        if (message.getMessage() != null) {
            if (!expression.trim().equals("")) {
                expression += " and ";
            }
            expression += "message = :v4";
            expressionValue.put(":v4", new AttributeValue().withS(message.getMessage()));
        }
        if (message.getTimestamp() != null) {
            if (!expression.trim().equals("")) {
                expression += " and ";
            }
            expression += "timestamp = :v5";
            expressionValue.put(":v5", new AttributeValue().withN(message.getTimestamp().toString()));
        } else {
            if (fromTimeStamp != null && toTimeStamp != null) {
                if (!expression.trim().equals("")) {
                    expression += " and ";
                }
                expressionAttrNames.put("#dynamo_timestamp", "timestamp");
                expression += "#dynamo_timestamp between :v5 and :v6";
                expressionValue.put(":v5", new AttributeValue().withN(fromTimeStamp.toString()));
                expressionValue.put(":v6", new AttributeValue().withN(toTimeStamp.toString()));
            } else if (fromTimeStamp != null) {
                if (!expression.trim().equals("")) {
                    expression += " and ";
                }
                expressionAttrNames.put("#dynamo_timestamp", "timestamp");
                expression += "#dynamo_timestamp >= :v5";
                expressionValue.put(":v5", new AttributeValue().withN(fromTimeStamp.toString()));
            } else if (toTimeStamp != null) {
                if (!expression.trim().equals("")) {
                    expression += " and ";
                }
                expressionAttrNames.put("#dynamo_timestamp", "timestamp");
                expression += "#dynamo_timestamp <= :v5";
                expressionValue.put(":v5", new AttributeValue().withN(toTimeStamp.toString()));
            }
        }

        if (expression.trim().equals("")) {
            return new ArrayList<Messages>();
        }
        DynamoDBScanExpression queryExpression = new DynamoDBScanExpression()
                .withFilterExpression(expression)
                .withExpressionAttributeValues(expressionValue);
        if (!expressionAttrNames.isEmpty()) {
            queryExpression.withExpressionAttributeNames(expressionAttrNames);
        }
        List<Messages> result = new ArrayList<>();
        try {
            result = mapper.scan(Messages.class, queryExpression);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        //LOG.log(Level.INFO, "twitch Message Fetched ::: " + Arrays.toString(result.toArray()));
        return result;
    }

    protected void deleteTwitchMessageForChannel(Messages message, Long fromTimeStamp, Long toTimeStamp) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        try{
            mapper.batchDelete(getTwitchMessageForChannel(message, fromTimeStamp, toTimeStamp));
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
//        LOG.log(Level.INFO, "twitch Messages Deleted");
    }

    protected void deleteChannelRelatedInfo(Channel channel){
        deleteTwitchAnalysisInDynamoDB(channel, null);
        clearMessagesCountForAChannel(channel);
    }

    protected JSONArray getTwitchAnalysisOfAChannelInJSON(Channel channel, Boolean isAscendingOrder) {
        List<TwitchAnalysis> data = getTwitchAnalysisOfAChannel(channel, isAscendingOrder);
        JSONArray result = new JSONArray();
        Iterator<TwitchAnalysis> dataIter = data.iterator();
        while(dataIter.hasNext()){
            TwitchAnalysis twitchAnalysis = dataIter.next();
            result.put(twitchAnalysis.getSentimentalClipsCollection());
        }
        return result;
    }

    protected List<TwitchAnalysis> getTwitchAnalysisOfAChannel(Channel channel, Boolean isAscending) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        expression += "twitchChannelPk = :v1";
        expressionValue.put(":v1", new AttributeValue().withN(channel.getId().toString()));

        DynamoDBScanExpression queryExpression = new DynamoDBScanExpression()
                .withFilterExpression(expression)
                .withExpressionAttributeValues(expressionValue);

        PaginatedScanList<TwitchAnalysis> result = mapper.scan(TwitchAnalysis.class, queryExpression);

        result.loadAllResults();
        List<TwitchAnalysis> data = new ArrayList<TwitchAnalysis>(result.size());
        Iterator<TwitchAnalysis> iterator = result.iterator();
        while (iterator.hasNext()) {
            TwitchAnalysis element = iterator.next();
            data.add(element);
        }

        return sortTwitchAnalysisBasedOnTimeStamp(data, isAscending);
    }

    protected Boolean isTwitchAnalysisOfAChannelPresentAtTimestamp(Channel channel, Long timestamp) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        Map<String, String> expressionAttrNames = new HashMap<>();
        expression += "twitchChannelPk = :v1";
        expressionValue.put(":v1", new AttributeValue().withN(channel.getId().toString()));

        expressionAttrNames.put("#dynamo_timestamp", Constants.TIMESTAMP);
        expression += " and #dynamo_timestamp = :v2";
        expressionValue.put(":v2", new AttributeValue().withN(timestamp.toString()));

        DynamoDBScanExpression queryExpression = new DynamoDBScanExpression()
                .withFilterExpression(expression)
                .withExpressionAttributeValues(expressionValue)
                .withExpressionAttributeNames(expressionAttrNames);

        PaginatedScanList<TwitchAnalysis> result = mapper.scan(TwitchAnalysis.class, queryExpression);
        result.loadAllResults();
        return !result.isEmpty();
    }

    private List<TwitchAnalysis> sortTwitchAnalysisBasedOnTimeStamp(List<TwitchAnalysis> result, Boolean isAscending) {
        Collections.sort(result, new Comparator<TwitchAnalysis>() {

            @Override
            public int compare(TwitchAnalysis o1, TwitchAnalysis o2) {
                return isAscending ? o1.getTimestamp().compareTo(o2.getTimestamp()) : o2.getTimestamp().compareTo(o1.getTimestamp());
            }

        });

        return result;
    }

    protected List<TwitchAnalysis> getTwitchAnalysisRawDataOfAChannel(Channel channel, Boolean isAscending) {
        return getTwitchAnalysisOfAChannel(channel, isAscending);
    }

    protected void addTwitchAnalysisInDynamoDB(Channel channel, String sentimental_result, ClipsDetails clip_details, Long timeStamp) {
        TwitchAnalysis twitchAnalysis = new TwitchAnalysis();
        try {
            SentimentalData sentimentalData = new SentimentalData();
            sentimentalData.setSentimental_analysis(sentimental_result);
            sentimentalData.setClip_details(clip_details);
            twitchAnalysis.setSentimentalClipsCollection(sentimentalData);
            twitchAnalysis.setTwitchChannelPk(Long.valueOf(channel.getId().toString()));
            twitchAnalysis.setTimestamp(timeStamp);

            DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
            mapper.save(twitchAnalysis);
            LOG.log(Level.INFO, "Twitch Analysis Added");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
        }
    }

    protected void deleteTwitchAnalysisInDynamoDB(Channel channel, Long timeStamp) {
        TwitchAnalysis twitchAnalysis = new TwitchAnalysis();
        try {
            twitchAnalysis.setTwitchChannelPk(Long.valueOf(channel.getId().toString()));
            if (timeStamp != null) {
                twitchAnalysis.setTimestamp(timeStamp);
            }

            DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
            mapper.batchDelete(twitchAnalysis);

            LOG.log(Level.INFO, "Twitch Analysis Deleted");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
        }
    }

    protected void clearMessagesCountForAChannel(Channel channel){
        MessagesCount messageCountObj = new MessagesCount();
        messageCountObj.setChannelName(channel.getChannelName());
        List<MessagesCount> messagesData = getMessageCount(messageCountObj);
        if(!messagesData.isEmpty()){
            deleteMessageCount(messagesData);
        }
    }

    protected List<MessagesCount> getMessageCountDataOfAChannel(Channel channel){
        MessagesCount messageCountObj = new MessagesCount();
        messageCountObj.setChannelName(channel.getChannelName());
        return getMessageCount(messageCountObj);
    }

    protected void addMessageCountToDynamoDB(Channel channel, Long messageCount, String hoursMinsKey){
        MessagesCount messageCountObj = new MessagesCount();
        messageCountObj.setHourMinutesKey(hoursMinsKey);
        messageCountObj.setChannelName(channel.getChannelName());
        messageCountObj.setMessageCount(messageCount);
        try {
            DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
            mapper.save(messageCountObj);
            LOG.log(Level.INFO, "Message Count Added in Dynamo DB");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception In Adding Message Count ::: " + ex);
        }
    }

    protected void updateMessageCountToDynamoDB(MessagesCount messageCountObj, Long messageCount){
        messageCountObj.setMessageCount(messageCountObj.getMessageCount() + messageCount);
        try {
            DynamoDBMapperConfig dynamoDBMapperConfig = new DynamoDBMapperConfig.Builder()
                                                        .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                                                        .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                                                        .build();
  
            DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
            mapper.save(messageCountObj, dynamoDBMapperConfig);
            LOG.log(Level.INFO, "Message Count Updated in Dynamo DB");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception In Updating Message Count ::: " + ex);
        }
    }

    protected void deleteMessageCount(List<MessagesCount> messagesData) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        try{
            mapper.batchDelete(messagesData);
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        LOG.log(Level.INFO, "Messages Count Deleted");
    }

    protected List<MessagesCount> getMessageCount(MessagesCount messageCount) {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        if (messageCount.getChannelName() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "channel_name = :v2";
            expressionValue.put(":v2", new AttributeValue().withS(messageCount.getChannelName()));
        }
        if (messageCount.getHourMinutesKey() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "hour_minutes_key = :v3";
            expressionValue.put(":v3", new AttributeValue().withS(messageCount.getHourMinutesKey()));
        }

        if (expression.trim() == "") {
            return new ArrayList<MessagesCount>();
        }
        DynamoDBScanExpression queryExpression = new DynamoDBScanExpression()
                .withFilterExpression(expression)
                .withExpressionAttributeValues(expressionValue);
        List<MessagesCount> result = new ArrayList<>();
        try {
            result = mapper.scan(MessagesCount.class, queryExpression);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        // LOG.log(Level.INFO, "Messages Count Data Fetched ::: " + Arrays.toString(result.toArray()));
        return result;
    }
}
