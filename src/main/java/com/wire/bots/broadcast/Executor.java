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
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Executor {
    private final ClientRepo repo;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(20);
    private final DbManager dbManager;

    public Executor(ClientRepo repo, DbManager dbManager) {
        this.repo = repo;
        this.dbManager = dbManager;
    }

    public void broadcastUrl(final String url, File[] botDirs) throws Exception {
        WireClient wireClient = repo.getWireClient(botDirs[0].getName());

        final String title = extractPageTitle(url);
        final Picture preview = uploadPreview(wireClient, extractPagePreview(url));

        saveBroadcast(url, title, preview);

        for (File botDir : botDirs) {
            final String botId = botDir.getName();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendLink(url, title, preview, botId);
                }
            });
        }
    }

    public void broadcastPicture(final Picture picture, File[] botDirs)  {
        saveBroadcast(null, null, picture);

        for (File botDir : botDirs) {
            final String botId = botDir.getName();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendPicture(picture, botId);
                }
            });
        }
    }

    public void broadcastText(final String messageId, final String text, File[] botDirs) throws SQLException {
        Broadcast b = new Broadcast();
        b.setMessageId(messageId);
        b.setText(text);

        dbManager.insertBroadcast(b);

        for (File botDir : botDirs) {
            final String botId = botDir.getName();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendText(messageId, text, botId);
                }
            });
        }
    }

    private void saveBroadcast(String url, String title, Picture picture) {
        try {
            // save to db
            Broadcast broadcast = new Broadcast();
            broadcast.setAssetData(picture.getImageData());
            broadcast.setAssetKey(picture.getAssetKey());
            broadcast.setToken(picture.getAssetToken());
            broadcast.setOtrKey(picture.getOtrKey());
            broadcast.setSha256(picture.getSha256());
            broadcast.setSize(picture.getSize());
            broadcast.setMimeType(picture.getMimeType());
            broadcast.setUrl(url);
            broadcast.setTitle(title);
            broadcast.setMessageId(picture.getMessageId());

            dbManager.insertBroadcast(broadcast);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void sendLink(String url, String title, Picture img, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.sendLinkPreview(url, title, img);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
        }
    }

    private void sendText(String messageId, String text, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.sendText(text, 0, messageId);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
        }
    }

    private void sendPicture(Picture picture, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.sendPicture(picture);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
        }
    }

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }

    private String extractPagePreview(String url) throws IOException {
        Connection con = Jsoup.connect(url);
        Document doc = con.get();

        Elements metaOgImage = doc.select("meta[property=og:image]");
        if (metaOgImage != null) {
            return metaOgImage.attr("content");
        }
        return null;
    }

    private String extractPageTitle(String url) throws IOException {
        Connection con = Jsoup.connect(url);
        Document doc = con.get();

        Elements title = doc.select("meta[property=og:title]");
        if (title != null) {
            return title.attr("content");
        }
        return doc.title();
    }

    public void deleteBroadcast(final String messageId, File[] botDirs) throws SQLException {
        dbManager.deleteBroadcast(messageId);

        for (File botDir : botDirs) {
            final String botId = botDir.getName();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    deleteMessage(messageId, botId);
                }
            });
        }
    }

    private void deleteMessage(String messageId, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.deleteMessage(messageId);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
        }
    }
}
