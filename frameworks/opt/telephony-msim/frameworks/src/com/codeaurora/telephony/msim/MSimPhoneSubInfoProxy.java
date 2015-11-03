/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.os.ServiceManager;

import android.telephony.Rlog;
import java.lang.NullPointerException;
import java.lang.ArrayIndexOutOfBoundsException;

import com.android.internal.telephony.msim.IPhoneSubInfoMSim;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneSubInfoProxy;

public class MSimPhoneSubInfoProxy extends IPhoneSubInfoMSim.Stub {
    private static final String TAG = "MSimPhoneSubInfoProxy";
    private Phone[] mPhone;

    public MSimPhoneSubInfoProxy(Phone[] phone) {
        mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo_msim") == null) {
            ServiceManager.addService("iphonesubinfo_msim", this);
        }
    }

    public String getDeviceId(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getDeviceId();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null" +
                      " for Subscription:"+subscription);
            return null;
        }
    }

    public String getDeviceSvn(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getDeviceSvn();
        } else {
            Rlog.e(TAG,"getDeviceId phoneSubInfoProxy is null" +
                      " for Subscription:"+subscription);
            return null;
        }
    }

    public String getSubscriberId(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getSubscriberId();
        } else {
            Rlog.e(TAG,"getSubscriberId phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    public String getIccSerialNumber(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getIccSerialNumber();
        } else {
            Rlog.e(TAG,"getIccSerialNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1Number(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getLine1Number();
        } else {
            Rlog.e(TAG,"getLine1Number phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getLine1AlphaTag(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getLine1AlphaTag();
        } else {
            Rlog.e(TAG,"getLine1AlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getMsisdn(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getMsisdn();
        } else {
            Rlog.e(TAG,"getMsisdn phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailNumber(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getVoiceMailNumber phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    public String getCompleteVoiceMailNumber(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getCompleteVoiceMailNumber();
        } else {
            Rlog.e(TAG,"getCompleteVoiceMailNumber phoneSubInfoProxy" +
                      " is null for Subscription:"+subscription);
            return null;
        }
    }

    public String getVoiceMailAlphaTag(int subscription) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subscription);
        if (phoneSubInfoProxy != null) {
            return getPhoneSubInfoProxy(subscription).getVoiceMailAlphaTag();
        } else {
            Rlog.e(TAG,"getVoiceMailAlphaTag phoneSubInfoProxy is" +
                      " null for Subscription:"+subscription);
            return null;
        }
    }

    /**
     * get Phone sub info proxy object based on subscription.
     **/
    private PhoneSubInfoProxy getPhoneSubInfoProxy(int subscription) {
        try {
            return ((MSimPhoneProxy)mPhone[subscription]).getPhoneSubInfoProxy();
        } catch (NullPointerException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace();
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subscription );
            e.printStackTrace();
            return null;
        }
    }
}
