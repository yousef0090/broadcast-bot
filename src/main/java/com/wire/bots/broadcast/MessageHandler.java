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
import com.wire.bots.broadcast.resource.BroadcastResource;
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private static final int HISTORY_DAYS = 7;
    private final DbManager dbManager;
    private final Timer timer;
    private final BroadcastResource broadcastResource;
    private final Config config;

    public MessageHandler(BroadcastResource broadcastResource, Config config) {
        this.broadcastResource = broadcastResource;
        this.config = config;

        dbManager = broadcastResource.getDbManager();
        timer = new Timer();
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            Logger.info(String.format("onNewBot: bot: %s, origin: %s", newBot.id, newBot.origin.id));

            if (!isWhiteListed(newBot.origin.id)) {
                Logger.info(String.format("Rejecting user: %s/%s", newBot.origin.id, newBot.origin.name));
                return false;
            }
            saveNewBot(newBot);

            broadcastResource.newUserFeedback(newBot.origin.name);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String botId = client.getId();

            if (isAdminBot(botId)) {
                broadcastResource.broadcast(msg.getText());
            } else {
                saveMessage(botId, msg);

                broadcastResource.forwardFeedback(msg);

                likeMessage(client, msg.getMessageId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            String botId = client.getId();

            if (isAdminBot(botId)) {
                byte[] bytes = client.downloadAsset(msg.getAssetKey(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
                broadcastResource.broadcast(msg, bytes);
            } else {
                saveMessage(botId, msg);

                broadcastResource.forwardFeedback(msg);

                likeMessage(client, msg.getMessageId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            //Logger.info(String.format("onNewConversation: bot: %s, conv: %s", client.getId(), client.getConversationId()));

            client.sendText(config.getOnNewSubscriberLabel());

            long from = new Date().getTime() - TimeUnit.DAYS.toMillis(config.getFallback());
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

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        broadcastResource.sendOnMemberFeedback("**%s** joined", userIds);
    }

    @Override
    public void onMemberLeave(WireClient ignored, ArrayList<String> userIds) {
        broadcastResource.sendOnMemberFeedback("**%s** left", userIds);
    }

    private void likeMessage(final WireClient client, final String messageId) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    client.sendReaction(messageId, "❤️");
                } catch (Exception e) {
                    Logger.error(e.getLocalizedMessage());
                }
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }

    private void saveMessage(String botId, ImageMessage msg) throws SQLException {
        try {
            Message m = new Message();
            m.setBotId(botId);
            m.setUserId(msg.getUserId());
            m.setAssetKey(msg.getAssetKey());
            m.setToken(msg.getAssetToken());
            m.setOtrKey(msg.getOtrKey());
            m.setSha256(msg.getSha256());
            m.setSize(msg.getSize());
            m.setTime(new Date().getTime() / 1000);
            m.setMimeType(msg.getMimeType());
            dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void saveMessage(String botId, TextMessage msg) {
        try {
            Message m = new Message();
            m.setBotId(botId);
            m.setUserId(msg.getUserId());
            m.setText(msg.getText());
            m.setMimeType("text");
            dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void saveNewBot(NewBot newBot) {
        try {
            dbManager.insertUser(newBot);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    private boolean isAdminBot(String botId) {
        return config.getFeedback() != null && config.getFeedback().equals(botId);
    }

    private boolean isWhiteListed(String userId) {
        return config.getWhitelist() == null || config.getWhitelist().contains(userId);
    }
}
