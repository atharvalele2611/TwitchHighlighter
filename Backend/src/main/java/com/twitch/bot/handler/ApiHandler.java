package com.twitch.bot.handler;

import com.twitch.bot.utilites.Constants;
import org.apache.http.client.methods.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.context.annotation.DependsOn;

@Component
public class ApiHandler {
    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());
    private static final TokenHandler tokenHandler = new TokenHandler();
    private JSONObject params;
    private JSONObject headers;
    private JSONObject body;
    private String path;
    private boolean isConnectionRunning = false;
    private static BufferedWriter twitch_writer;
    private static BufferedReader twitch_reader;

    public ApiHandler(ApiHandlerBuilder apiHandlerBuilder) {
        this.params = apiHandlerBuilder.params;
        this.headers = apiHandlerBuilder.headers;
        this.body = apiHandlerBuilder.body;
        this.path = apiHandlerBuilder.path;
    }

    public enum DOMAIN {
        CONNECT(Constants.CONNECT_DOMAIN, 6667),
        OAUTH_TOKEN(Constants.OAUTH2_TOKEN_DOMAIN, 0),
        OAUTH_VALIDATE(Constants.OAUTH2_VALIDATION_DOMAIN, 0),
        GET_USERS(Constants.USERS_DOMAIN, 0),
        GET_CHANNEL(Constants.CHANNELS_DOMAIN, 0),
        CLIPS(Constants.CLIPS_DOMAIN, 0),
        GET_STREAMS(Constants.STREAMS_DOMAIN, 0);

        private final String domain;
        private final Integer ip;

        DOMAIN(String domain, Integer ip) {
            this.domain = domain;
            this.ip = ip;
        }

        public String getDomain() {
            return domain;
        }

        public Integer getIp() {return ip;}
    }

    public BufferedWriter getTwitch_writer() {
        return twitch_writer;
    }

    public BufferedReader getTwitch_reader() {
        return twitch_reader;
    }

    public boolean CONNECT() throws Exception {
        if (!isConnectionEstablishedAndRunning()) {
            tokenHandler.validateAndUpdateAccessToken();
            try {
                @SuppressWarnings("resource")
                Socket socketConnection = new Socket(DOMAIN.CONNECT.getDomain(), DOMAIN.CONNECT.getIp());
                this.twitch_writer = new BufferedWriter(
                        new OutputStreamWriter(socketConnection.getOutputStream()));
                this.twitch_reader = new BufferedReader(
                        new InputStreamReader(socketConnection.getInputStream()));

                this.twitch_writer.write(Constants.PASS_OAUTH + tokenHandler.getAccessToken() + "\r\n");
                this.twitch_writer.write(Constants.NICK + tokenHandler.getUserName() + "\r\n");
                this.twitch_writer.write(Constants.CAP_REQ_TWITCH_TV_COMMANDS);
                this.twitch_writer.write(Constants.CAP_REQ_TWITCH_TV_MEMBERSHIP);
                this.twitch_writer.flush();

                String currentLine = "";
                while ((currentLine = this.twitch_reader.readLine()) != null) {
                    if (currentLine.contains(Constants.CONNECTED_004)) {
                        LOG.log(Level.INFO,
                                "Connected >> " + tokenHandler.getUserName()
                                        + " ~ irc.twitch.tv");
                        break;
                    } else {
                        LOG.log(Level.INFO, currentLine);
                    }
                }
                isConnectionRunning = true;
                return isConnectionEstablishedAndRunning();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Exception is " + ex);
            }
        }
        return isConnectionEstablishedAndRunning();
    }

    public boolean isConnectionEstablishedAndRunning() {
        return isConnectionRunning;
    }

    public String POST() throws Exception {
        tokenHandler.validateAndUpdateAccessToken();
        checkRequestQuality();
        HttpPost httpPost = new HttpPost(Constants.API_TWTICH_TV_DOMAIN + Constants.SLASH + path);
        return createAndSendRequest(httpPost);
    }

    public String GET() throws Exception {
        tokenHandler.validateAndUpdateAccessToken();
        checkRequestQuality();
        HttpGet httpGet = new HttpGet(Constants.API_TWTICH_TV_DOMAIN + Constants.SLASH + path);
        return createAndSendRequest(httpGet);
    }

    private String createAndSendRequest(HttpRequestBase requestBase) throws IOException, URISyntaxException {
        String result = "";
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                requestBase.setHeader(key, headers.getString(key));
            }
        }
        // parameters part
        URIBuilder uriBuilder = new URIBuilder(requestBase.getURI());
        for (String key : params.keySet()) {
            uriBuilder.addParameter(key, params.getString(key));
        }
        // Timeout Part
        int socketTimeOut = 5000;
        int connectionTimeOut = 5000;
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(socketTimeOut).build();
        requestBase.setConfig(requestConfig);

        requestBase.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(requestBase)) {

            result = EntityUtils.toString(response.getEntity());
        }

        clear();
        return result;
    }

    private void checkRequestQuality() {
        if (params == null) {
            params = new JSONObject();
        }
        if (headers == null) {
            headers = new JSONObject();
        }
        if (body == null) {
            body = new JSONObject();
        }
        if (path == null) {
            path = "";
        }

        if (!(headers.has(Constants.CONTENT_TYPE) && path.equals(DOMAIN.CONNECT.getDomain()))) {
            headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        }
        if (!(headers.has(Constants.AUTHORIZATION) && path.equals(DOMAIN.CONNECT.getDomain()))) {
            headers.put(Constants.AUTHORIZATION, Constants.BEARER + tokenHandler.getAccessToken());
        }
        if(headers.has(Constants.SET_CLIENT_ID)){
            headers.put(headers.getString(Constants.SET_CLIENT_ID), tokenHandler.getClientId());
//            headers.remove(Constants.SET_CLIENT_ID);
        }
    }

    private void clear() {
        params = null;
        headers = null;
        body = null;
        path = null;
    }

    @Component
    public static class ApiHandlerBuilder {
        private JSONObject params;
        private JSONObject headers;
        private JSONObject body;
        private String path;

        public ApiHandlerBuilder setParams(JSONObject params){
            this.params = params;
            return this;
        }

        public ApiHandlerBuilder setHeaders(JSONObject headers){
            this.headers = headers;
            return this;
        }

        public ApiHandlerBuilder setBody(JSONObject bod){
            this.body = body;
            return this;
        }
        public ApiHandlerBuilder setPath(String path){
            this.path = path;
            return this;
        }

        public ApiHandler build(){
            return new ApiHandler(this);
        }
    }
}