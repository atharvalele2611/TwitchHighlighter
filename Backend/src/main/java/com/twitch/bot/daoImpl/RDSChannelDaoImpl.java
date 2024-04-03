package com.twitch.bot.daoImpl;

import com.twitch.bot.aws.AWSRelationalDatabaseSystem;
import com.twitch.bot.dao.RDSDao;
import com.twitch.bot.model.Channel;
import com.twitch.bot.utilites.Constants;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class RDSChannelDaoImpl implements RDSDao<Channel> {
    Connection rdsConnection;

    public RDSChannelDaoImpl(AWSRelationalDatabaseSystem rdsConnection){
        this.rdsConnection = rdsConnection.getRdsConnection();
    }

    @Override
    public List<Channel> getAll() throws Exception {
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(AWSRelationalDatabaseSystem.TWITCH_STREAMERS
                .SELECT_RECORDS_WITHOUT_WHERE.toString().replace("{0}", "*"));
        List<Channel> channels = new ArrayList<>();
        while(result.next()){
            channels.add(getChannelFromResultSet(result));
        }
        return channels;
    }

    @Override
    public Channel get(Integer id) throws Exception{
        String filterCondition ;
        filterCondition = AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_ID
                + Constants.EQUALS + id;

        ResultSet result = getTwitchStreamersRecordBasedOnCriteria(getAllTwitchStreamersColumns(), filterCondition);
        while(result.next()){
            return getChannelFromResultSet(result);
        }
        return null;
    }

    @Override
    public Boolean delete(Channel channel) throws Exception {
        if (!ifGivenObjectIsValid(channel.getId())) {
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_ID
                + Constants.EQUALS + channel.getId();
        return deleteTwitchStreamersRecord(filterCondition);
    }


    public Channel getChannelDetails(String channelName, String twitchId) throws Exception{
        String filterCondition = "";
        if(ifGivenObjectIsValid(channelName)){
            filterCondition = AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_NAME
                    + Constants.EQUALS + addStringLiteralToString(channelName);
        }
        if(ifGivenObjectIsValid(twitchId)){
            if(!filterCondition.trim().equals("")){
                filterCondition += " " + AWSRelationalDatabaseSystem.AND + " ";
            }
            filterCondition += AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_TWITCH_ID + " = " + twitchId;
        }


        ResultSet result = getTwitchStreamersRecordBasedOnCriteria(getAllTwitchStreamersColumns(), filterCondition);
        while(result.next()){
            return getChannelFromResultSet(result);
        }
        return null;
    }


    public Channel addChannelDetails(String channelName,
                                     String twitchId,
                                     Boolean isListeningToChannel) throws Exception{
        if(ifGivenObjectIsValid(channelName)
                && ifGivenObjectIsValid(twitchId)
                && ifGivenObjectIsValid(isListeningToChannel)){
            Integer id = createTwitchStreamersRecord(channelName, twitchId, isListeningToChannel);
            return get(id);
        }
        return null;
    }

    public Boolean updateChannelDetails(Channel channel) throws Exception{
        if(!ifGivenObjectIsValid(channel.getId())){
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_ID.toString()
                + Constants.EQUALS + channel.getId();
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(channel.getChannelName())){
            data.put(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_NAME.toString(),
                    channel.getChannelName());
        }
        if(ifGivenObjectIsValid(channel.getTwitchId())){
            data.put(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString(),
                    channel.getTwitchId());
        }
        if(ifGivenObjectIsValid(channel.getIsListeningToChannel())){
            data.put(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString(),
                    channel.getIsListeningToChannel());
        }
        if(!data.isEmpty()){
            return updateTwitchStreamersRecord(data, filterCondition);
        }
        return false;
    }

    public Boolean updateChannelListenDetails(Integer channelId, Boolean isListeningToChannel) throws Exception{
        if(!ifGivenObjectIsValid(channelId)){
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_ID
                + Constants.EQUALS + channelId;
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(isListeningToChannel)){
            data.put(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString(),
                    isListeningToChannel);
        }
        if(!data.isEmpty()){
            return updateTwitchStreamersRecord(data, filterCondition);
        }
        return false;
    }

    protected ResultSet getTwitchStreamersRecordBasedOnCriteria(List<String> columnNames,
                                                                String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNamesStr = "";
        List<String> validColumnNames =  getAllTwitchStreamersColumns();
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && !columnNamesStr.trim().equals("")
                && whereCondition != null && !whereCondition.trim().equals("")){
            result = statement.executeQuery(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.
                    SELECT_RECORDS_WITH_WHERE.toString()
                    .replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    protected Integer createTwitchStreamersRecord(String name,
                                                  String twitchId,
                                                  Boolean isListeningToChannel) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(name != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.
                    TWITCH_STREAMERS.COLUMN_NAME.toString());
            values = buildColumnName(values, addStringLiteralToString(name));
        }
        if(twitchId != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.
                    TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString());
            values = buildColumnName(values, addStringLiteralToString(twitchId));
        }
        if(isListeningToChannel != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.
                    TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString());
            values = buildColumnName(values, addStringLiteralToString(isListeningToChannel.toString()));
        }
        ResultSet result = null;
        if(!columnNames.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.
                    TWITCH_STREAMERS.CREATE_RECORDS.toString()
                    .replace("{0}", columnNames)
                    .replace("{1}", values),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating Twitch Streamers failed, no ID obtained.");
            }
        }
        return null;
    }

    protected Boolean updateTwitchStreamersRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_NAME.toString())){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_NAME
                            + Constants.EQUALS + addStringLiteralToString(
                                    data.get(
                                            AWSRelationalDatabaseSystem.TWITCH_STREAMERS
                                                    .COLUMN_NAME.toString()).toString()));
        }
        if(data.has(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString())){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_TWITCH_ID
                            + Constants.EQUALS
                            + addStringLiteralToString(data.get(
                                    AWSRelationalDatabaseSystem.TWITCH_STREAMERS.
                                            COLUMN_TWITCH_ID.toString()).toString()));
        }
        if(data.has(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString())){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL
                            + Constants.EQUALS + addStringLiteralToString(data.get(
                                    AWSRelationalDatabaseSystem.TWITCH_STREAMERS
                                            .COLUMN_IS_LISTENING_TO_CHANNEL
                                            .toString()).toString()));
        }
        if(!columnNames.trim().equals("") && whereCondition != null
                && !whereCondition.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.TWITCH_STREAMERS
                    .UPDATE_RECORDS.toString()
                    .replace("{0}", columnNames)
                    .replace("{1}", whereCondition),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Updated.");
            }
            return true;
        }
        return false;
    }

    protected Boolean deleteTwitchStreamersRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result;
        if(whereCondition != null && !whereCondition.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.DELETE_RECORDS
                            .toString().replace("{0}", whereCondition),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting Twitch Streamers failed, no ID obtained.");
            }
        }
        return false;
    }


    protected List<String> getAllTwitchStreamersColumns(){
        List<String> columns = new ArrayList<>();
        AWSRelationalDatabaseSystem.TWITCH_STREAMERS[] twitchStreamersVal =
                AWSRelationalDatabaseSystem.TWITCH_STREAMERS.values();
        for(AWSRelationalDatabaseSystem.TWITCH_STREAMERS twitchStream : twitchStreamersVal){
            if(twitchStream.name().startsWith("COLUMN_")
                    && !twitchStream.name().equals("COLUMN_PRIMARY")){
                columns.add(twitchStream.toString());
            }
        }
        return columns;
    }

    private Channel getChannelFromResultSet(ResultSet result) throws Exception{
        return new Channel(result.getInt(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_ID.toString()),
                result.getString(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_NAME.toString()),
                result.getString(AWSRelationalDatabaseSystem.TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString()),
                Boolean.valueOf(result.getString(
                        AWSRelationalDatabaseSystem.TWITCH_STREAMERS
                                .COLUMN_IS_LISTENING_TO_CHANNEL.toString())));
    }

    private Boolean ifGivenObjectIsValid(Object data){
        if(data instanceof String){
            return !data.toString().trim().equals("");
        }else return data instanceof Integer || data instanceof Long || data instanceof Float
                || data instanceof Double || data instanceof Boolean;
    }

    public String addStringLiteralToString(String data){
        return "'" + data + "'";
    }

    protected String buildColumnName(String columnNames, Object currentColumnName){
        if(!columnNames.trim().equals("")){
            columnNames += ", ";
        }
        columnNames += currentColumnName;
        return columnNames;
    }
}
