/*
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
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
import android.telephony.SmsCbMessage;
import android.telephony.Rlog;

import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.ImsSMSDispatcher;

final class MSimGsmSMSDispatcher extends GsmSMSDispatcher {
    private static final String TAG = "GSM";

    public MSimGsmSMSDispatcher(PhoneBase phone, SmsUsageMonitor usageMonitor,
            ImsSMSDispatcher imsSMSDispatcher, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, usageMonitor, imsSMSDispatcher, gsmInboundSmsHandler);
        Rlog.d(TAG, "MSimGsmSMSDispatcher created");
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        SubscriptionManager subMgr = SubscriptionManager.getInstance();
        if (subMgr != null) {
            Rlog.d(TAG, "MSimGsmSMSDispatcher: subId = " + mPhone.getSubscription()
                    + " slotId = " + mPhone.getSubscription());
            return  ((MSimUiccController) mUiccController).getUiccCardApplication(
                    SubscriptionManager.getInstance().getSlotId(mPhone.getSubscription()),
                    UiccController.APP_FAM_3GPP);
        }
        return null;
    }

}

