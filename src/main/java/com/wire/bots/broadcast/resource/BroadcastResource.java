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
import com.wire.bots.broadcast.model.BroadcastMessage;
import com.wire.bots.broadcast.model.Config;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.assets.Picture;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/broadcast/v1")
public class BroadcastResource {
    private final ClientRepo repo;
    private final Config conf;

    public BroadcastResource(ClientRepo repo, Config conf) {
        this.repo = repo;
        this.conf = conf;
    }

    @POST
    public Response broadcast(BroadcastMessage broadcastMessage) throws Exception {

        String message = broadcastMessage.getMessage();
        String challenge = Util.getHmacSHA1(message, conf.getAppSecret());
        if (!challenge.equals(broadcastMessage.getSignature())) {
            Logger.warning("Invalid Signature.");
            return Response.
                    status(403).
                    build();
        }

        try {
            Executor exec = new Executor(repo, conf);

            if (isPicture(message)) {
                Picture picture = new Picture(message);

                exec.broadcast(picture);
            } else if (message.startsWith("http")) {
                exec.broadcastUrl(message);
            } else {
                exec.broadcastText(UUID.randomUUID().toString(), handleNewLines(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
            return Response.
                    ok("Something went terribly wrong: %s" + e.getLocalizedMessage()).
                    status(500).
                    build();
        }

        return Response.
                ok().
                build();
    }

    private static boolean isPicture(String text) {
        return text.startsWith("http") && (
                text.endsWith(".jpg")
                        || text.endsWith(".gif")
                        || text.endsWith(".png"));
    }

    private static String handleNewLines(String text) {
        return text.replaceAll("\\n", "\n");
    }
}

