package com.twitch.bot.aws;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class AWSRelationalDatabaseSystem {
    private static final Logger LOG = Logger.getLogger(AWSRelationalDatabaseSystem.class.getName());
    public static Connection rdsConnection;
    public static final String AND = "AND";
    private static final String RDS_DB_NAME = "twitchdb";
    private static final String RDS_USERNAME = "root";
    private static final String RDS_PASSWORD = "root1234";
    private static final String RDS_HOSTNAME = "twitchdb.cc3vhgvflzt3.us-east-1.rds.amazonaws.com";
    private static final String RDS_PORT = "3306";

    public enum USERS{
        TABLENAME("Users"),
        COLUMN_ID("USER_ID"),
        COLUMN_NAME("NAME"),
        COLUMN_EMAIL("EMAIL"),
        COLUMN_PASSWORD("PASSWORD"),
        COLUMN_PRIMARY("USER_ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString() + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_NAME.toString() + " VARCHAR(255), " + COLUMN_EMAIL.toString() + " VARCHAR(255), " + COLUMN_PASSWORD.toString() + " VARCHAR(255), PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        USERS(String attributeName){
            this.attributeName = attributeName;
        }       
        
        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public enum TWITCH_STREAMERS{
        TABLENAME("Twitch_Streamers"),
        COLUMN_ID("ID"),
        COLUMN_NAME("NAME"),
        COLUMN_TWITCH_ID("TWITCH_ID"),
        COLUMN_IS_LISTENING_TO_CHANNEL("IS_LISTENING_TO_CHANNEL"),
        COLUMN_PRIMARY("ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString() + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_NAME.toString() + " VARCHAR(255), " + COLUMN_TWITCH_ID.toString() + " VARCHAR(255), " + COLUMN_IS_LISTENING_TO_CHANNEL.toString() + " VARCHAR(255), PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        TWITCH_STREAMERS(String attributeName){
            this.attributeName = attributeName;
        }        

        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public enum USER_SUBSCRIPTION{
        TABLENAME("User_Subscription"),
        COLUMN_ID("ID"),
        COLUMN_USER_ID("USER_ID"),
        COLUMN_TWITCH_STREAMERS_ID("TWITCH_STREAMERS_ID"),
        COLUMN_PRIMARY("ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString()  + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_USER_ID.toString() + " INTEGER not NULL, " + COLUMN_TWITCH_STREAMERS_ID.toString() + " INTEGER not NULL, FOREIGN KEY(" + COLUMN_USER_ID.toString() + ") references " + USERS.TABLENAME.toString() + "(" + USERS.COLUMN_ID.toString() + "),  FOREIGN KEY(" + COLUMN_TWITCH_STREAMERS_ID.toString() + ") references " + TWITCH_STREAMERS.TABLENAME.toString() + "(" + TWITCH_STREAMERS.COLUMN_ID.toString() + ")" + ", PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        USER_SUBSCRIPTION(String attributeName){
            this.attributeName = attributeName;
        }
        
        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public AWSRelationalDatabaseSystem() throws Exception{
        rdsConnection = make_RDS_JDBC_Connection();
        checkAndCreateTables();
    }

    public Connection getRdsConnection() {
        return rdsConnection;
    }

    private Connection make_RDS_JDBC_Connection() throws Exception{
        LOG.log(Level.INFO, "Getting remote connection with connection string from environment variables");
        Class.forName("com.mysql.cj.jdbc.Driver");
        checkAndCreateDatabase(RDS_DB_NAME);
        final String jdbcUrl = "jdbc:mysql://" + RDS_HOSTNAME + ":" + RDS_PORT
                + "/" + RDS_DB_NAME + "?user=" + RDS_USERNAME
                + "&password=" + RDS_PASSWORD;
        Connection connection = DriverManager.getConnection(jdbcUrl);
        LOG.log(Level.INFO, "RDS JDBC Connection Successful");
        return connection;
    }

    public void checkAndCreateDatabase(String dbName){
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String myConnectionString = "jdbc:mysql://" + RDS_HOSTNAME + ":" + RDS_PORT + "?" +
                        "useUnicode=yes&characterEncoding=UTF-8";
                Connection connection = DriverManager.getConnection(myConnectionString, RDS_USERNAME, RDS_PASSWORD);
                boolean isDbPresent = false;
                Statement stmt = connection.createStatement();
                stmt.execute("SHOW DATABASES");
                ResultSet rs = stmt.getResultSet();
                while (rs.next()) {
                    if(dbName.equals(rs.getString(1))){
                        isDbPresent = true;
                    }
                }
                if(!isDbPresent){
                    stmt = connection.createStatement();
                    stmt.execute("CREATE DATABASE " + dbName);
                }
                rs.close();
                stmt.close();
            } catch (ClassNotFoundException | SQLException ex) {
                LOG.log(Level.INFO, "Exception ::: " + ex.getMessage());
            }
    }

    private void checkAndCreateTables() throws Exception{
        DatabaseMetaData meta = rdsConnection.getMetaData();
        List<String> tables = getRDSDbTableNames();
        List<String> existingTables = new ArrayList<>();
        for(String tableName: tables){
            ResultSet resultSet = meta.getTables(null, null, tableName, new String[] {"TABLE"});
            if(resultSet.next()){
                existingTables.add(tableName);
            }
        }
        tables.removeAll(existingTables);
        if(!tables.isEmpty()){
            LOG.log(Level.SEVERE, "Tables Not Found In RDS ::: "+ tables.toString());
            for (String tableName : tables) {
                LOG.log(Level.INFO, "Creating Table " + tableName);
                createTable(tableName);
            }
        }
    }

    private void createTable(String tableName) throws Exception{
        Statement statement = rdsConnection.createStatement();
        try{
            statement.executeUpdate(getCreateTableQueryBasedOnTableName(tableName));
        }catch(SQLSyntaxErrorException ex){
            LOG.log(Level.INFO, "Exception ::: " + ex);
            throw ex;
        }
        LOG.log(Level.INFO, "Table {0} Created in RDS", new Object[]{tableName});
    }

    private static String getCreateTableQueryBasedOnTableName(String tableName){
        if(USERS.TABLENAME.toString().equals(tableName)){
            return USERS.CREATE_TABLE.toString();
        }else if(TWITCH_STREAMERS.TABLENAME.toString().equals(tableName)){
            return TWITCH_STREAMERS.CREATE_TABLE.toString();
        }else if(USER_SUBSCRIPTION.TABLENAME.toString().equals(tableName)){
            return USER_SUBSCRIPTION.CREATE_TABLE.toString();
        }
        return null;
    }

    private static List<String> getRDSDbTableNames() {
        List<String> tableNames = new ArrayList<>();
        tableNames.add(USERS.TABLENAME.toString());
        tableNames.add(TWITCH_STREAMERS.TABLENAME.toString());
        tableNames.add(USER_SUBSCRIPTION.TABLENAME.toString());
        return tableNames;
    }

}
