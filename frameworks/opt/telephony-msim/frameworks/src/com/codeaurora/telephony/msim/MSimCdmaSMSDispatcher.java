/*
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import android.telephony.Rlog;

import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.ImsSMSDispatcher;


final class MSimCdmaSMSDispatcher extends CdmaSMSDispatcher {

    public MSimCdmaSMSDispatcher(PhoneBase phone, SmsUsageMonitor usageMonitor,
            ImsSMSDispatcher imsSMSDispatcher) {
        super(phone, usageMonitor, imsSMSDispatcher);
        Rlog.d(TAG, "MSimCdmaSMSDispatcher created");
    }

}
