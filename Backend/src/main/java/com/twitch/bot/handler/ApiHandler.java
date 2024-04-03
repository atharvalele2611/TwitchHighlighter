package com.twitch.bot.handler;

import com.google.protobuf.Api;
import org.apache.http.client.methods.*;
import org.springframework.stereotype.Component;
import com.twitch.bot.utilites.TwitchData;

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
@DependsOn({"twitchData"})
public class ApiHandler {
    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());
    private final TokenHandler tokenHandler = new TokenHandler();
    private JSONObject params;
    private JSONObject headers;
    private JSONObject body;
    private String path;
    private boolean isConnectionRunning = false;
    private BufferedWriter twitch_writer;
    private BufferedReader twitch_reader;

    private final String domain = HTTPS + "api.twitch.tv";;
    private static final String HTTPS = "https://";
    private static final String SLASH = "/";

    public ApiHandler(ApiHandlerBuilder apiHandlerBuilder) {
        this.params = apiHandlerBuilder.params;
        this.headers = apiHandlerBuilder.headers;
        this.body = apiHandlerBuilder.body;
        this.path = apiHandlerBuilder.path;
    }

    public enum PATH {
        CONNECT("irc.twitch.tv", 6667),
        OAUTH_TOKEN("oauth2/token", 0),
        OAUTH_VALIDATE("oauth2/validate", 0),
        GET_USERS("helix/users", 0),
        GET_CHANNEL("helix/channels", 0),
        CLIPS("helix/clips", 0),
        GET_STREAMS("helix/streams", 0);

        private final String path;
        private final Integer ip;

        PATH(String path, Integer ip) {
            this.path = path;
            this.ip = ip;
        }

        public String getPath() {
            return path;
        }
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
                Socket socketConnection = new Socket(PATH.CONNECT.path, PATH.CONNECT.ip);
                this.twitch_writer = new BufferedWriter(new OutputStreamWriter(socketConnection.getOutputStream()));
                this.twitch_reader = new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));

                this.twitch_writer.write("PASS " + "oauth:" + tokenHandler.getAccessToken() + "\r\n");
                this.twitch_writer.write("NICK " + tokenHandler.getUserName() + "\r\n");
                this.twitch_writer.write("CAP REQ :twitch.tv/commands \r\n");
                this.twitch_writer.write("CAP REQ :twitch.tv/membership \r\n");
                this.twitch_writer.flush();

                String currentLine = "";
                while ((currentLine = this.twitch_reader.readLine()) != null) {
                    if (currentLine.indexOf("004") >= 0) {
                        LOG.log(Level.INFO, "Connected >> " + tokenHandler.getUserName() + " ~ irc.twitch.tv");
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
        HttpPost httpPost = new HttpPost(domain + SLASH + path);
        return createAndSendRequest(httpPost);
    }

    public String GET() throws Exception {
        tokenHandler.validateAndUpdateAccessToken();
        checkRequestQuality();
        HttpGet httpGet = new HttpGet(domain + SLASH + path);
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

    private void checkRequestQuality() throws Exception {
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

        if (!(headers.has("Content-Type") && path.equals(PATH.CONNECT.path))) {
            headers.put("Content-Type", "application/json");
        }
        if (!(headers.has("Authorization") && path.equals(PATH.CONNECT.path))) {
            headers.put("Authorization", "Bearer " + tokenHandler.getAccessToken());
        }
        if(headers.has("set_client_id")){
            headers.put(headers.getString("set_client_id"), tokenHandler.getClientId());
            headers.remove("set_client_id");
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