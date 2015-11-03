/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.codeaurora.telephony.msim;

import android.telephony.Rlog;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneSubInfo;

/**
 * This class holds the instances of default phone sub info
 * proxy and default icc sms interface manager objects.
 * Create instance for this object before PhoneProxy creation,
 * that helps to register "iphonesubinfo" and "isms" services by using
 * sDefaultIccSmsInterfaceManager and sDefaultPhoneSubInfoProxy objects.
 */
public class DefaultPhoneProxy {
    static final String LOG_TAG = "DefaultPhoneProxy";

    static private MSimDefaultIccSmsInterfaceManager sDefaultIccSmsInterfaceManager = null;
    static private DefaultPhoneSubInfoProxy sDefaultPhoneSubInfoProxy = null;

    public DefaultPhoneProxy(Phone phone) {
        sDefaultIccSmsInterfaceManager = new MSimDefaultIccSmsInterfaceManager();
        sDefaultPhoneSubInfoProxy = new DefaultPhoneSubInfoProxy(phone.getPhoneSubInfo());
    }

    // Update the default phone(i.e SUB0 phone) in DefaultPhoneSubInfoProxy
    public void updateDefaultPhoneInSubInfo(Phone phone) {
        if (sDefaultPhoneSubInfoProxy != null) {
            Rlog.d(LOG_TAG, "update default Phone object in subInfo");
            sDefaultPhoneSubInfoProxy.updateDefaultPhone(phone);
        }
    }

    // Update the phone sub info corresponds to current default phone
    public void updatePhoneSubInfo(PhoneSubInfo phoneSubInfo) {
        if (sDefaultPhoneSubInfoProxy != null) {
            Rlog.d(LOG_TAG, "updatePhoneSubInfo object");
            sDefaultPhoneSubInfoProxy.setmPhoneSubInfo(phoneSubInfo);
        }
    }

    // Update SMS preferred subscription in MSimDefaultIccSmsInterfaceManager
    public void updateDefaultSMSIntfManager(int subscription) {
        if (sDefaultIccSmsInterfaceManager != null) {
            MSimPhoneProxy phone = (MSimPhoneProxy) MSimPhoneFactory.getPhone(subscription);

            Rlog.d(LOG_TAG, "updateDefaultSMSIntfManager: sub = " + subscription);
            // Update the defaut phone in object
            sDefaultIccSmsInterfaceManager.updatePhoneProxyObject(phone);
        }
    }
}
