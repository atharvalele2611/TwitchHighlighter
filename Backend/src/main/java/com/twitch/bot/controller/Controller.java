package com.twitch.bot.controller;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.Service.SubscriptionService;
import com.twitch.bot.model.Channel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.twitch.bot.utilites.TwitchData;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;
import com.twitch.bot.Service.ChannelService;
import com.twitch.bot.Service.Connection;
import com.twitch.bot.Service.UserService;

@CrossOrigin
// ("http://localhost:5173/")
@RestController
@RequestMapping("/")
public class Controller {
    private static final Logger LOG = Logger.getLogger(Controller.class.getName());
    private final Connection twitchConnection;
    private final UserService userService;
    private final ChannelService channelService;
    private final SubscriptionService subscriptionService;
    HttpHeaders responseHeaders = new HttpHeaders();

    public Controller(Connection twitchConnection) {
        this.twitchConnection = twitchConnection;
        this.userService = twitchConnection.getUserService();
        this.channelService = twitchConnection.getChannelService();
        this.subscriptionService = twitchConnection.getSubscriptionService();
//        responseHeaders.add("Access-Control-Allow-Origin", "*");
//        responseHeaders.add("Access-Control-Allow-Methods", "DELETE,GET,POST,PUT,OPTIONS");
//        responseHeaders.add("Access-Control-Allow-Headers",
//                    "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

    }

    @GetMapping("/channels")
    public ResponseEntity<Object> getTwitchChannels(@RequestHeader Object userId) {
        Boolean isValidUser = null;
        try {
            isValidUser = userService.authenticateUser(Integer.parseInt(userId.toString()));
            if (!isValidUser) {
                return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(
                    twitchConnection.getAllChannels(userService.getUserDetails(Integer.parseInt(userId.toString()))),
                    responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception in /channels " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/twitch_analysis")
    public ResponseEntity<Object> getTwitchAnalysisData(@RequestParam("channel_name") String channelName) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("twitch_analysis", twitchConnection.getTwitchAnalysisOfAChannelInListOfHashmap(channelName));
        response.put("channel_name", channelName);
        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/channel_broadcastId")
    public ResponseEntity<Object> getChannelBroadcastId(@RequestParam("channel_name") String channelName) {
        try {
            return new ResponseEntity<>(channelService.getChannelBroadcasterId(channelName), responseHeaders,
                    HttpStatus.OK);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception in /channel_broadcastId " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/subscribeChannel")
    public ResponseEntity<Object> subscribeChannel(@RequestParam("channel_name") String channelName,
                                                   @RequestParam("user_id") Integer userId) {
        LOG.log(Level.INFO, "POST /addChannel {0}", channelName);
        try {
            channelName = channelName.toLowerCase();
            Channel newChannel = channelService.addChannel(channelName);
            channelService.joinChannel(channelName,twitchConnection);
            if(!subscriptionService.isUserSubscribedToChannel(userId,newChannel)){
                subscriptionService.addUserSubscriptions(userId,newChannel);
            }
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception in /addChannel " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/unSubscribeChannel")
    public ResponseEntity<Object> unSubscribeChannel(@RequestParam("channel_name") String channelName,
                                                     @RequestParam("user_id") Integer userId) {
        LOG.log(Level.INFO, "DELETE /removeChannel {0}", channelName);
        try {
            channelName = channelName.toLowerCase();
            twitchConnection.removeAndDeleteChannelData(channelName,userId);
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception in /removeChannel " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("user/authenticate")
    public ResponseEntity<HashMap<String, Object>> authenticateUser(@RequestBody HashMap<String,
            String> credentials) {
        LOG.log(Level.INFO, "POST /user/authenticate {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!(credentials.containsKey("username") || credentials.containsKey("email"))
                    || !credentials.containsKey("password")) {
                throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            Boolean isValidUser = (userName != null) ? userService.authenticateUser(userName,
                    password, true)
                    : userService.authenticateUser(email, password, false);
            if (isValidUser) {
                User user = (userName != null) ? userService.getUserDetails(userName, password, true)
                        : userService.getUserDetails(email, password, false);
                response.put("user_name", user.getName());
                response.put("email", user.getEmail());
                response.put("user_id", user.getUserId());
                return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
            }

        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "Exception in user/authenticate INVALID_BODY " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception in user/authenticate " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/register")
    public ResponseEntity<HashMap<String, Object>> register(@RequestBody HashMap<String,
            String> credentials) {
        LOG.log(Level.INFO, "POST /user/register {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!credentials.containsKey("username") || !credentials.containsKey("email")
                    || !credentials.containsKey("password")) {
                throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            User user = userService.registerUser(userName, password, email);
            response.put("user_name", user.getName());
            response.put("email", user.getEmail());
            response.put("user_id", user.getUserId());
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "Exception in user/register INVALID_BODY " + ex.getMessage());
            return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception in user/register " + ex.getMessage());
            ex.printStackTrace();
            if (ex.getMessage() != null && ex.getMessage().equals("User Already Present")) {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_ACCEPTABLE);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @GetMapping("user/subscriptions")
    public ResponseEntity<Object> getUserSubscriptions(@RequestHeader Object userId) {
        try {
            Boolean isValidUser = userService.authenticateUser(Integer.parseInt(userId.toString()));
            if (isValidUser) {
                User user = userService.getUserDetails(Integer.parseInt(userId.toString()));
                return new ResponseEntity<>(subscriptionService.getUserSubscribedChannels(user),
                        responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception in GET user/subscriptions " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/subscriptions")
    public ResponseEntity<Object> addUserSubscriptions(@RequestHeader Object userId,
            @RequestParam("channel_id") String channelId) {
        try {
            Subscriptions subscription = subscriptionService.
                    checkAndAddUserSubscriptions(Integer.parseInt(userId.toString()),
                    Integer.parseInt(channelId));
            if (subscription != null) {
                return new ResponseEntity<>(subscription, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception in POST user/subscriptions " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("user/subscriptions")
    public ResponseEntity<Object> deleteUserSubscriptions(@RequestHeader Object userId,
            @RequestParam("channel_id") String channelId) {
        try {
            Boolean isDeleteDone = subscriptionService
                    .checkAndDeleteUserSubscriptions(Integer.parseInt(userId.toString()),
                    Integer.parseInt(channelId));
            if (isDeleteDone) {
                return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception in DELETE user/subscriptions " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
