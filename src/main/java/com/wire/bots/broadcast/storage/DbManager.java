//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
package com.wire.bots.broadcast.storage;

import com.wire.bots.broadcast.model.Broadcast;
import com.wire.bots.broadcast.model.Message;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.server.model.NewBot;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class DbManager {
    private final String path;

    public DbManager(String path) {
        this.path = path;

        File dir = new File(path);
        dir.mkdirs();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();

            int update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS Message " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " BotId STRING," +
                    " UserId STRING," +
                    " Text STRING," +
                    " Asset_key STRING," +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");
            if (update > 0)
                Logger.info("CREATED TABLE Message");

            update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS Broadcast " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " Text STRING," +
                    " Asset BLOB," +
                    " Url STRING," +
                    " Title STRING," +
                    " Asset_key STRING," +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");
            if (update > 0)
                Logger.info("CREATED TABLE Broadcast");

            update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS User " +
                    "(BotId STRING PRIMARY KEY," +
                    " UserId STRING," +
                    " Locale STRING," +
                    " Name STRING," +
                    " Time INTEGER)");

            if (update > 0)
                Logger.info("CREATED TABLE User");

        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    public int insertMessage(Message msg) throws SQLException {
        try (Connection connection = getConnection()) {

            String cmd = "INSERT INTO Message " +
                    "(BotId, UserId, Text, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, msg.getBotId());
            stm.setString(2, msg.getUserId());
            stm.setString(3, msg.getText());
            stm.setString(4, msg.getAssetKey());
            stm.setString(5, msg.getToken());
            stm.setBytes(6, msg.getOtrKey());
            stm.setString(7, msg.getMimeType());
            stm.setLong(8, msg.getSize());
            stm.setBytes(9, msg.getSha256());
            stm.setLong(10, new Date().getTime() / 1000);

            return stm.executeUpdate();
        }
    }

    public ArrayList<Message> getMessages() throws SQLException {
        ArrayList<Message> ret = new ArrayList<>();

        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM Message");
            while (rs.next()) {
                Message msg = new Message();
                msg.setBotId(rs.getString("BotId"));
                msg.setUserId(rs.getString("UserId"));
                msg.setText(rs.getString("Text"));
                msg.setAssetKey(rs.getString("Asset_key"));
                msg.setToken(rs.getString("Token"));
                msg.setTime(rs.getInt("Time"));
                ret.add(msg);
            }
        }
        return ret;
    }

    public int insertBroadcast(String text) throws SQLException {
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();

            String cmd = String.format("INSERT INTO Broadcast " +
                            "(Text, Time) " +
                            "VALUES('%s', %d)",
                    text,
                    new Date().getTime() / 1000);

            return statement.executeUpdate(cmd);
        }
    }

    public int insertBroadcast(Broadcast broadcast) throws SQLException {
        try (Connection connection = getConnection()) {

            String cmd = "INSERT INTO Broadcast " +
                    "(Text, Asset, Url, Title, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, broadcast.getText());
            stm.setBytes(2, broadcast.getAssetData());
            stm.setString(3, broadcast.getUrl());
            stm.setString(4, broadcast.getTitle());
            stm.setString(5, broadcast.getAssetKey());
            stm.setString(6, broadcast.getToken());
            stm.setBytes(7, broadcast.getOtrKey());
            stm.setString(8, broadcast.getMimeType());
            stm.setLong(9, broadcast.getSize());
            stm.setBytes(10, broadcast.getSha256());
            stm.setLong(11, new Date().getTime() / 1000);

            return stm.executeUpdate();
        }
    }

    public ArrayList<Broadcast> getBroadcast(long from) throws SQLException {
        ArrayList<Broadcast> ret = new ArrayList<>();

        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM Broadcast WHERE Time > " + from);
            while (rs.next()) {
                Broadcast b = new Broadcast();
                b.setId(rs.getInt("Id"));
                b.setText(rs.getString("Text"));
                b.setUrl(rs.getString("Url"));
                b.setTitle(rs.getString("Title"));
                b.setAssetKey(rs.getString("Asset_key"));
                b.setToken(rs.getString("Token"));
                b.setAssetData(rs.getBytes("Asset"));
                b.setOtrKey(rs.getBytes("Otr_key"));
                b.setSha256(rs.getBytes("Sha256"));
                b.setSize(rs.getInt("Size"));
                b.setMimeType(rs.getString("Mime_type"));
                b.setTime(rs.getInt("Time"));
                ret.add(b);
            }
        }
        return ret;
    }

    public int insertUser(NewBot newBot) throws SQLException {
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();

            String cmd = String.format("INSERT INTO User " +
                            "(BotId, UserId, Locale , Time, Name) " +
                            "VALUES('%s', '%s', '%s', '%s', %d)",
                    newBot.id,
                    newBot.origin.id,
                    newBot.locale,
                    newBot.origin.name,
                    new Date().getTime() / 1000
            );

            return statement.executeUpdate(cmd);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
    }


}
