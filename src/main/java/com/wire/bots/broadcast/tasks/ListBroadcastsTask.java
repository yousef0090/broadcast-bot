package com.wire.bots.broadcast.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.broadcast.model.Broadcast;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.sdk.server.tasks.TaskBase;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListBroadcastsTask extends TaskBase {
    private final Config config;

    public ListBroadcastsTask(Config config) {
        super("broadcasts");
        this.config = config;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> params, PrintWriter printWriter) throws Exception {
        printWriter.println("Id, Text, Url, Asset, Token, Mime, Size, Date");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        DbManager manager = new DbManager(config.getDatabase());
        ArrayList<Broadcast> broadcasts = manager.getBroadcast(0);
        for (Broadcast b : broadcasts) {
            printWriter.printf("%d, %s, %s, %s, %s, %s, %dKB, %s\n",
                    b.getId(),
                    b.getText(),
                    b.getUrl(),
                    b.getAssetKey(),
                    b.getToken(),
                    b.getMimeType(),
                    b.getSize() / 1024,
                    df.format(new Date(b.getTime() * 1000))
            );
        }
    }
}
