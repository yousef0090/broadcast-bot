package com.wire.bots.broadcast;

import com.wire.bots.broadcast.model.Config;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;

public class BroadcastMessageHandler extends BroadcastMessageHandlerBase {
    public BroadcastMessageHandler(ClientRepo repo, Config config) {
        super(repo, config);
    }

    @Override
    protected void onNewBroadcast(TextMessage msg) throws Exception {
        executor.broadcast(msg);
    }

    @Override
    protected void onNewBroadcast(ImageMessage msg, byte[] bytes) throws Exception {
        executor.broadcast(msg, bytes);
    }

    @Override
    protected void onNewSubscriber(User origin, String locale) throws Exception {
        Logger.info(String.format("onNewSubscriber: origin: %s '%s' locale: %s", origin.id, origin.name, locale));
        executor.newUserFeedback(origin.name);
    }

    @Override
    protected void onNewFeedback(TextMessage msg) throws Exception {
        executor.forwardFeedback(msg);
    }

    @Override
    protected void onNewFeedback(ImageMessage msg) throws Exception {
        executor.forwardFeedback(msg);
    }
}
