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

@Data
public class Broadcast {
    private int id;
    private String text;
    private long time;
    private String assetKey;
    private String token;
    private byte[] otrKey;
    private byte[] sha256;
    private String mimeType;
    private long size;
    private String url;
    private String title;
    private byte[] assetData;
    private String messageId;
}
