/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2013 The Android Open Source Project
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

package com.codeaurora.internal.telephony.msim;


import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsCbMessage;

import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;


public class MSimCdmaCellBroadcastHandler extends CellBroadcastHandler {

    protected MSimCdmaCellBroadcastHandler(Context context, PhoneBase phone) {
        super("MSimCdmaCellBroadcastHandler", context, phone);
    }

    public static MSimCdmaCellBroadcastHandler makeMSimCdmaCellBroadcastHandler(Context context,
            PhoneBase phone) {
        MSimCdmaCellBroadcastHandler handler = new MSimCdmaCellBroadcastHandler(context, phone);
        handler.start();
        return handler;
    }

    /**
     * Dispatch a Cell Broadcast message to listeners.
     * @param message the Cell Broadcast to broadcast
     */
    @Override
    protected void handleBroadcastSms(SmsCbMessage message) {
        String receiverPermission;
        int appOp;
        Intent intent;
        if (message.isEmergencyMessage()) {
            log("Dispatching emergency SMS CB");
            intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
            receiverPermission = Manifest.permission.RECEIVE_EMERGENCY_BROADCAST;
            appOp = AppOpsManager.OP_RECEIVE_EMERGECY_SMS;
        } else {
            log("Dispatching SMS CB");
            intent = new Intent(Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION);
            receiverPermission = Manifest.permission.RECEIVE_SMS;
            appOp = AppOpsManager.OP_RECEIVE_SMS;
        }
        intent.putExtra("message", message);
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                mPhone.getSubscription()); //Subscription information to be passed in an intent
        mContext.sendOrderedBroadcast(intent, receiverPermission, appOp, mReceiver,
                getHandler(), Activity.RESULT_OK, null, null);
    }
}
