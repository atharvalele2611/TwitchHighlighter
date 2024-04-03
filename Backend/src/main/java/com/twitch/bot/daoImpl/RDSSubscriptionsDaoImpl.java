package com.twitch.bot.daoImpl;

import com.twitch.bot.aws.AWSRelationalDatabaseSystem;
import com.twitch.bot.dao.RDSDao;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
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
public class RDSSubscriptionsDaoImpl implements RDSDao<Subscriptions> {
    private final Connection rdsConnection;

    public RDSSubscriptionsDaoImpl(AWSRelationalDatabaseSystem rdsConnection){
        this.rdsConnection = rdsConnection.getRdsConnection();
    }

    @Override
    public List<Subscriptions> getAll() throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                .SELECT_RECORDS_WITHOUT_WHERE
                .toString().replace("{0}", "*"));
        List<Subscriptions> subscriptions = new ArrayList<>();
        while(result.next()){
            subscriptions.add(getSubscriptionsFromResultSet(result));
        }
        statement.close();
        result.close();
        return subscriptions;
    }

    @Override
    public Subscriptions get(Integer id) throws Exception {
        String filterCondition;
        filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_PRIMARY
                + Constants.EQUALS + id;

        ResultSet result = getUsersSubscriptionRecordBasedOnCriteria(
                getAllUsersSubscriptionColumns(), filterCondition);
        while(result.next()){
            return getSubscriptionsFromResultSet(result);
        }
        return null;
    }

    @Override
    public Boolean delete(Subscriptions subscription) throws Exception {
        if(ifGivenObjectIsValid(subscription.getUserId()) && ifGivenObjectIsValid(subscription.getChannelId())){
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID
                + Constants.EQUALS + subscription.getUserId() + " "
                + AWSRelationalDatabaseSystem.AND + " "
                + AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID + Constants.EQUALS
                + subscription.getChannelId();
        return deleteUsersSubscriptionRecord(filterCondition);
    }

    public List<Subscriptions> getSubscriptionDetailsBasedOnUserOrSubscriptionId(Integer id, Boolean isUserId)
            throws Exception{
        String filterCondition;
        if(isUserId){
            filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID
                    + Constants.EQUALS + id;
        }else{
            filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID
                    + Constants.EQUALS + id;
        }

        List<Subscriptions> data = new ArrayList<>();
        ResultSet result = getUsersSubscriptionRecordBasedOnCriteria(
                getAllUsersSubscriptionColumns(), filterCondition);
        while(result.next()){
            data.add(getSubscriptionsFromResultSet(result));
        }
        return data;
    }

    public Boolean checkIfSubscriptionExists(Integer userId, Integer channelId) throws Exception {
        String filterCondition;
        filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID
                + Constants.EQUALS + userId;
        filterCondition += " " + AWSRelationalDatabaseSystem.AND + " "
                + AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID
                + Constants.EQUALS + channelId;
        ResultSet result = getUsersSubscriptionRecordBasedOnCriteria(
                getAllUsersSubscriptionColumns(), filterCondition);
        return result.next();
    }


    public Subscriptions addSubscriptionDetails(Integer userId, Channel channel) throws Exception{
        if(ifGivenObjectIsValid(userId) && ifGivenObjectIsValid(channel.getId())){
            Integer id = createUsersSubscriptionRecord(userId, channel.getId());
            return get(id);
        }
        return null;
    }


    public Boolean deleteSubscriptionDetailsForAChannel(Channel channel, Integer userId) throws Exception {
        String filterCondition = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID
                + Constants.EQUALS + channel.getId() + " " + AWSRelationalDatabaseSystem.AND + " "
                + AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID
                + Constants.EQUALS + userId;
        return deleteUsersSubscriptionRecord(filterCondition);
    }

    private ResultSet getUsersSubscriptionRecordBasedOnCriteria(List<String> columnNames,
                                                                  String whereCondition)
            throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNamesStr = "";
        List<String> validColumnNames =  getAllUsersSubscriptionColumns();
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && !columnNamesStr.trim().equals("")
                && whereCondition != null && !whereCondition.trim().equals("")){
            result = statement.executeQuery(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                    .SELECT_RECORDS_WITH_WHERE.toString()
                    .replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    private Integer createUsersSubscriptionRecord(Integer userId, Integer twitchId) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(userId > 0){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID.toString());
            values = buildColumnName(values, userId);
        }
        if(twitchId > 0){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString());
            values = buildColumnName(values, twitchId);
        }

        ResultSet result;
        if(!columnNames.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem
                    .USER_SUBSCRIPTION.CREATE_RECORDS.toString()
                    .replace("{0}", columnNames).replace("{1}", values),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating User Subscriptions failed, no ID obtained.");
            }
        }
        return null;
    }

    private Boolean updateUsersSubscriptionRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID.toString())
                && Integer.parseInt(
                        data.get(AWSRelationalDatabaseSystem
                                .USER_SUBSCRIPTION.COLUMN_USER_ID.toString()).toString()) > 0){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID
                            + Constants.EQUALS + Integer.parseInt(
                                    data.get(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                                            .COLUMN_USER_ID.toString()).toString()));
        }
        if(data.has(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString())
                && Integer.parseInt(
                        data.get(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                .COLUMN_TWITCH_STREAMERS_ID
                                .toString()).toString()) > 0){
            columnNames = buildColumnName(columnNames,
                    AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID
                            + Constants.EQUALS + Integer.parseInt(data.get(
                                    AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                                            .COLUMN_TWITCH_STREAMERS_ID
                                            .toString()).toString()));
        }
        if(!columnNames.trim().equals("") && whereCondition != null && !whereCondition.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
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

    private Boolean deleteUsersSubscriptionRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result;
        if(whereCondition != null && !whereCondition.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION
                    .DELETE_RECORDS.toString().replace("{0}", whereCondition),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting User Subscriptions failed, no ID obtained.");
            }
        }
        return false;
    }

    private List<String> getAllUsersSubscriptionColumns(){
        List<String> columns = new ArrayList<>();
        AWSRelationalDatabaseSystem.USER_SUBSCRIPTION[] userSubscriptionVal = AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.values();
        for(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION userSubscription : userSubscriptionVal){
            if(userSubscription.name().startsWith("COLUMN_") && !userSubscription.name().equals("COLUMN_PRIMARY")){
                columns.add(userSubscription.toString());
            }
        }
        return columns;
    }

    private Boolean ifGivenObjectIsValid(Object data){
        if(data instanceof String){
            return !data.toString().trim().equals("");
        }else return data instanceof Integer || data instanceof Long || data instanceof Float
                || data instanceof Double || data instanceof Boolean;
    }

    protected String buildColumnName(String columnNames, Object currentColumnName){
        if(!columnNames.trim().equals("")){
            columnNames += ", ";
        }
        columnNames += currentColumnName;
        return columnNames;
    }

    private Subscriptions getSubscriptionsFromResultSet(ResultSet result) throws Exception{
        return new Subscriptions(result.getInt(
                AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_USER_ID.toString()),
                result.getInt(AWSRelationalDatabaseSystem.USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID
                        .toString()));
    }
}
