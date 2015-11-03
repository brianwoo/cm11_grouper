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
import android.provider.Telephony.Sms.Intents;
import android.telephony.cdma.CdmaSmsCbProgramData;
import android.telephony.PhoneNumberUtils;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cdma.CdmaServiceCategoryProgramHandler;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;

import java.util.ArrayList;

public final class MSimCdmaServiceCategoryProgramHandler extends CdmaServiceCategoryProgramHandler {

    MSimCdmaServiceCategoryProgramHandler(Context context, PhoneBase phone) {
        super(context, phone.mCi);
        mPhone = phone;
    }

    static MSimCdmaServiceCategoryProgramHandler makeMSimScpHandler(Context context,
            PhoneBase phone) {
        MSimCdmaServiceCategoryProgramHandler handler = new MSimCdmaServiceCategoryProgramHandler(
                context, phone);
        handler.start();
        return handler;
    }

    /**
     * Send SCPD request to CellBroadcastReceiver as an ordered broadcast.
     * @param sms the CDMA SmsMessage containing the SCPD request
     * @return true if an ordered broadcast was sent; false on failure
     */
    @Override
    protected boolean handleServiceCategoryProgramData(SmsMessage sms) {
        ArrayList<CdmaSmsCbProgramData> programDataList = sms.getSmsCbProgramData();
        if (programDataList == null) {
            loge("handleServiceCategoryProgramData: program data list is null!");
            return false;
        }

        Intent intent = new Intent(Intents.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION);
        intent.putExtra("sender", sms.getOriginatingAddress());
        intent.putParcelableArrayListExtra("program_data", programDataList);
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                mPhone.getSubscription()); //Subscription information to be passed in an intent

        mContext.sendOrderedBroadcast(intent, Manifest.permission.RECEIVE_SMS,
                AppOpsManager.OP_RECEIVE_SMS, mScpResultsReceiver,
                getHandler(), Activity.RESULT_OK, null, null);
        return true;
    }
}
