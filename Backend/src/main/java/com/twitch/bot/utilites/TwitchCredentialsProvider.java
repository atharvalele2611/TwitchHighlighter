package com.twitch.bot.utilites;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

@Component
public class TwitchCredentialsProvider {
    private static final Logger LOG = Logger.getLogger(TwitchCredentialsProvider.class.getName());
    private final MongoClient mongoClient;

    public TwitchCredentialsProvider(){
        this.mongoClient = makeConnectionToDB();
    }

    public MongoClient makeConnectionToDB() {
        ConnectionString connectionString = new ConnectionString(Constants.MONGO_CONNECTION_URL);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        LOG.log(Level.INFO, "Connection to MongoDB Sucessful");
        return mongoClient;
    }

    public JSONObject getTwitchCredentials() {
        return getTwitchCredentialsFromMongoDB();
    }

    private JSONObject getTwitchCredentialsFromMongoDB() {
        MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DATABASE);
        MongoCollection<Document> collection = database.getCollection(Constants.MONGO_COLLECTION);
        JSONObject data = new JSONObject();
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            data.put(Constants.ACCESS_TOKEN, documentData.getString(Constants.ACCESS_TOKEN));
            data.put(Constants.REFRESH_TOKEN, documentData.getString(Constants.REFRESH_TOKEN));
            data.put(Constants.CLIENT_ID, documentData.getString(Constants.CLIENT_ID));
            data.put(Constants.CLIENT_SECRET, documentData.getString(Constants.CLIENT_SECRET));
            data.put(Constants.USER_NAME, documentData.getString(Constants.USER_NAME));
        }
        return data;
    }

    public Boolean setTwitchCredentials(JSONObject data) {
        return setTwitchCredentialsToMongoDB(data);
    }

    private Boolean setTwitchCredentialsToMongoDB(JSONObject data) {
        MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DATABASE);
        MongoCollection<Document> collection = database.getCollection(Constants.MONGO_COLLECTION);
        if (!data.has(Constants.CLIENT_ID)) {
            LOG.info("Client Id not present in given data ::: " + data);
            return false;
        }
        if (data.has(Constants.ACCESS_TOKEN)){
            collection.updateOne(eq(Constants.CLIENT_ID, data.get(Constants.CLIENT_ID).toString()),
                    Updates.set(Constants.ACCESS_TOKEN, data.get(Constants.ACCESS_TOKEN).toString()));
        }
        if (data.has(Constants.REFRESH_TOKEN)) {
            collection.updateOne(eq(Constants.CLIENT_ID, data.get(Constants.CLIENT_ID).toString()),
                    Updates.set(Constants.REFRESH_TOKEN, data.get(Constants.REFRESH_TOKEN).toString()));
        }
        return true;
    }
}
