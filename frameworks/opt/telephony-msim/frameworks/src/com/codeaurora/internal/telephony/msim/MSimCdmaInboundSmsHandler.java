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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;

import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SmsStorageMonitor;

public class MSimCdmaInboundSmsHandler extends CdmaInboundSmsHandler {

    private MSimCdmaInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor,
            PhoneBase phone, CdmaSMSDispatcher smsDispatcher) {
        super(context, storageMonitor, phone, smsDispatcher);
    }

    @Override
    protected void init(Context context, PhoneBase phone) {
        mCellBroadcastHandler = MSimCdmaCellBroadcastHandler.makeMSimCdmaCellBroadcastHandler(
                context, phone);
        mServiceCategoryProgramHandler = MSimCdmaServiceCategoryProgramHandler.makeMSimScpHandler(
            context, phone);
    }

    public static MSimCdmaInboundSmsHandler makeInboundSmsHandler(Context context,
            SmsStorageMonitor storageMonitor, PhoneBase phone, CdmaSMSDispatcher smsDispatcher) {
        MSimCdmaInboundSmsHandler handler = new MSimCdmaInboundSmsHandler(context, storageMonitor,
                phone, smsDispatcher);
        handler.start();
        return handler;
    }

    /**
     * Dispatch the intent with the specified permission, appOp, and result receiver, using
     * this state machine's handler thread to run the result receiver.
     *
     * @param intent the intent to broadcast
     * @param permission receivers are required to have this permission
     * @param appOp app op that is being performed when dispatching to a receiver
     */
    @Override
    protected void dispatchIntent(Intent intent, String permission, int appOp,
            BroadcastReceiver resultReceiver) {
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                mPhone.getSubscription()); //Subscription information to be passed in an intent
        mContext.sendOrderedBroadcast(intent, permission, appOp, resultReceiver,
                getHandler(), Activity.RESULT_OK, null, null);
    }
}
