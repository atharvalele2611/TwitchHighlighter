package com.twitch.bot.dao;

import java.util.List;

public interface RDSDao<T> {

    List<T> getAll() throws Exception;

    T get(Integer id) throws Exception;

    Boolean delete(T t) throws Exception;
}
