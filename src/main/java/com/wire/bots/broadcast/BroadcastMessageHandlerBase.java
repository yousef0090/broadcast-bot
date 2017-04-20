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

import com.waz.model.Messages;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

abstract class BroadcastMessageHandlerBase extends MessageHandlerBase {
    protected final DbManager dbManager;
    protected final Timer timer;
    protected final Config config;
    protected final Executor executor;

    protected BroadcastMessageHandlerBase(ClientRepo repo, Config config) {
        this.config = config;

        executor = new Executor(repo, config);
        dbManager = executor.getDbManager();
        timer = new Timer();
    }

    abstract protected void onNewBroadcast(TextMessage msg) throws Exception;

    abstract protected void onNewBroadcast(ImageMessage msg, byte[] bytes) throws Exception;

    abstract protected void onNewSubscriber(User origin, String locale) throws Exception;

    abstract protected void onNewFeedback(TextMessage msg) throws Exception;

    abstract protected void onNewFeedback(ImageMessage msg) throws Exception;

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            User origin = newBot.origin;
            if (!isWhiteListed(origin.id)) {
                Logger.info(String.format("Rejecting user: %s/%s", origin.id, origin.name));
                return false;
            }
            saveNewBot(newBot);

            onNewSubscriber(origin, newBot.locale);
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
                onNewBroadcast(msg);
            } else {
                saveMessage(botId, msg);

                if (config.isLike())
                    likeMessage(client, msg.getMessageId());

                onNewFeedback(msg);
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
                onNewBroadcast(msg, bytes);
            } else {
                saveMessage(botId, msg);

                if (config.isLike())
                    likeMessage(client, msg.getMessageId());

                onNewFeedback(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            String label = config.getOnNewSubscriberLabel().replace("[botId]", client.getId());
            client.sendText(label);

            long from = new Date().getTime() - TimeUnit.MINUTES.toMillis(config.getFallback());
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
                    picture.setExpires(TimeUnit.MINUTES.toMillis(config.getExpiration()));

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
        executor.sendOnMemberFeedback("**%s** joined", userIds);
    }

    @Override
    public void onMemberLeave(WireClient ignored, ArrayList<String> userIds) {
        executor.sendOnMemberFeedback("**%s** left", userIds);
    }

    @Override
    public void onEvent(WireClient client, String userId, Messages.GenericMessage msg) {
        if (msg.hasReaction()) {
            Logger.info(String.format("onEvent (Like) bot: %s, user: %s", client.getId(), userId));
        }

        if (msg.hasDeleted()) {
            if (isAdminBot(client.getId())) {
                Messages.MessageDelete deleted = msg.getDeleted();
                try {
                    executor.revokeBroadcast(deleted.getMessageId());
                } catch (SQLException e) {
                    Logger.error("Error revoking broadcast. " + e.getLocalizedMessage());
                }
            }
        }
    }

    @Override
    public String getName() {
        return config.getChannelName();
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
        return config.getAdmin() != null && config.getAdmin().equals(botId);
    }

    private boolean isWhiteListed(String userId) {
        return config.getWhitelist() == null
                || config.getWhitelist().isEmpty()
                || config.getWhitelist().contains(userId);

    }
}
