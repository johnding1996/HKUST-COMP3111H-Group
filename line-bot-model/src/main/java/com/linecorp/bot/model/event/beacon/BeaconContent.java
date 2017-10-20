/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.bot.model.event.beacon;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class BeaconContent {
    /**
     * Hardware ID of the beacon that was detected
     */
    private final String hwid;

    /**
     * Type of beacon event
     */
    private final String type;

    /**
     * @see #getDeviceMessage()
     */
    private final byte[] deviceMessage;

    /**
     * Device message of beacon that was detected in byte[] format. (Optional)
     *
     * <p>The "device message" consists of data generated by the beacon to send notifications to bots.
     *
     * <p>The beacon.dm property is only included in webhooks from devices that support the "device message" property.
     * If device message is not included in webhooks, value is {@code null}.</p>
     *
     * <p>You can use beacon.dm with the LINE Simple Beacon specification.</p>
     *
     * @see #getDeviceMessageAsHex()
     * @see <a href="https://github.com/line/line-simple-beacon/blob/master/README.en.md#line-simple-beacon-frame">LINE Simple Beacon specification (en)</a>
     * @see <a href="https://github.com/line/line-simple-beacon/blob/master/README.ja.md#line-simple-beacon-frame">LINE Simple Beacon specification (ja)</a>
     */
    public byte[] getDeviceMessage() {
        if (deviceMessage == null) {
            return null;
        }
        return deviceMessage.clone(); // Defensive copy.
    }

    /**
     * Device message of beacon that was detected in lower-case, hex String format. (Optional)
     *
     * @see #getDeviceMessage()
     */
    public String getDeviceMessageAsHex() {
        return BeaconContentUtil.printHexBinary(deviceMessage);
    }

    public BeaconContent(
            @JsonProperty("hwid") String hwid,
            @JsonProperty("type") String type,
            @JsonProperty("dm") String deviceMessageAsHex) {
        this.hwid = hwid;
        this.type = type;
        this.deviceMessage = BeaconContentUtil.parseBytesOrNull(deviceMessageAsHex);
    }

    // Delombok for byte[] pretty print.
    @Override
    public String toString() {
        return "BeaconContent"
               + "(hwid=" + getHwid()
               + ", type=" + getType()
               + ", deviceMessage=" + getDeviceMessageAsHex() + ')';
    }
}
