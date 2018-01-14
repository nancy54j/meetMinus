package com.example.nancy.meetminus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.left.rightmesh.id.MeshID;

/**
 * Created by nancy on 2018-01-13.
 */

public class User {
    private int count = 0;
    private String name;
    private String username;
    private String email;
    private String number;
    private String password;
    public final String userID;
    private Map<User, String> friends = new HashMap<>();

    private MeshID meshID;
    private double Longitude;
    private double Latitude;

    public User(String name, String username, String email, String password, String number) {
        this.password = password;
        this.name = name;
        this.userID = count+"user";
        count++;
        this.username = username;
        this.email = email;
        this.number = number;

    }

    public Map<User, String> getFriends(){
        return friends;
    }

    public void setMeshID(MeshID id){
        this.meshID = id;
    }
    public String getUsername(){
        return username;
    }

    public MeshID getMeshID(){ return meshID; }

    public void setLongitude(double longitude){
        this.Longitude = longitude;

    }

    public void setLatitude(double latitude){
        this.Latitude = latitude;
    }

    public double getLongitude(){
        return this.Longitude;
    }

    public double getLatitude(){
        return this.Latitude;
    }

    public void addFriend(User user, String Category){

        //get the user from firebase
        this.friends.put(user, Category);
    }




}
