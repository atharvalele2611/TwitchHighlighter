package com.twitch.bot.handler;

import com.twitch.bot.utilites.Constants;
import com.twitch.bot.utilites.TwitchCredentialsProvider;
import com.twitch.bot.utilites.TwitchData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@DependsOn({"twitchCredentialsProvider"})
public class TokenHandler {
    private static final Logger LOG = Logger.getLogger(TokenHandler.class.getName());
    private String accessToken;
    private String refreshToken;
    private final String clientId;
    private final String clientSecret;
    private final Integer connectionTimeOut = 5000;
    private final String userName;
    private final TwitchCredentialsProvider twitchCredentialsProvider;

    public TokenHandler() {
        this.twitchCredentialsProvider = new TwitchCredentialsProvider();
        JSONObject credentials = twitchCredentialsProvider.getTwitchCredentials();
        this.accessToken = credentials.getString(Constants.ACCESS_TOKEN);
        this.refreshToken = credentials.getString(Constants.REFRESH_TOKEN);
        this.clientId = credentials.getString(Constants.CLIENT_ID);
        this.clientSecret = credentials.getString(Constants.CLIENT_SECRET);
        this.userName = credentials.getString(Constants.USER_NAME);
    }

    public String validateAndUpdateAccessToken() throws Exception {
        String result = "";
        int status;
        HttpGet httpGet = new HttpGet(Constants.AUTHORIZATION_DOMAIN
                + Constants.SLASH + ApiHandler.PATH.OAUTH_VALIDATE.getPath());

        // Headers Part
        httpGet.setHeader(Constants.AUTHORIZATION, Constants.BEARER + accessToken);

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(300000).build();
        httpGet.setConfig(requestConfig);

        // httpGet.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            status = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());
        }

        if (status == 401) {
            String oauth_data = generateAccessToken();
            JSONObject oauth_json = new JSONObject(oauth_data);
            sendAccessTokenAndRefreshTokenToMongoDB(oauth_json.getString(Constants.ACCESS_TOKEN),
                    oauth_json.getString(Constants.REFRESH_TOKEN));
            result = new JSONObject().put("status", "OAUTH_UPDATED_SUCCESSFUL").toString();
        } else if (status == 200) {
            result = new JSONObject().put("status", "OAUTH_FETCHED_SUCCESSFUL").toString();
        } else {
            LOG.log(Level.SEVERE, "Issue in Oauth ::: status ::: " + status + " ::: response ::: " + result);
            throw new Exception("Issue in Oauth Generation");
        }
        return result;
    }

    private String generateAccessToken() throws Exception {
        String result = "";
        int status;
        HttpPost httpPost = new HttpPost(Constants.AUTHORIZATION_DOMAIN
                + Constants.SLASH + ApiHandler.PATH.OAUTH_TOKEN.getPath());

        // Headers Part
        httpPost.setHeader(Constants.CONTENT_TYPE, Constants.URL_ENCODED);

        URIBuilder uriBuilder = new URIBuilder(httpPost.getURI());
        httpPost.setURI(uriBuilder.build());

        // Body Part
        JSONObject oauth_body = new JSONObject();
        oauth_body.put(Constants.CLIENT_ID, clientId);
        oauth_body.put(Constants.CLIENT_SECRET, clientSecret);
        oauth_body.put(Constants.GRANT_TYPE, Constants.REFRESH_TOKEN);
        oauth_body.put(Constants.REFRESH_TOKEN, refreshToken);
        httpPost.setEntity(new StringEntity(jsonToHttpSupportedString(oauth_body)));

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(300000).build();
        httpPost.setConfig(requestConfig);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {

            status = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());
        }

        if (status != 200) {
            LOG.log(Level.SEVERE, "OAUTH TOKEN REFRESHING FAILED");
            throw new Exception("Oauth Generation Failed");
        }
        return result;
    }

    private void sendAccessTokenAndRefreshTokenToMongoDB(String accessToken, String refreshToken) {
        twitchCredentialsProvider.setTwitchCredentials(new JSONObject()
                .put(Constants.ACCESS_TOKEN, accessToken)
                .put(Constants.REFRESH_TOKEN, refreshToken)
                .put(Constants.CLIENT_ID, clientId));
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String jsonToHttpSupportedString(JSONObject json) {
        String result = "";
        Iterator<String> jsonIter = json.keys();
        while (jsonIter.hasNext()) {
            String key = jsonIter.next();
            result += key + "=" + json.get(key).toString();
            if (jsonIter.hasNext()) {
                result += "&";
            }
        }
        return result;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserName() {
        return userName;
    }
}
