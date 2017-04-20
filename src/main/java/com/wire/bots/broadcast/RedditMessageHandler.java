package com.wire.bots.broadcast;

import com.wire.bots.broadcast.model.Broadcast;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class RedditMessageHandler extends BroadcastMessageHandlerBase {
    private BufferedImage img;
    private final Object lock = new Object();

    protected RedditMessageHandler(ClientRepo repo, Config config) {
        super(repo, config);

        try {
            for (Broadcast broadcast : dbManager.getLastAsset(1)) {
                try (InputStream in = new ByteArrayInputStream(broadcast.getAssetData())) {
                    img = ImageIO.read(in);
                }
            }
            if (img == null) {
                initImage();
                broadcast();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        long delay = TimeUnit.MINUTES.toMillis(config.getExpiration());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    broadcast();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delay, delay);
    }

    private void initImage() {
        img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < img.getWidth(); i++)
            for (int j = 0; j < img.getHeight(); j++) {
                img.setRGB(i, j, Color.WHITE.getRGB());
            }
    }

    private void broadcast() throws Exception {
        synchronized (lock) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "png", bos);

                Picture picture = new Picture(bos.toByteArray());
                picture.setExpires(TimeUnit.MINUTES.toMillis(config.getExpiration()));

                executor.broadcast(picture);
            }
        }
    }

    @Override
    protected void onNewFeedback(TextMessage msg) throws Exception {
        synchronized (lock) {
            String text = msg.getText();
            String[] split = text.split("[^0-9]");

            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int r = Integer.parseInt(split[2]);
            int g = Integer.parseInt(split[3]);
            int b = Integer.parseInt(split[4]);

            int rgb = new Color(r, g, b).getRGB();
            img.setRGB(x, y, rgb);
        }
    }

    @Override
    protected void onNewSubscriber(User origin, String locale) throws Exception {
        Logger.info(String.format("onNewSubscriber: origin: %s '%s' locale: %s", origin.id, origin.name, locale));
        executor.newUserFeedback(origin.name);
    }

    @Override
    protected void onNewFeedback(ImageMessage msg) throws Exception {

    }

    @Override
    protected void onNewBroadcast(TextMessage msg) throws Exception {

    }

    @Override
    protected void onNewBroadcast(ImageMessage msg, byte[] bytes) throws Exception {

    }
}
