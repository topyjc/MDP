package com.mdp.server.service;

import com.mdp.server.client.DbClient;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final DbClient dbClient;

    public UserService(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public String getUsers() {
        return dbClient.getUsers();
    }
}