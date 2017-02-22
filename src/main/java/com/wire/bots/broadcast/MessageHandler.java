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

package com.wire.bots.broadcast;

import com.wire.bots.broadcast.model.Broadcast;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.broadcast.model.Message;
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private static final int HISTORY_DAYS = 7;
    private final DbManager dbManager;
    private final Config config;
    private final ClientRepo repo;

    public MessageHandler(Config config, ClientRepo repo) {
        this.config = config;
        this.repo = repo;

        dbManager = new DbManager(config.getDatabase());
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.info(String.format("onNewBot: bot: %s, origin: %s", newBot.id, newBot.origin.id));

        try {
            dbManager.insertUser(newBot);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            Message m = new Message();
            m.setBotId(client.getId());
            m.setUserId(msg.getUserId());
            m.setText(msg.getText());
            m.setMimeType("text");
            dbManager.insertMessage(m);

            client.sendReaction(msg.getMessageId(), "❤️");

            forwardFeedback(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            Message m = new Message();
            m.setBotId(client.getId());
            m.setUserId(msg.getUserId());
            m.setAssetKey(msg.getAssetKey());
            m.setToken(msg.getAssetToken());
            m.setOtrKey(msg.getOtrKey());
            m.setSha256(msg.getSha256());
            m.setSize(msg.getSize());
            m.setTime(new Date().getTime() / 1000);
            m.setMimeType(msg.getMimeType());
            dbManager.insertMessage(m);

            forwardFeedback(msg);

            client.sendReaction(msg.getMessageId(), "❤️");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            Logger.info(String.format("onNewConversation: bot: %s, conv: %s", client.getId(), client.getConversationId()));

            client.sendText("Hello! I am Broadcast bot. I broadcast a lot. Here is something you've missed");

            long from = new Date().getTime() - TimeUnit.DAYS.toMillis(HISTORY_DAYS);
            for (Broadcast b : dbManager.getBroadcast(from / 1000)) {
                if (b.getText() != null) {
                    client.sendText(b.getText());
                } else if (b.getUrl() != null) {
                    Picture preview = new Picture(b.getAssetData());
                    preview.setAssetKey(b.getAssetKey());
                    preview.setAssetToken(b.getToken());
                    preview.setOtrKey(b.getOtrKey());
                    preview.setSha256(b.getSha256());

                    client.sendLinkPreview(b.getUrl(), b.getTitle(), preview);
                } else if (b.getAssetData() != null) {
                    Picture picture = new Picture(b.getAssetData());
                    picture.setAssetKey(b.getAssetKey());
                    picture.setAssetToken(b.getToken());
                    picture.setOtrKey(b.getOtrKey());
                    picture.setSha256(b.getSha256());

                    client.sendPicture(picture);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    private void forwardFeedback(TextMessage msg) throws Exception {
        String botId = config.getFeedback();
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

    private void forwardFeedback(ImageMessage msg) throws Exception {
        String botId = config.getFeedback();
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
}
