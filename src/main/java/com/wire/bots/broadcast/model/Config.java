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

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@Data
public class Config extends com.wire.bots.sdk.Configuration {
    @NotNull
    private String database;
    private String admin;
    private ArrayList<String> whitelist;
    private String onNewSubscriberLabel;
    private long fallback;
    private String channelName;
    private boolean like = true;
    private long expiration;
    private String appSecret;
}
