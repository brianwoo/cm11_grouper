/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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
import android.os.ServiceManager;
import android.telephony.Rlog;

import com.android.internal.telephony.msim.ISmsMSim;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsRawData;

import java.util.ArrayList;
import java.util.List;

/**
 * MSimIccSmsInterfaceManagerProxy to provide an inter-process communication to
 * access Sms in Icc.
 */
public class MSimIccSmsInterfaceManagerProxy extends ISmsMSim.Stub {
    static final String LOG_TAG = "RIL_MSimIccSms";

    protected Phone[] mPhone;

    protected MSimIccSmsInterfaceManagerProxy(Phone[] phone){
        mPhone = phone;

        if (ServiceManager.getService("isms_msim") == null) {
            ServiceManager.addService("isms_msim", this);
        }
    }

    public boolean
    updateMessageOnIccEf(String callingPackage, int index, int status, byte[] pdu, int subscription)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.updateMessageOnIccEf(callingPackage, index, status, pdu);
        } else {
            Rlog.e(LOG_TAG,"updateMessageOnIccEf iccSmsIntMgr is null" +
                          " for Subscription:"+subscription);
            return false;
        }
    }

    public boolean copyMessageToIccEf(String callingPackage, int status, byte[] pdu,
            byte[] smsc, int subscription) throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.copyMessageToIccEf(callingPackage, status, pdu, smsc);
        } else {
            Rlog.e(LOG_TAG,"copyMessageToIccEf iccSmsIntMgr is null" +
                          " for Subscription:"+subscription);
            return false;
        }
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String callingPackage, int subscription)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getAllMessagesFromIccEf(callingPackage);
        } else {
            Rlog.e(LOG_TAG,"getAllMessagesFromIccEf iccSmsIntMgr is" +
                          " null for Subscription:"+subscription);
            return null;
        }
    }

    public void sendData(String callingPackage, String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendData(callingPackage, destAddr, scAddr, destPort, data,
                    sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendData iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
    }

    public void sendDataWithOrigPort(String callingPackage, String destAddr, String scAddr,
            int destPort, int origPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendDataWithOrigPort(callingPackage, destAddr, scAddr, destPort, origPort,
                    data, sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendDataWithOrigPort iccSmsIntMgr is null ");
        }
    }

    public void sendText(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendText(callingPackage, destAddr, scAddr, text, sentIntent,
                    deliveryIntent);
        } else {
            Rlog.e(LOG_TAG,"sendText iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
    }

    public void sendTextWithOptions(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            int priority, boolean isExpectMore, int validityPeriod, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendTextWithOptions(callingPackage, destAddr, scAddr, text, sentIntent,
                    deliveryIntent, priority, isExpectMore, validityPeriod);
        } else {
            Rlog.e(LOG_TAG,"sendTextWithOptions iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
    }

    public void sendMultipartText(String callingPackage, String destAddr, String scAddr,
            List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents, int subscription)
            throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.sendMultipartText(callingPackage, destAddr, scAddr, parts, sentIntents,
                    deliveryIntents);
        } else {
            Rlog.e(LOG_TAG,"sendMultipartText iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
    }

    public void sendMultipartTextWithOptions(String callingPackage, String destAddr,
            String scAddr, List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents, int priority, boolean isExpectMore,
            int validityPeriod, int subscription) throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.sendMultipartTextWithOptions(callingPackage, destAddr, scAddr, parts,
                    sentIntents, deliveryIntents, priority, isExpectMore, validityPeriod);
        } else {
            Rlog.e(LOG_TAG,"sendMultipartTextWithOptions iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
    }


    public boolean enableCellBroadcast(int messageIdentifier, int subscription)
                throws android.os.RemoteException {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier, subscription);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int subscription)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.enableCellBroadcastRange(startMessageId, endMessageId);
        } else {
            Rlog.e(LOG_TAG,"enableCellBroadcast iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
        return false;
    }

    public boolean disableCellBroadcast(int messageIdentifier, int subscription)
                throws android.os.RemoteException {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier, subscription);
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int subscription)
                throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.disableCellBroadcastRange(startMessageId, endMessageId);
        } else {
            Rlog.e(LOG_TAG,"disableCellBroadcast iccSmsIntMgr is null for" +
                          " Subscription:"+subscription);
        }
       return false;
    }

    @Override
    public int getPremiumSmsPermission(String packageName, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getPremiumSmsPermission(packageName);
        } else {
            Rlog.e(LOG_TAG, "getPremiumSmsPermission iccSmsIntMgr is null");
        }
        //TODO Rakesh
        return 0;
    }

    @Override
    public void setPremiumSmsPermission(String packageName, int permission, int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            iccSmsIntMgr.setPremiumSmsPermission(packageName, permission);
        } else {
            Rlog.e(LOG_TAG, "setPremiumSmsPermission iccSmsIntMgr is null");
        }
    }

    @Override
    public boolean isImsSmsSupported(int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.isImsSmsSupported();
        } else {
            Rlog.e(LOG_TAG, "isImsSmsSupported iccSmsIntMgr is null");
        }
        return false;
    }

    @Override
    public String getImsSmsFormat(int subscription) {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);
        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getImsSmsFormat();
        } else {
            Rlog.e(LOG_TAG, "getImsSmsFormat iccSmsIntMgr is null");
        }
        return null;
    }

    /**
     * Get the capacity count of sms on Icc card.
     **/
    public int getSmsCapacityOnIcc(int subscription)
            throws android.os.RemoteException {
        MSimIccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subscription);

        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getSmsCapacityOnIcc();
        } else {
            Rlog.e(LOG_TAG, "iccSmsIntMgr is null for " + " subID: " + subscription);
            return -1;
        }
    }

    /**
     * get sms interface manager object based on subscription.
     **/
    private MSimIccSmsInterfaceManager getIccSmsInterfaceManager(int subscription) {
        try {
            return (MSimIccSmsInterfaceManager)
                ((MSimPhoneProxy)mPhone[subscription]).getIccSmsInterfaceManager();
        } catch (NullPointerException e) {
            Rlog.e(LOG_TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace(); //This will print stact trace
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace(); //This will print stack trace
            return null;
        }
    }

    /**
       Gets User preferred SMS subscription */
    public int getPreferredSmsSubscription() {
        return MSimPhoneFactory.getSMSSubscription();
    }

    /**
     * Get SMS prompt property,  enabled or not
     **/
    public boolean isSMSPromptEnabled() {
        return MSimPhoneFactory.isSMSPromptEnabled();
    }

}
