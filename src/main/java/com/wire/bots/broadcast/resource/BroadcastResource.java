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
package com.wire.bots.broadcast.resource;

import com.wire.bots.broadcast.Executor;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

@Path("/broadcast")
public class BroadcastResource {
    private final ClientRepo repo;
    private final Config conf;
    private final DbManager dbManager;

    public BroadcastResource(ClientRepo repo, Config conf) {
        this.repo = repo;
        this.conf = conf;
        dbManager = new DbManager(conf.getDatabase());
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    @GET
    public Response broadcast(@QueryParam("text") final String text) throws Exception {
        if (text == null) {
            return Response.
                    ok("Nothing to broadcast").
                    status(200).
                    build();
        }

        File[] botDirs = getCryptoDirs();

        if (botDirs.length == 0)
            return Response.
                    ok("No subscribers yet :(").
                    status(200).
                    build();

        try {
            Executor exec = new Executor(repo, dbManager);

            if (isPicture(text)) {
                Picture picture = new Picture(text);

                WireClient wireClient = repo.getWireClient(botDirs[0].getName());
                AssetKey assetKey = wireClient.uploadAsset(picture);
                picture.setAssetKey(assetKey.key);
                picture.setAssetToken(assetKey.token);

                exec.broadcastPicture(picture, botDirs);
            } else if (isUrl(text)) {
                exec.broadcastUrl(text, botDirs);
            } else {
                exec.broadcastText(text, botDirs);
            }

            return Response.
                    ok(String.format("Scheduled broadcast for %,d conversations", botDirs.length)).
                    status(201).
                    build();

        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
            return Response.
                    ok(String.format("Something went terribly wrong: %s", e.getLocalizedMessage())).
                    status(500).
                    build();
        }
    }

    public void broadcast(ImageMessage msg, byte[] imgData) throws Exception {
        Picture picture = new Picture(imgData, msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());

        Executor executor = new Executor(repo, dbManager);
        executor.broadcastPicture(picture, getCryptoDirs());
    }

    public void forwardFeedback(TextMessage msg) throws Exception {
        String botId = conf.getFeedback();
        if (botId == null)
            return;

        WireClient feedbackClient = repo.getWireClient(botId);
        ArrayList<String> ids = new ArrayList<>();
        ids.add(msg.getUserId());
        for (User user : feedbackClient.getUsers(ids)) {
            String feedback = String.format("**%s** wrote: _%s_", user.name, msg.getText());
            feedbackClient.sendText(feedback);
        }
    }

    public void forwardFeedback(ImageMessage msg) throws Exception {
        String botId = conf.getFeedback();
        if (botId == null)
            return;

        WireClient feedbackClient = repo.getWireClient(botId);
        ArrayList<String> ids = new ArrayList<>();
        ids.add(msg.getUserId());
        for (User user : feedbackClient.getUsers(ids)) {
            String feedback = String.format("**%s** sent:", user.name);
            feedbackClient.sendText(feedback);

            Picture picture = new Picture();
            picture.setMimeType(msg.getMimeType());
            picture.setSize((int) msg.getSize());
            picture.setWidth(msg.getWidth());
            picture.setHeight(msg.getHeight());
            picture.setAssetKey(msg.getAssetKey());
            picture.setAssetToken(msg.getAssetToken());
            picture.setOtrKey(msg.getOtrKey());
            picture.setSha256(msg.getSha256());

            feedbackClient.sendPicture(picture);
        }
    }

    public void sendOnMemberFeedback(String format, ArrayList<String> userIds) {
        try {
            String botId = conf.getFeedback();
            if (botId != null) {
                WireClient feedbackClient = repo.getWireClient(botId);
                for (User user : feedbackClient.getUsers(userIds)) {
                    String feedback = String.format(format, user.name);
                    feedbackClient.sendText(feedback);
                }
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    public void newUserFeedback(String name) throws Exception {
        String botId = conf.getFeedback();
        if (botId == null)
            return;

        WireClient feedbackClient = repo.getWireClient(botId);
        String feedback = String.format("**%s** just joined", name);
        feedbackClient.sendText(feedback);
    }

    private File[] getCryptoDirs() {
        File dir = new File(conf.cryptoDir);
        return dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String botId = file.getName();

                // Don't broadcast to Feedback conv.
                if (conf.getFeedback() != null && conf.getFeedback().equals(botId))
                    return false;
                return botId.split("-").length == 5 && file.isDirectory();
            }
        });
    }

    private static boolean isUrl(String text) {
        return text.startsWith("http");
    }

    private static boolean isPicture(String text) {
        return text.startsWith("http") && (
                text.endsWith(".jpg")
                        || text.endsWith(".gif")
                        || text.endsWith(".png"));
    }
}

