/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
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

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.telephony.Rlog;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_APN_SIM_OPERATOR_NUMERIC;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_SIM_STATE;

public class MSimIccCardProxy extends IccCardProxy {
    private static final String LOG_TAG = "RIL_MSimIccCardProxy";
    private static final boolean DBG = true;

    private static final int EVENT_ICC_RECORD_EVENTS = 500;
    private static final int EVENT_SUBSCRIPTION_ACTIVATED = 501;
    private static final int EVENT_SUBSCRIPTION_DEACTIVATED = 502;

    private Integer mCardIndex = null;
    private Subscription mSubscriptionData = null;

    public MSimIccCardProxy(Context context, CommandsInterface ci, int cardIndex) {
        super(context, ci);

        mCardIndex = cardIndex;

        //TODO: Card index and subscription are same???
        SubscriptionManager subMgr = SubscriptionManager.getInstance();
        subMgr.registerForSubscriptionActivated(mCardIndex,
                this, EVENT_SUBSCRIPTION_ACTIVATED, null);
        subMgr.registerForSubscriptionDeactivated(mCardIndex,
                this, EVENT_SUBSCRIPTION_DEACTIVATED, null);

        resetProperties();
        setExternalState(State.NOT_READY, false);
    }

    @Override
    public void dispose() {
        super.dispose();
        resetProperties();
    }

    @Override
    public void handleMessage(Message msg) {
        if (mExternalState != State.READY &&
            (msg.what == EVENT_RECORDS_LOADED || msg.what == EVENT_IMSI_READY)) {
            //Do not process LOADED or IMSI if app is not in READY State.
            log("App State is not READY, So ignore LOADED or IMSI event.");
            return;
        }
        switch (msg.what) {
            case EVENT_SUBSCRIPTION_ACTIVATED:
                log("EVENT_SUBSCRIPTION_ACTIVATED");
                onSubscriptionActivated();
                break;

            case EVENT_SUBSCRIPTION_DEACTIVATED:
                log("EVENT_SUBSCRIPTION_DEACTIVATED");
                onSubscriptionDeactivated();
                break;

            case EVENT_RECORDS_LOADED:
                if (mIccRecords != null) {
                    String operator = mIccRecords.getOperatorNumeric();
                    int sub = mCardIndex;

                    log("operator = " + operator + " SUB = " + sub);

                    if (operator != null) {
                        MSimTelephonyManager.setTelephonyProperty(
                                PROPERTY_ICC_OPERATOR_NUMERIC, sub, operator);
                        if (mCurrentAppType == UiccController.APP_FAM_3GPP) {
                            MSimTelephonyManager.setTelephonyProperty(
                                    PROPERTY_APN_SIM_OPERATOR_NUMERIC, sub, operator);
                        }
                        String countryCode = operator.substring(0,3);
                        if (countryCode != null) {
                            MSimTelephonyManager.setTelephonyProperty(
                                    PROPERTY_ICC_OPERATOR_ISO_COUNTRY, sub,
                                    MccTable.countryCodeForMcc(Integer.parseInt(countryCode)));
                        } else {
                            loge("EVENT_RECORDS_LOADED Country code is null");
                        }

                        // Update MCC MNC device configuration information only for default sub.
                        if (sub == TelephonyManager.getDefaultSubscription()) {
                            log("Update mccmnc config for default subscription.");
                            MccTable.updateMccMncConfiguration(mContext, operator, false);
                        }
                    } else {
                        loge("EVENT_RECORDS_LOADED Operator name is null");
                    }
                }
                broadcastIccStateChangedIntent(IccCardConstants.INTENT_VALUE_ICC_LOADED, null);
                break;

            case EVENT_ICC_RECORD_EVENTS:
                if ((mCurrentAppType == UiccController.APP_FAM_3GPP) && (mIccRecords != null)) {
                    int sub = mCardIndex;
                    AsyncResult ar = (AsyncResult)msg.obj;
                    int eventCode = (Integer) ar.result;
                    if (eventCode == SIMRecords.EVENT_SPN) {
                        MSimTelephonyManager.setTelephonyProperty(
                                PROPERTY_ICC_OPERATOR_ALPHA, sub,
                                mIccRecords.getServiceProviderName());
                    }
                }
                break;

            default:
                super.handleMessage(msg);
        }
    }

    private void onSubscriptionActivated() {
        SubscriptionManager subMgr = SubscriptionManager.getInstance();
        mSubscriptionData = subMgr.getCurrentSubscription(mCardIndex);

        updateIccAvailability();
        updateStateProperty();
    }

    private void onSubscriptionDeactivated() {
        resetProperties();
        mSubscriptionData = null;
        updateIccAvailability();
        updateStateProperty();
    }


    @Override
    protected void updateIccAvailability() {
        synchronized (mLock) {
            UiccCard newCard = ((MSimUiccController) mUiccController).getUiccCard(mCardIndex);
            CardState state = CardState.CARDSTATE_ABSENT;
            UiccCardApplication newApp = null;
            IccRecords newRecords = null;
            if (newCard != null) {
                state = newCard.getCardState();
                log("Card State = " + state);
                newApp = newCard.getApplication(mCurrentAppType);
                if (newApp != null) {
                    newRecords = newApp.getIccRecords();
                }
            }

            if (mIccRecords != newRecords || mUiccApplication != newApp || mUiccCard != newCard) {
                if (DBG) log("Icc changed. Reregestering.");
                unregisterUiccCardEvents();
                mUiccCard = newCard;
                mUiccApplication = newApp;
                mIccRecords = newRecords;
                updateproperty();
                registerUiccCardEvents();
                updateActiveRecord();
            }

            updateExternalState();
        }
    }

    protected void updateproperty(){
        if (mIccRecords == null) {
            log("EVENT_RECORDS_LOADED null mIccRecords");
        } else {
            String operator = mIccRecords.getOperatorNumeric();
            int sub = mCardIndex;
            log("operator = " + operator + " SUB = " + sub);

            if (operator != null && mIccRecords.getRecordsLoaded()) {
                MSimTelephonyManager.setTelephonyProperty(
                        PROPERTY_ICC_OPERATOR_NUMERIC, sub, operator);
                MSimTelephonyManager.setTelephonyProperty(
                        PROPERTY_APN_SIM_OPERATOR_NUMERIC, sub, operator);
                String countryCode = operator.substring(0,3);
                if (countryCode != null) {
                    MSimTelephonyManager.setTelephonyProperty(PROPERTY_ICC_OPERATOR_ISO_COUNTRY,
                            sub, MccTable.countryCodeForMcc(Integer.parseInt(countryCode)));
                } else {
                    loge("EVENT_RECORDS_LOADED Country code is null");
                }

                // Update MCC MNC device configuration information only for default sub.
                if (sub == TelephonyManager.getDefaultSubscription()) {
                    log("Update mccmnc config for default subscription.");
                    MccTable.updateMccMncConfiguration(mContext, operator, false);
                }
            } else {
                loge("EVENT_RECORDS_LOADED Operator name = " + operator + ", loaded = "
                        + mIccRecords.getRecordsLoaded());
            }
        }
    }

    void resetProperties() {
        if (mCurrentAppType == UiccController.APP_FAM_3GPP) {
            MSimTelephonyManager.setTelephonyProperty(
                    PROPERTY_APN_SIM_OPERATOR_NUMERIC, mCardIndex, "");
            MSimTelephonyManager.setTelephonyProperty(
                    PROPERTY_ICC_OPERATOR_NUMERIC, mCardIndex, "");
            MSimTelephonyManager.setTelephonyProperty(
                    PROPERTY_ICC_OPERATOR_ISO_COUNTRY, mCardIndex, "");
            MSimTelephonyManager.setTelephonyProperty(
                    PROPERTY_ICC_OPERATOR_ALPHA, mCardIndex, "");
         }
    }

    private void updateStateProperty() {
        MSimTelephonyManager.setTelephonyProperty(PROPERTY_SIM_STATE,
                SubscriptionManager.getInstance().getSlotId(mCardIndex),getState().toString());
    }

    @Override
    protected void registerUiccCardEvents() {
        super.registerUiccCardEvents();
        if (mIccRecords != null) {
            mIccRecords.registerForRecordsEvents(this, EVENT_ICC_RECORD_EVENTS, null);
        }
    }

    @Override
    protected void unregisterUiccCardEvents() {
        super.unregisterUiccCardEvents();
        if (mIccRecords != null) mIccRecords.unregisterForRecordsEvents(this);
    }

    @Override
    public void broadcastIccStateChangedIntent(String value, String reason) {
        synchronized (mLock) {
            if (mCardIndex == null) {
                loge("broadcastIccStateChangedIntent: Card Index is not set; Return!!");
                return;
            }

            int subId = mCardIndex;
            if (mQuietMode) {
                log("QuietMode: NOT Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                        + " reason " + reason);
                return;
            }

            Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            intent.putExtra(PhoneConstants.PHONE_NAME_KEY, "Phone");
            intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE, value);
            intent.putExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON, reason);

            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subId);
            log("Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                + " reason " + reason + " for subscription : " + subId);
            ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE,
                    UserHandle.USER_ALL);
        }
    }

    @Override
    protected void setExternalState(State newState, boolean override) {
        synchronized (mLock) {
            if (mCardIndex == null) {
                loge("setExternalState: Card Index is not set; Return!!");
                return;
            }

            if (!override && newState == mExternalState) {
                return;
            }
            mExternalState = newState;
            MSimTelephonyManager.setTelephonyProperty(PROPERTY_SIM_STATE,
                    mCardIndex, getState().toString());
            broadcastIccStateChangedIntent(getIccStateIntentString(mExternalState),
                    getIccStateReason(mExternalState));
            // TODO: Need to notify registrants for other states as well.
            if ( State.ABSENT == mExternalState) {
                mAbsentRegistrants.notifyRegistrants();
            }
        }
    }

    @Override
    protected void HandleDetectedState() {
        setExternalState(State.DETECTED, false);
    }

    @Override
    protected void log(String msg) {
        if (DBG) Rlog.d(LOG_TAG, "[CardIndex:" + mCardIndex + "]" + msg);
    }

    @Override
    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[CardIndex:" + mCardIndex + "]" + msg);
    }
}
