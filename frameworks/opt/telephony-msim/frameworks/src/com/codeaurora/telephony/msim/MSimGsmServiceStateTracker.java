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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.MSimTelephonyManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gsm.GsmServiceStateTracker;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

import static com.android.internal.telephony.MSimConstants.DEFAULT_SUBSCRIPTION;

/**
 * {@hide}
 */
public final class MSimGsmServiceStateTracker extends GsmServiceStateTracker {

    static final String LOG_TAG = "GsmSST";
    static final boolean DBG = true;
    protected static final int EVENT_ALL_DATA_DISCONNECTED = 1001;

    public MSimGsmServiceStateTracker(GSMPhone phone) {
        super(phone);
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        return  ((MSimUiccController) mUiccController).getUiccCardApplication(SubscriptionManager.
                getInstance().getSlotId(((MSimGSMPhone)mPhone).getSubscription()),
                UiccController.APP_FAM_3GPP);
    }

    @Override
    protected void updateSpnDisplay() {
        // The values of plmn/showPlmn change in different scenarios.
        // 1) No service but emergency call allowed -> expected
        //    to show "Emergency call only"
        //    EXTRA_SHOW_PLMN = true
        //    EXTRA_PLMN = "Emergency call only"

        // 2) No service at all --> expected to show "No service"
        //    EXTRA_SHOW_PLMN = true
        //    EXTRA_PLMN = "No service"

        // 3) Normal operation in either home or roaming service
        //    EXTRA_SHOW_PLMN = depending on IccRecords rule
        //    EXTRA_PLMN = plmn

        // 4) No service due to power off, aka airplane mode
        //    EXTRA_SHOW_PLMN = false
        //    EXTRA_PLMN = null

        IccRecords iccRecords = mIccRecords;
        String plmn = null;
        boolean showPlmn = false;
        int rule = (iccRecords != null) ? iccRecords.getDisplayRule(mSS.getOperatorNumeric()) : 0;
        if (mSS.getState() == ServiceState.STATE_OUT_OF_SERVICE
                || mSS.getState() == ServiceState.STATE_EMERGENCY_ONLY) {
            showPlmn = true;
            if (mEmergencyOnly) {
                // No service but emergency call allowed
                plmn = Resources.getSystem().
                        getText(com.android.internal.R.string.emergency_calls_only).toString();
            } else {
                // No service at all
                plmn = Resources.getSystem().
                        getText(com.android.internal.R.string.lockscreen_carrier_default).toString();
            }
            if (DBG) log("updateSpnDisplay: radio is on but out " +
                    "of service, set plmn='" + plmn + "'");
        } else if (mSS.getState() == ServiceState.STATE_IN_SERVICE) {
            // In either home or roaming service
            plmn = mSS.getOperatorAlphaLong();
            showPlmn = !TextUtils.isEmpty(plmn) &&
                    ((rule & SIMRecords.SPN_RULE_SHOW_PLMN)
                            == SIMRecords.SPN_RULE_SHOW_PLMN);
        } else {
            // Power off state, such as airplane mode
            if (DBG) log("updateSpnDisplay: radio is off w/ showPlmn="
                    + showPlmn + " plmn=" + plmn);
        }

        // The value of spn/showSpn are same in different scenarios.
        //    EXTRA_SHOW_SPN = depending on IccRecords rule
        //    EXTRA_SPN = spn
        String spn = (iccRecords != null) ? iccRecords.getServiceProviderName() : "";
        boolean showSpn = !TextUtils.isEmpty(spn)
                && ((rule & SIMRecords.SPN_RULE_SHOW_SPN)
                        == SIMRecords.SPN_RULE_SHOW_SPN);

        // Update SPN_STRINGS_UPDATED_ACTION IFF any value changes
        if (showPlmn != mCurShowPlmn
                || showSpn != mCurShowSpn
                || !TextUtils.equals(spn, mCurSpn)
                || !TextUtils.equals(plmn, mCurPlmn)) {
            if (DBG) {
                log(String.format("updateSpnDisplay: changed" +
                        " sending intent rule=" + rule +
                        " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                        showPlmn, plmn, showSpn, spn));
            }
            Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, showSpn);
            intent.putExtra(TelephonyIntents.EXTRA_SPN, spn);
            intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);
            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, mPhone.getSubscription());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        mCurShowSpn = showSpn;
        mCurShowPlmn = showPlmn;
        mCurSpn = spn;
        mCurPlmn = plmn;
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        return MSimTelephonyManager.getTelephonyProperty(
                property, mPhone.getSubscription(), defValue);
    }

    /**
     * Clean up existing voice and data connection then turn off radio power.
     *
     * Hang up the existing voice calls to decrease call drop rate.
     */
    @Override
    public void powerOffRadioSafely(DcTrackerBase dcTracker) {
        synchronized (this) {
            if (!mPendingRadioPowerOffAfterDataOff) {
                int dds = MSimPhoneFactory.getDataSubscription();
                // To minimize race conditions we call cleanUpAllConnections on
                // both if else paths instead of before this isDisconnected test.
                if (dcTracker.isDisconnected()
                        && (dds == mPhone.getSubscription()
                            || (dds != mPhone.getSubscription()
                                && MSimProxyManager.getInstance().isDataDisconnected(dds)))) {
                    // To minimize race conditions we do this after isDisconnected
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    if (DBG) log("Data disconnected, turn off radio right away.");
                    hangupAndPowerOff();
                } else {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    if (dds != mPhone.getSubscription()
                            && !MSimProxyManager.getInstance().isDataDisconnected(dds)) {
                        if (DBG) log("Data is active on DDS.  Wait for all data disconnect");
                        // Data is not disconnected on DDS. Wait for the data disconnect complete
                        // before sending the RADIO_POWER off.
                        MSimProxyManager.getInstance().registerForAllDataDisconnected(dds, this,
                                EVENT_ALL_DATA_DISCONNECTED, null);
                        mPendingRadioPowerOffAfterDataOff = true;
                    }
                    Message msg = Message.obtain(this);
                    msg.what = EVENT_SET_RADIO_POWER_OFF;
                    msg.arg1 = ++mPendingRadioPowerOffAfterDataOffTag;
                    if (sendMessageDelayed(msg, 30000)) {
                        if (DBG) log("Wait upto 30s for data to disconnect, then turn off radio.");
                        mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_ALL_DATA_DISCONNECTED:
                int dds = MSimPhoneFactory.getDataSubscription();
                MSimProxyManager.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized(this) {
                    if (mPendingRadioPowerOffAfterDataOff) {
                        if (DBG) log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                }
                break;

            case EVENT_NITZ_TIME:
                if (!(SystemProperties.getBoolean("persist.timed.enable", false))) {
                    SubscriptionManager subMgr = SubscriptionManager.getInstance();
                    log("EVENT_NITZ_TIME received phone type ::" + MSimTelephonyManager.
                            getDefault().getCurrentPhoneType(DEFAULT_SUBSCRIPTION) +
                            "is cdma sub active ::" + subMgr.isSubActive(DEFAULT_SUBSCRIPTION));
                    if (TelephonyManager.PHONE_TYPE_CDMA == MSimTelephonyManager.
                            getDefault().getCurrentPhoneType(DEFAULT_SUBSCRIPTION) &&
                            subMgr.isSubActive(DEFAULT_SUBSCRIPTION)) {
                        log("EVENT_NITZ_TIME received in c + g ignore updating time");
                    }
                } else {
                    super.handleMessage(msg);
                }
                break;

            default:
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[MSimGsmSST] [SUB : " + mPhone.getSubscription() + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[MSimGsmSST] [SUB : " + mPhone.getSubscription() + "] " + s);
    }
}
