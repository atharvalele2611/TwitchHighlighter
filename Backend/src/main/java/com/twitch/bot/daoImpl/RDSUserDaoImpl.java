package com.twitch.bot.daoImpl;

import com.twitch.bot.aws.AWSRelationalDatabaseSystem;
import com.twitch.bot.dao.RDSDao;
import com.twitch.bot.model.User;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class RDSUserDaoImpl implements RDSDao<User> {
    private final Connection rdsConnection;

    public RDSUserDaoImpl(AWSRelationalDatabaseSystem rdsConnection){
        this.rdsConnection = rdsConnection.getRdsConnection();
    }

    @Override
    public List<User> getAll() throws Exception {
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(AWSRelationalDatabaseSystem.USERS.
                SELECT_RECORDS_WITHOUT_WHERE
                .toString()
                .replace("{0}", "*"));
        List<User> users = new ArrayList<>();
        while(result.next()){
            users.add(getUserObjectFromResultSet(result));
        }
        return users;
    }

    @Override
    public User get(Integer userId) throws Exception {
        String filterCondition = "";
        filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_ID + " = " + userId;

        ResultSet result = getUsersRecordBasedOnCriteria(getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    @Override
    public Boolean delete(User user) throws Exception {
        if (!ifGivenObjectIsValid(user.getUserId())) {
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_ID.toString() + " = " + user.getUserId();
        return deleteUsersRecord(filterCondition);
    }


    public User getUserDetails(String emailOrName, String password, Boolean isName) throws Exception{
        String filterCondition = "";
        if(isName){
            filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_NAME
                    + " = " + addStringLiteralToString(emailOrName);
        }else{
            filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL
                    + " = " + addStringLiteralToString(emailOrName);
        }
        filterCondition += " " + AWSRelationalDatabaseSystem.AND + " "
                + AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD
                + " = " + addStringLiteralToString(password);

        ResultSet result = getUsersRecordBasedOnCriteria(getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    public User getUserDetails(String emailOrName, Boolean isName) throws Exception{
        String filterCondition = "";
        if(isName){
            filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_NAME
                    + " = " + addStringLiteralToString(emailOrName);
        }else{
            filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL
                    + " = " + addStringLiteralToString(emailOrName);
        }

        ResultSet result = getUsersRecordBasedOnCriteria(getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    public User addUserDetails(String name, String email, String password) throws Exception{
        if(ifGivenObjectIsValid(name) && ifGivenObjectIsValid(email) && ifGivenObjectIsValid(password)){
            Integer id = createUserRecord(name, email, password);
            return get(id);
        }
        return null;
    }

    public Boolean updateUserDetails(User user) throws Exception{
        if(!ifGivenObjectIsValid(user.getUserId())){
            return false;
        }
        String filterCondition = AWSRelationalDatabaseSystem.USERS.COLUMN_ID + " = " + user.getUserId();
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(user.getName())){
            data.put(AWSRelationalDatabaseSystem.USERS.COLUMN_NAME.toString(), user.getName());
        }
        if(ifGivenObjectIsValid(user.getEmail())){
            data.put(AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL.toString(), user.getName());
        }
        if(ifGivenObjectIsValid(user.getPassword())){
            data.put(AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD.toString(), user.getName());
        }
        if(!data.isEmpty()){
            return updateUsersRecord(data, filterCondition);
        }
        return false;
    }


    // Utility functions

    private Integer createUserRecord(String name, String email, String password) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(name != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_NAME.toString());
            values = buildColumnName(values, addStringLiteralToString(name));
        }
        if(email != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL.toString());
            values = buildColumnName(values, addStringLiteralToString(email));
        }
        if(password != null){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD.toString());
            values = buildColumnName(values, addStringLiteralToString(password));
        }
        ResultSet result = null;
        if(columnNames.trim() != ""){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.USERS.CREATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", values), Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }

        return null;
    }

    private Boolean deleteUsersRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = null;
        if(whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.USERS.DELETE_RECORDS.toString().replace("{0}", whereCondition), Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting user failed, no ID obtained.");
            }
        }
        return false;
    }

    private Boolean updateUsersRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(AWSRelationalDatabaseSystem.USERS.COLUMN_NAME.toString())){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_NAME
                    + " = " + addStringLiteralToString(data.get(AWSRelationalDatabaseSystem.USERS.COLUMN_NAME.toString()).toString()));
        }
        if(data.has(AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL.toString())){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL
                    + " = " + addStringLiteralToString(data.get(AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL.toString()).toString()));
        }
        if(data.has(AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD.toString())){
            columnNames = buildColumnName(columnNames, AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD
                    + " = " + addStringLiteralToString(data.get(AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD.toString()).toString()));
        }
        if(!columnNames.trim().equals("") && whereCondition != null && !whereCondition.trim().equals("")){
            int affectedRows =  statement.executeUpdate(AWSRelationalDatabaseSystem.USERS.UPDATE_RECORDS
                            .toString().replace("{0}", columnNames).replace("{1}", whereCondition),
                    Statement.RETURN_GENERATED_KEYS);

            if (affectedRows == 0) {
                throw new SQLException("No Rows Updated.");
            }
            return true;
        }
        return false;
    }

    private ResultSet getUsersRecordBasedOnCriteria(List<String> columnNames, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        List<String> validColumnNames =  getAllUsersColumns();
        String columnNamesStr = "";
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && !columnNamesStr.trim().equals("")
                && whereCondition != null && !whereCondition.trim().equals("")){
            result = statement.executeQuery(AWSRelationalDatabaseSystem.USERS
                    .SELECT_RECORDS_WITH_WHERE.toString()
                    .replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    private List<String> getAllUsersColumns(){
        List<String> columns = new ArrayList<>();
        AWSRelationalDatabaseSystem.USERS[] userVal = AWSRelationalDatabaseSystem.USERS.values();
        for(AWSRelationalDatabaseSystem.USERS user : userVal){
            if(user.name().startsWith("COLUMN_") && !user.name().equals("COLUMN_PRIMARY")){
                columns.add(user.toString());
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

    private User getUserObjectFromResultSet(ResultSet result) throws Exception{
        return new User(result.getInt(AWSRelationalDatabaseSystem.USERS.COLUMN_ID.toString()),
                result.getString(AWSRelationalDatabaseSystem.USERS.COLUMN_NAME.toString()),
                result.getString(AWSRelationalDatabaseSystem.USERS.COLUMN_EMAIL.toString()),
                result.getString(AWSRelationalDatabaseSystem.USERS.COLUMN_PASSWORD.toString()));
    }

    public String addStringLiteralToString(String data){
        return "'" + data + "'";
    }

    protected String buildColumnName(String columnNames, Object currentColumnName){
        if(columnNames.trim() != ""){
            columnNames += ", ";
        }
        columnNames += currentColumnName;
        return columnNames;
    }
}
