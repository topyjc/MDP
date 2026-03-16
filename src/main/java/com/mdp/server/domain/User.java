package com.mdp.server.domain;

@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

}