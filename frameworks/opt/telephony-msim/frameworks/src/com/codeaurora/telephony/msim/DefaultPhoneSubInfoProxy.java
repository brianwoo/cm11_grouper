/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

import android.telephony.Rlog;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.PhoneSubInfoProxy;

public class DefaultPhoneSubInfoProxy extends PhoneSubInfoProxy {
    private static final String TAG = "DefaultPhoneSubInfoProxy";
    private MSimPhoneProxy defaultPhone;

    public DefaultPhoneSubInfoProxy(PhoneSubInfo phoneSubInfo) {
        super(phoneSubInfo);
    }

    // update the default phone object which will be used by
    // getDeviceId and getDeviceSvn methods.
    public void updateDefaultPhone(Phone phone) {
        defaultPhone = (MSimPhoneProxy) phone;
    }

    // getDeviceId always returns device id of first subscription.
    @Override
    public String getDeviceId() {
        PhoneSubInfo phoneSubInfo = defaultPhone.getPhoneSubInfo();

        if (phoneSubInfo != null) {
            return phoneSubInfo.getDeviceId();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null");
            return null;
        }
    }

    @Override
    public String getDeviceSvn() {
        PhoneSubInfo phoneSubInfo = defaultPhone.getPhoneSubInfo();

        if (phoneSubInfo != null) {
            return phoneSubInfo.getDeviceSvn();
        } else {
            Rlog.e(TAG,"getDeviceSvn phoneSubInfoProxy is null");
            return null;
        }
    }
}
