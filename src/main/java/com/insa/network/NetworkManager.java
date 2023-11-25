package com.insa.network;

import com.google.gson.GsonBuilder;
import com.insa.database.LocalDatabase;
import com.insa.utils.Constants;
import com.insa.utils.MyLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class NetworkManager {
    private static volatile NetworkManager instance;
    private UDPServer udpServer;
    private InetAddress myIP;
    private String myIPString;


    private NetworkManager() {
        udpServer = new UDPServer();
        udpServer.start();
//        this.myIPString = ip;
//        throw new UnsupportedOperationException();
    }

    public void notifyConnected(ConnectedUser user) {
        if (LocalDatabase.Database.connectedUserList.stream().noneMatch(u -> u.getUsername().equals(user.getUsername()))) {
            MyLogger.getInstance().info(String.format("Added user to connectedUserList: %s\n",
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(user))
            );
            LocalDatabase.Database.connectedUserList.add(user);
        } else {
            MyLogger.getInstance().info(String.format("User already in connectedUserList: %s\n",
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(user))
            );
        }
    }

    public void notifyDisconnected(ConnectedUser user) {
        //TODO check if it's not better to use user.equals(other user) ((uuid check))
        if (LocalDatabase.Database.connectedUserList.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()))) {
            MyLogger.getInstance().info(String.format("Removed user from connectedUserList: %s\n",
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(user))
            );
            boolean removed = LocalDatabase.Database.connectedUserList.removeIf(u -> u.getUsername().equals(user.getUsername()));

        } else {
            MyLogger.getInstance().info(String.format("User not found in connectedUserList: %s\n",
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(user))
            );
        }
    }

    //TODO notifychangeusername

    public void notifyChangeUsername(ConnectedUser user, String newUsername){
        if (LocalDatabase.Database.connectedUserList.stream().anyMatch(u -> u.getUsername().equals(newUsername))) {
            MyLogger.getInstance().info(String.format("Username already used in connectedUserList, has not been updated."));
        } else {
            boolean removed = LocalDatabase.Database.connectedUserList.removeIf(u -> u.getUsername().equals(user.getUsername()));
            if(removed) {
                LocalDatabase.Database.connectedUserList.add(new ConnectedUser(newUsername, user.getIP()));
            }

            MyLogger.getInstance().info(String.format("Username has been changed in connectedUserList: %s\n",
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(LocalDatabase.Database.connectedUserList))
            );
        }
    }

    public boolean discoverNetwork(String username) {
        boolean userInDB = false;

        MyLogger.getInstance().info("Begin client discovery");

        // Message creation
        UDPClient udpClient = new UDPClient();
        Message discoveryMessage = new Message();
        discoveryMessage.setType(Message.MessageType.DISCOVERY);
        discoveryMessage.setDate(new Date());
        discoveryMessage.setSender(new User(username));

        try {
            MyLogger.getInstance().info("Broadcast discovery sent");
            udpClient.sendBroadcast(discoveryMessage, Constants.UDP_SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MyLogger.getInstance().info("Waiting for responses");
        try {
            Thread.sleep(Constants.DISCOVERY_TIMEOUT);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        MyLogger.getInstance().info("Checking whether the username is already taken...");
        List<ConnectedUser> contactList = LocalDatabase.Database.connectedUserList;
        if (!contactList.isEmpty() && contactList.stream().anyMatch(u -> u.getUsername().equals(username))) {
                userInDB = true;
        }
        MyLogger.getInstance().info("Discovery finished");

        return userInDB;
    }

    public void sendChangeUsername(ConnectedUser user, String newUsername){

        MyLogger.getInstance().info(String.format("Change username to: %s Message sent.", newUsername));

        UDPClient udpClient = new UDPClient();
        Message changeUsernameMessage = new Message();
        changeUsernameMessage.setType(Message.MessageType.USERNAME_CHANGED);
        changeUsernameMessage.setSender(user);
        changeUsernameMessage.setDate(new Date());
        changeUsernameMessage.setContent(newUsername);

        try {
            udpClient.sendBroadcast(changeUsernameMessage, Constants.UDP_SERVER_PORT);
            LocalDatabase.Database.currentUser = new User(newUsername, LocalDatabase.Database.currentUser.getUuid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //HAS ALSO BEEN DONE : disconnection of a user
    public void sendDisconnection(ConnectedUser user) {

        MyLogger.getInstance().info("Disconnection message sent.");

        UDPClient udpClient = new UDPClient();
        Message disconnectedMessage = new Message();
        disconnectedMessage.setType(Message.MessageType.USER_DISCONNECTED);
        disconnectedMessage.setDate(new Date());
        disconnectedMessage.setSender(user);

        try {
            udpClient.sendBroadcast(disconnectedMessage, Constants.UDP_SERVER_PORT);
            LocalDatabase.Database.connectedUserList = new ArrayList<>();
            LocalDatabase.Database.currentUser = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static NetworkManager getInstance() {
        NetworkManager result = instance;
        if (result != null) {
            return result;
        }
        synchronized (NetworkManager.class) {
            if (instance == null) {
                instance = new NetworkManager();
            }
            return instance;
        }
    }
}
