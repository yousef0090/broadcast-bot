package com.wire.bots.broadcast.resource;

import com.wire.bots.broadcast.model.Broadcast;
import com.wire.bots.broadcast.model.Config;
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Path("/broadcast")
public class BroadcastResource {
    private final ClientRepo repo;
    private final Config conf;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(20);
    private final DbManager dbManager;

    public BroadcastResource(ClientRepo repo, Config conf) {
        this.repo = repo;
        this.conf = conf;
        dbManager = new DbManager(conf.getDatabase());
    }

    @GET
    public Response broadcast(@QueryParam("text") final String text) throws Exception {
        if (text == null) {
            return Response.
                    ok("Nothing to broadcast").
                    status(200).
                    build();
        }

        File dir = new File(conf.cryptoDir);
        File[] botDirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().split("-").length == 5 && file.isDirectory();
            }
        });

        if (botDirs.length == 0)
            return Response.
                    ok("No subscribers yet :(").
                    status(200).
                    build();

        WireClient wireClient = repo.getWireClient(botDirs[0].getName());

        if (isPicture(text)) {
            final Picture picture = new Picture(text);
            AssetKey assetKey = wireClient.uploadAsset(picture);
            picture.setAssetKey(assetKey.key);
            picture.setAssetToken(assetKey.token);

            saveBroadcast(null, null, picture);

            for (File botDir : botDirs) {
                final String botId = botDir.getName();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        send(picture, botId);
                    }
                });
            }
        } else if (isUrl(text)) {
            final String url = text.trim();
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
        } else {
            dbManager.insertBroadcast(text);

            for (File botDir : botDirs) {
                final String botId = botDir.getName();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        send(text, botId);
                    }
                });
            }
        }
        return Response.
                ok(String.format("Scheduled broadcast for %,d conversations", botDirs.length)).
                status(201).
                build();
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

    private void saveBroadcast(String url, String title, Picture preview) throws NoSuchAlgorithmException, SQLException {
        // save to db
        Broadcast broadcast = new Broadcast();
        broadcast.setAssetData(preview.getImageData());
        broadcast.setAssetKey(preview.getAssetKey());
        broadcast.setToken(preview.getAssetToken());
        broadcast.setOtrKey(preview.getOtrKey());
        broadcast.setSha256(preview.getSha256());
        broadcast.setSize(preview.getSize());
        broadcast.setMimeType(preview.getMimeType());
        broadcast.setUrl(url);
        broadcast.setTitle(title);
        dbManager.insertBroadcast(broadcast);
    }

    static boolean isUrl(String text) {
        return text.startsWith("http");
    }

    static boolean isPicture(String text) {
        return text.startsWith("http") && (
                text.endsWith(".jpg")
                        || text.endsWith(".gif")
                        || text.endsWith(".png"));
    }

    private void send(String text, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.sendText(text);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
        }
    }

    private void send(Picture picture, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.sendPicture(picture);
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);
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

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }
}
