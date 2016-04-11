/*
 * Copyright (C) 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Contributed by: Giesecke & Devrient GmbH.
 */

package org.simalliance.openmobileapi.service.terminals;

import android.content.Context;
import org.simalliance.openmobileapi.service.CardException;
import org.simalliance.openmobileapi.service.SmartcardService;
import org.simalliance.openmobileapi.service.Terminal;
import org.simalliance.openmobileapi.service.Util;
import org.simalliance.openmobileapi.service.security.arf.SecureElementException;


import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.msim.ITelephonyMSim;

import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class UiccTerminal extends Terminal {

    public final String _TAG;

    private ITelephony manager = null;

    private ITelephonyMSim mMsimManager = null;

    private int[] channelId = new int[20];

    private String currentSelectedFilePath = "";

    private final int mUiccSlot;

    public UiccTerminal(Context context, int slot) {
        super(SmartcardService._UICC_TERMINAL + SmartcardService._UICC_TERMINAL_EXT[slot], context);

        if (SmartcardService.mIsMultiSimEnabled) {
            try {
                mMsimManager = ITelephonyMSim.Stub.asInterface(ServiceManager
                        .getService(Context.MSIM_TELEPHONY_SERVICE));
            } catch (Exception ex) {
            }
        } else {
            try {
                manager = ITelephony.Stub.asInterface(ServiceManager
                        .getService(Context.TELEPHONY_SERVICE));
            } catch (Exception ex) {
            }
        }

        mUiccSlot = slot;
        _TAG = SmartcardService._UICC_TERMINAL + SmartcardService._UICC_TERMINAL_EXT[slot];

        Log.i(_TAG, "UiccTerminal(): mIsMultiSimEnabled = " + SmartcardService.mIsMultiSimEnabled);

        for (int i = 0; i < channelId.length; i++)
            channelId[i] = 0;
    }

    public boolean isCardPresent() throws CardException {
        String prop = SystemProperties
                .get(TelephonyProperties.PROPERTY_SIM_STATE);

        Log.i(_TAG, "isCardPresent(): PROPERTY_SIM_STATE=" + prop);

        String[] simStatusStr = prop.split(",");

        if ((simStatusStr.length > mUiccSlot)&&
            (simStatusStr[mUiccSlot].equals("READY"))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void internalConnect() throws CardException {
        if (SmartcardService.mIsMultiSimEnabled) {
            if (mMsimManager == null) {
                throw new CardException("Cannot connect to MSIM Telephony Service");
            }
        } else {
            if (manager == null) {
                throw new CardException("Cannot connect to Telephony Service");
            }
        }
        mIsConnected = true;
    }

    @Override
    protected void internalDisconnect() throws CardException {
    }

    private byte[] StringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return b;
    }

    private String ByteArrayToString(byte[] b, int start) {
        StringBuffer s = new StringBuffer();
        for (int i = start; i < b.length; i++) {
            s.append(Integer.toHexString(0x100 + (b[i] & 0xff)).substring(1));
        }
        return s.toString();
    }

    /**
     * Clear the channel number
     *
     * @param cla
     *
     * @return the cla without channel number
     */
    private byte clearChannelNumber(byte cla) {
        // bit 7 determines which standard is used
        boolean isFirstInterindustryClassByteCoding = ((cla & 0x40) == 0x00);

        if(isFirstInterindustryClassByteCoding){
            // First Interindustry Class Byte Coding
            // see 11.1.4.1: channel number is encoded in the 2 rightmost bits
            return (byte)(cla & 0xFC);
        }else{
            // Further Interindustry Class Byte Coding
            // see 11.1.4.2: channel number is encoded in the 4 rightmost bits
            return (byte)(cla & 0xF0);
        }
    }

    @Override
    protected byte[] internalTransmit(byte[] command) throws CardException {
        int cla = clearChannelNumber(command[0]) & 0xff;
        int ins = command[1] & 0xff;
        int p1 = command[2] & 0xff;
        int p2 = command[3] & 0xff;
        int p3 = -1;
        if (command.length > 4) {
            p3 = command[4] & 0xff;
        }
        String data = null;
        if (command.length > 5) {
            data = ByteArrayToString(command, 5);
        }

        int channelNumber = parseChannelNumber(command[0]);

        if (channelNumber == 0) {

            try {
                if (SmartcardService.mIsMultiSimEnabled) {
                    String response = mMsimManager.transmitIccBasicChannel(cla, ins, p1,
                            p2, p3, data, mUiccSlot);
                    return StringToByteArray(response);
                } else {
                    String response = manager.transmitIccBasicChannel(cla, ins, p1,
                            p2, p3, data);
                    return StringToByteArray(response);
                }
            } catch (Exception ex) {
                throw new CardException("transmit command failed");
            }

        } else {
            if ((channelNumber > 0) && (channelId[channelNumber] == 0)) {
                throw new CardException("channel not open");
            }

            try {
                if (SmartcardService.mIsMultiSimEnabled) {
                    String response = mMsimManager.transmitIccLogicalChannel(cla, ins, channelId[channelNumber], p1, p2, p3, data, mUiccSlot);
                    return StringToByteArray(response);
                } else {
                    String response = manager.transmitIccLogicalChannel(cla, ins, channelId[channelNumber], p1, p2, p3, data);
                    return StringToByteArray(response);
                }
            } catch (Exception ex) {
                throw new CardException("transmit command failed");
            }
        }
    }

    /**
     * Returns the ATR of the connected card or null if the ATR is not
     * available.
     *
     * @return the ATR of the connected card or null if the ATR is not
     *         available.
     */
    @Override
    public byte[] getAtr() {
        try {
            if (SmartcardService.mIsMultiSimEnabled) {
                byte[] atr = mMsimManager.getATR(mUiccSlot);
                return atr;
            } else {
                byte[] atr = manager.getATR();
                return atr;
            }
        } catch (Exception e) {
            throw new IllegalStateException("internal error: getAtr() execution: "
                    + e.getCause());
        }
    }

    /**
     * Exchanges APDU (SELECT, READ/WRITE) to the
     * given EF by File ID and file path via iccIO.
     *
     * The given command is checked and might be rejected.
     *
     * @param fileID
     * @param filePath
     * @param cmd
     * @return
     */
    @Override
    public byte[] simIOExchange(int fileID,String filePath,byte[] cmd)
            throws Exception {
        try {
            int ins = 0;
            int p1=cmd[2] & 0xff;
            int p2=cmd[3] & 0xff;
            int p3=cmd[4] & 0xff;
            switch(cmd[1]) {
                case (byte)0xB0: ins=176; break;
                case (byte)0xB2: ins=178; break;
                case (byte)0xA4: ins=192;  p1=0; p2=0; p3=15; break;
                default:
                    throw new SecureElementException("Unknown SIM_IO command");
            }

            if(filePath != null && filePath.length() > 0) {
                currentSelectedFilePath = filePath;
            }

            if (SmartcardService.mIsMultiSimEnabled) {
                byte[] ret = mMsimManager.transmitIccSimIO(fileID, ins, p1, p2, p3, currentSelectedFilePath, mUiccSlot);
                return ret;
            } else {
                byte[] ret = manager.transmitIccSimIO(fileID, ins, p1, p2, p3, currentSelectedFilePath);
                return ret;
            }
        } catch (Exception e) {
            throw new Exception("SIM IO access error");
    }}


    /**
     * Extracts the channel number from a CLA byte. Specified in GlobalPlatform
     * Card Specification 2.2.0.7: 11.1.4 Class Byte Coding
     *
     * @param cla
     *            the command's CLA byte
     * @return the channel number within [0x00..0x0F]
     */
    private int parseChannelNumber(byte cla) {
        // bit 7 determines which standard is used
        boolean isFirstInterindustryClassByteCoding = ((cla & 0x40) == 0x00);

        if(isFirstInterindustryClassByteCoding){
            // First Interindustry Class Byte Coding
            // see 11.1.4.1: channel number is encoded in the 2 rightmost bits
            return cla & 0x03;
        }else{
            // Further Interindustry Class Byte Coding
            // see 11.1.4.2: channel number is encoded in the 4 rightmost bits
            return (cla & 0x0F) + 4;
        }
    }

    @Override
    protected int internalOpenLogicalChannel() throws Exception {

        mSelectResponse = null;
        throw new UnsupportedOperationException(
                "open channel without select AID is not supported by UICC");
    }

    @Override
    protected int internalOpenLogicalChannel(byte[] aid) throws Exception {

        if (aid == null) {
            throw new NullPointerException("aid must not be null");
        }
        mSelectResponse = null;
        for (int i = 1; i < channelId.length; i++)
            if (channelId[i] == 0) {
                if (SmartcardService.mIsMultiSimEnabled) {
                    channelId[i] = mMsimManager.openIccLogicalChannel(ByteArrayToString(aid, 0), mUiccSlot);
                } else {
                    channelId[i] = manager.openIccLogicalChannel(ByteArrayToString(aid, 0));
                }

                if (!(channelId[i] > 0)) { // channelId[i] == 0
                    channelId[i] = 0;
                    int lastError;
                    if (SmartcardService.mIsMultiSimEnabled) {
                        lastError = mMsimManager.getLastError(mUiccSlot);
                    } else {
                        lastError = manager.getLastError();
                    }

                    if (lastError == 2) {
                        throw new MissingResourceException(
                                "all channels are used", "", "");
                    }
                    if (lastError == 3) {
                        throw new NoSuchElementException("applet not found");
                    }
                    throw new CardException("open channel failed");
                }
                else{
                    byte[] getResponseCmd = new byte[] { 0x00, (byte) 0xC0, 0x00, 0x00, 0x00 };

                    if (i < 4) {
                        // b7 = 0 indicates the first interindustry class byte coding
                        getResponseCmd[0] = (byte) (i);
                    } else if (i < 20) {
                        // b7 = 1 indicates the further interindustry class byte coding
                        getResponseCmd[0] = (byte) (0x40 | (byte)(i - 4));
                    } else {
                        channelId[i] = 0;
                        throw new CardException( "Channel number index must be within [1..19]");
                    }
                        mSelectResponse = internalTransmit(getResponseCmd);
                    }

                return i;
            }
        throw new MissingResourceException("out of channels", "","");
    }

    @Override
    protected void internalCloseLogicalChannel(int channelNumber)
            throws CardException {
        if (channelNumber == 0) {
            return;
        }
        if (channelId[channelNumber] == 0) {
            throw new CardException("channel not open");
        }
        try {
            if (SmartcardService.mIsMultiSimEnabled) {
                if (mMsimManager.closeIccLogicalChannel(channelId[channelNumber], mUiccSlot) == false) {
                    throw new CardException("close channel failed");
                }
            } else {
                if (manager.closeIccLogicalChannel(channelId[channelNumber]) == false) {
                    throw new CardException("close channel failed");
                }
            }
        } catch (Exception ex) {
            throw new CardException("close channel failed");
        }
        channelId[channelNumber] = 0;
    }
}
