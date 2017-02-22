package com.wire.bots.broadcast.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.broadcast.storage.DbManager;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.broadcast.model.Message;
import com.wire.bots.sdk.server.tasks.TaskBase;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListMessagesTask extends TaskBase {
    private final Config config;

    public ListMessagesTask(Config config) {
        super("messages");
        this.config = config;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> params, PrintWriter printWriter) throws Exception {
        printWriter.println("BotId, UserId, Text, Asset, Date");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        DbManager manager = new DbManager(config.getDatabase());
        ArrayList<Message> messages = manager.getMessages();
        for (Message msg : messages) {
            printWriter.printf("%s, %s, %s, %s, %s\n",
                    msg.getBotId(),
                    msg.getUserId(),
                    msg.getText(),
                    msg.getAssetKey(),
                    df.format(new Date(msg.getTime() * 1000))
            );
        }
    }
}
