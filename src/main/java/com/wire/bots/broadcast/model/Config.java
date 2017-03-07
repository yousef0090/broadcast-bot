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

package com.wire.bots.broadcast.model;

import java.util.HashSet;

public class Config extends com.wire.bots.sdk.Configuration {
    private String database = "crypto/broadcast.db";
    private String feedback;
    private HashSet<String> whitelist;
    private String onNewSubscriberLabel;
    private long fallback;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public HashSet<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(HashSet<String> whitelist) {
        this.whitelist = whitelist;
    }

    public String getOnNewSubscriberLabel() {
        return onNewSubscriberLabel;
    }

    public void setOnNewSubscriberLabel(String onNewSubscriberLabel) {
        this.onNewSubscriberLabel = onNewSubscriberLabel;
    }

    public long getFallback() {
        return fallback;
    }

    public void setFallback(long fallback) {
        this.fallback = fallback;
    }
}
