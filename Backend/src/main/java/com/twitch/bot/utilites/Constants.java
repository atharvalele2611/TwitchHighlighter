package com.twitch.bot.utilites;

public class Constants {

    // Database Credentials
    public static final String MONGO_CONNECTION_URL =
            "mongodb+srv://twitch:twitch@twitch.vc4bbvf.mongodb.net/?retryWrites=true&w=majority";
    public static final String MONGO_DATABASE = "twitch";
    public static final String MONGO_COLLECTION = "credentials";

    // Twitch TOKEN
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USER_NAME = "user_name";
    public static final String LOGIN = "login";
    public static final String STATUS = "status";
    public static final String USER_LOGIN = "user_login";
    public static final String GRANT_TYPE = "grant_type";
    public static final String OAUTH_UPDATED_SUCCESSFUL = "OAUTH_UPDATED_SUCCESSFUL";

    // AWS Credentials
    public static final String ACCESS_KEY = "access_key";
    public static final String ACCESS_ID = "access_id";
    public static final String ACCESS_KEY_VALUE = "fGy9eD0l2p5e1/8E444QoDY++7a7WFIoUqFinyyX";
    public static final String ACCESS_ID_VALUE = "AKIA5MEYDMG5YPMA7QFU";

    // Twitch Parameters
    public static final String ID = "id";
    public static final String CHANNEL_NAME = "channel_name";
    public static final String TIMESTAMP = "timestamp";
    public static final String SENTIMENTAL_CLIP_COLLECTIONS = "sentimentalClipsCollection";
    public static final String TWITCH_ID = "twitch_id";
    public static final String BROADCASTER_ID = "broadcaster_id";
    public static final String IS_USER_SUBSCRIBED = "is_user_subscribed";
    public static final String DATA = "data";
    public static final String TYPE = "type";
    public static final String LIVE = "live";
    public static final String IS_CHANNEL_LIVE = "is_channel_live";

    public static final String CLIP_ID = "clip_id";
    public static final String CLIP_DETAILS = "clip_details";
    public static final String CREATED_AT = "created_at";
    public static final String STARTED_AT = "started_at";
    public static final String STREAM_STARTED_AT = "stream_started_at";
    public static final String EMBED_URL = "embed_url";
    public static final String VIDEO_URL = "video_url";
    public static final String URL = "url";
    public static final String THUMBNAIL_URL = "thumbnail_url";
    public static final String SENTIMENTAL_ANALYSIS = "sentimental_analysis";

    // Twitch Socket Communication
    public static final String PING = "ping";
    public static final String PONG = "PONG ";
    public static final String PRIVMSG = "PRIVMSG";
    public static final String DISCONNECTED = "disconnected";
    public static final String JOIN = "JOIN ";
    public static final String PART = "PART ";
    public static final String NICK = "NICK ";
    public static final String PASS_OAUTH = "PASS oauth:";
    public static final String CAP_REQ_TWITCH_TV_COMMANDS = "CAP REQ :twitch.tv/commands \r\n";
    public static final String CAP_REQ_TWITCH_TV_MEMBERSHIP = "CAP REQ :twitch.tv/membership \r\n";
    public static final String CONNECTED_004 = "004";
    public static final String HTTPS = "https://";
    public static final String SLASH = "/";

    // DOMAINS
    public static final String AUTHORIZATION_DOMAIN = HTTPS + "id.twitch.tv";
    public static final String API_TWTICH_TV_DOMAIN = HTTPS + "api.twitch.tv";
    public static final String CONNECT_DOMAIN = "irc.twitch.tv";
    public static final String OAUTH2_TOKEN_DOMAIN = "oauth2/token";
    public static final String OAUTH2_VALIDATION_DOMAIN = "oauth2/validate";
    public static final String USERS_DOMAIN = "helix/users";
    public static final String CHANNELS_DOMAIN = "helix/channels";
    public static final String CLIPS_DOMAIN = "helix/clips";
    public static final String STREAMS_DOMAIN = "helix/streams";

    // Request Header and Paramters
    public static final String SET_CLIENT_ID = "set_client_id";
    public static final String CLIENT_ID_HEADER = "Client-Id";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_JSON = "application/json";

    // RDS Constants
    public static final String EQUALS = " = ";
    public static final String AND = "AND";
    public static final String RDS_DB_NAME = "twitchdb";
    public static final String RDS_USERNAME = "root";
    public static final String RDS_PASSWORD = "root1234";
    public static final String RDS_HOSTNAME = "twitchdb.cc3vhgvflzt3.us-east-1.rds.amazonaws.com";
    public static final String RDS_PORT = "3306";
    public static final String SHOW_DATABASES = "SHOW DATABASES";
    public static final String CREATE_DATABASES = "CREATE DATABASES ";
    public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";



    // Misc
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String UTC = "UTC";


}
