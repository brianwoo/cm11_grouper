/*
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2008 The Android Open Source Project
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

package com.codeaurora.telephony.msim;

import android.app.PendingIntent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsRawData;

import java.util.List;

/**
 * MSimDefaultIccSmsInterfaceManager to provide an inter-process communication to
 * access Sms in Icc.
 */
public class MSimDefaultIccSmsInterfaceManager extends ISms.Stub {
    static final String LOG_TAG = "RIL_DefaultIccSms";

    // Holds SMS preferred subscription related phone proxy object
    private MSimPhoneProxy mProxyPhone = null;

    protected MSimDefaultIccSmsInterfaceManager() {
        Rlog.d(LOG_TAG, "MSimIccSmsInterfaceManager created");

        if(ServiceManager.getService("isms") == null) {
            ServiceManager.addService("isms", this);
        }
    }

    public boolean
    updateMessageOnIccEf(String callingPackage, int index, int status, byte[] pdu)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.updateMessageOnIccEf(callingPackage, index, status, pdu);
        } else {
            Rlog.e(LOG_TAG,"updateMessageOnIccEf iccSmsIntMgr is null");
            return false;
        }
    }

    public boolean copyMessageToIccEf(String callingPackage, int status, byte[] pdu,
            byte[] smsc) throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.copyMessageToIccEf(callingPackage, status, pdu, smsc);
        } else {
            Rlog.e(LOG_TAG,"copyMessageToIccEf iccSmsIntMgr is null");
            return false;
        }
    }

    public void synthesizeMessages(String originatingAddress, String scAddress, List<String> messages, long timestampMillis) throws RemoteException {
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String callingPackage)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getAllMessagesFromIccEf(callingPackage);
        } else {
            Rlog.e(LOG_TAG,"getAllMessagesFromIccEf iccSmsIntMgr is null");
            return null;
        }
    }

    public void sendData(String callingPackage, String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendData(callingPackage, destAddr, scAddr, destPort, data,
                    sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendData iccSmsIntMgr is null ");
        }
    }

    public void sendDataWithOrigPort(String callingPackage, String destAddr, String scAddr,
            int destPort, int origPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendDataWithOrigPort(callingPackage, destAddr, scAddr, destPort, origPort,
                    data, sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendDataWithOrigPort iccSmsIntMgr is null ");
        }
    }

    public void sendText(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendText(callingPackage, destAddr, scAddr, text, sentIntent,
                    deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendText iccSmsIntMgr is null ");
        }
    }

    public void sendTextWithOptions(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            int priority, boolean isExpectMore, int validityPeriod) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendTextWithOptions(callingPackage, destAddr, scAddr, text, sentIntent,
                    deliveryIntent, priority, isExpectMore, validityPeriod);
        } else {
            Rlog.e(LOG_TAG,"sendTextWithOptions iccSmsIntMgr is null ");
        }
    }


    public void sendMultipartText(String callingPackage, String destAddr, String scAddr,
            List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.sendMultipartText(callingPackage, destAddr, scAddr, parts, sentIntents,
                    deliveryIntents);
        } else {
            Rlog.e(LOG_TAG,"sendMultipartText iccSmsIntMgr is null ");
        }
    }

    public void sendMultipartTextWithOptions(String callingPackage, String destAddr,
            String scAddr, List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents, int priority, boolean isExpectMore,
            int validityPeriod) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.sendMultipartTextWithOptions(callingPackage, destAddr, scAddr, parts,
                    sentIntents, deliveryIntents, priority, isExpectMore, validityPeriod);
        } else {
            Rlog.e(LOG_TAG,"sendMultipartTextWithOptions iccSmsIntMgr is null ");
        }
    }

    public boolean enableCellBroadcast(int messageIdentifier)
                throws android.os.RemoteException {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.enableCellBroadcastRange(startMessageId, endMessageId);
        } else {
            Rlog.e(LOG_TAG,"enableCellBroadcast iccSmsIntMgr is null ");
        }
        return false;
    }

    public boolean disableCellBroadcast(int messageIdentifier)
                throws android.os.RemoteException {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier);
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.disableCellBroadcastRange(startMessageId, endMessageId);
        } else {
            Rlog.e(LOG_TAG,"disableCellBroadcast iccSmsIntMgr is null ");
        }
       return false;
    }

    public int getPremiumSmsPermission(String packageName) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getPremiumSmsPermission(packageName);
        } else {
            Rlog.e(LOG_TAG, "getPremiumSmsPermission iccSmsIntMgr is null");
        }
        return 0;
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.setPremiumSmsPermission(packageName, permission);
        } else {
            Rlog.e(LOG_TAG, "setPremiumSmsPermission iccSmsIntMgr is null");
        }
    }

    public boolean isImsSmsSupported() {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.isImsSmsSupported();
        } else {
            Rlog.e(LOG_TAG, "isImsSmsSupported iccSmsIntMgr is null");
        }
        return false;
    }

    public String getImsSmsFormat() {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getImsSmsFormat();
        } else {
            Rlog.e(LOG_TAG, "getImsSmsFormat iccSmsIntMgr is null");
        }
        return null;
    }

    public int getSmsCapacityOnIcc() {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager();
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getSmsCapacityOnIcc();
        } else {
            Rlog.e(LOG_TAG, "getSmsCapacityOnIcc iccSmsIntMgr is null");
        }
        return -1;
    }

    /**
     * get sms interface manager object from user preferred sms subscription
     * related phone proxy object.
     **/
    private MSimIccSmsInterfaceManager getIccSmsInterfaceManager() {
        try {
            return (MSimIccSmsInterfaceManager)mProxyPhone.getIccSmsInterfaceManager();
        } catch (NullPointerException e) {
            Rlog.e(LOG_TAG, "Exception is : " + e.toString());
            e.printStackTrace(); //This will print stact trace
            return null;
        }
    }

    protected void updatePhoneProxyObject(MSimPhoneProxy phone) {
        mProxyPhone = phone;
    }
}
