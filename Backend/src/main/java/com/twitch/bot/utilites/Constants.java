package com.twitch.bot.utilites;

public class Constants {

    public static final String MONGO_CONNECTION_URL =
            "mongodb+srv://twitch:twitch@twitch.vc4bbvf.mongodb.net/?retryWrites=true&w=majority";
    public static final String MONGO_DATABASE = "twitch";
    public static final String MONGO_COLLECTION = "credentials";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USER_NAME = "user_name";
    public static final String LOGIN = "login";
    public static final String GRANT_TYPE = "grant_type";

    public static final String ACCESS_KEY = "access_key";
    public static final String ACCESS_ID = "access_id";
    public static final String ACCESS_KEY_VALUE = "fGy9eD0l2p5e1/8E444QoDY++7a7WFIoUqFinyyX";
    public static final String ACCESS_ID_VALUE = "AKIA5MEYDMG5YPMA7QFU";

    public static final String ID = "id";
    public static final String CHANNEL_NAME = "channel_name";
    public static final String TIMESTAMP = "timestamp";
    public static final String SENTIMENTAL_CLIP_COLLECTIONS = "sentimentalClipsCollection";
    public static final String TWITCH_ID = "twitch_id";
    public static final String BROADCASTER_ID = "broadcaster_id";
    public static final String IS_USER_SUBSCRIBED = "is_user_subscribed";
    public static final String DATA = "data";
    public static final String IS_CHANNEL_LIVE = "is_channel_live";

    public static final String CLIP_ID = "clip_id";
    public static final String CLIP_DETAILS = "clip_details";
    public static final String CREATED_AT = "created_at";
    public static final String EMBED_URL = "embed_url";
    public static final String VIDEO_URL = "video_url";
    public static final String URL = "url";
    public static final String THUMBNAIL_URL = "thumbnail_url";
    public static final String SENTIMENTAL_ANALYSIS = "sentimental_analysis";

    public static final String PING = "ping";
    public static final String PONG = "PONG ";
    public static final String PRIVMSG = "PRIVMSG";
    public static final String DISCONNECTED = "disconnected";
    public static final String JOIN = "JOIN ";
    public static final String PART = "PART ";

    // Request Header
    public static final String SET_CLIENT_ID = "set_client_id";
    public static final String CLIENT_ID_HEADER = "Client-Id";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String URL_ENCODED = "application/x-www-form-urlencoded";

    public static final String HTTPS = "https://";
    public static final String SLASH = "/";
    public static final String AUTHORIZATION_DOMAIN = HTTPS + "id.twitch.tv";


}
