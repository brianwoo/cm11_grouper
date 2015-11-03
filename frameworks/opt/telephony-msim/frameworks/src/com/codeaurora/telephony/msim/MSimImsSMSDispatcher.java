/*
 * Copyright (c) 2010-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.AppOpsManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Sms.Intents;
import android.telephony.Rlog;

import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.ImsSMSDispatcher;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.SmsBroadcastUndelivered;
import com.codeaurora.internal.telephony.msim.MSimCdmaInboundSmsHandler;
import com.codeaurora.internal.telephony.msim.MSimGsmInboundSmsHandler;



final class MSimImsSMSDispatcher extends ImsSMSDispatcher {
    private static final String TAG = "RIL_IMS";

    public MSimImsSMSDispatcher(PhoneBase phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor) {
        super(phone, storageMonitor, usageMonitor);
        Rlog.d(TAG, "MSimImsSMSDispatcher created");
    }

    @Override
    protected void initDispatchers(PhoneBase phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor) {
        Rlog.d(TAG, "MSimImsSMSDispatcher: initDispatchers()");
        mCdmaDispatcher = new MSimCdmaSMSDispatcher(phone, usageMonitor, this);
        mGsmInboundSmsHandler = MSimGsmInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                storageMonitor, phone);
        mCdmaInboundSmsHandler = MSimCdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                storageMonitor, phone, (CdmaSMSDispatcher) mCdmaDispatcher);
        mGsmDispatcher = new MSimGsmSMSDispatcher(phone, usageMonitor, this, mGsmInboundSmsHandler);
        Thread broadcastThread = new Thread(new SmsBroadcastUndelivered(phone.getContext(),
                mGsmInboundSmsHandler, mCdmaInboundSmsHandler));
        broadcastThread.start();

    }
}
