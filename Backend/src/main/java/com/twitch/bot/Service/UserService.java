package com.twitch.bot.Service;

import com.twitch.bot.daoImpl.RDSUserDaoImpl;
import com.twitch.bot.model.User;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.twitch.bot.utilites.RDSDaoProvider;

@Component
@DependsOn({"RDSUserDaoImpl"})
public class UserService {
    RDSUserDaoImpl rdsUserDao;
    public UserService(RDSUserDaoImpl rdsUserDao){
        this.rdsUserDao = rdsUserDao;
    }

    public Boolean authenticateUser(String username, String password, Boolean isUserName) throws Exception{
        User user = rdsUserDao.getUserDetails(username, password, isUserName);
        return (user != null);
    }

    public Boolean checkIfEmailOrUserNamePresent(String name, Boolean isUserName) throws Exception{
        User user = rdsUserDao.getUserDetails(name, isUserName);
        return (user != null);
    }

    public Boolean authenticateUser(Integer userId) throws Exception{
        User user = rdsUserDao.get(userId);
        return (user != null);
    }

    public User getUserDetails(String username, String password, Boolean isUserName) throws Exception{
        return rdsUserDao.getUserDetails(username, password, isUserName);
    }

    public User getUserDetails(Integer userId) throws Exception{
        return rdsUserDao.get(userId);
    }

    public User registerUser(String username, String password, String email) throws Exception{
        if(!checkIfEmailOrUserNamePresent(email, false)){
            return rdsUserDao.addUserDetails(username, email, password);
        }else{
            throw new Exception("User Already Present");
        }
    }

}
