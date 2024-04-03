package com.twitch.bot.model;

public class User {
    private final Integer userId;
    private final String name;
    private final String email;
    private final String password;

    public User(Integer userId, String name, String email, String password){
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
