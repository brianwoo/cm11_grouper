/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;

import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;

/**
 * Class that implements SIM Toolkit Telephony Service. Interacts with the RIL
 * and application.
 *
 * {@hide}
 */
public class MSimCatService extends CatService {

    // Class members
    private HandlerThread mHandlerThread;
    private int mSlotId;

    /* For multisim catservice should not be singleton */
    private MSimCatService(CommandsInterface ci, UiccCardApplication ca, IccRecords ir,
            Context context, IccFileHandler fh, MSimUiccCard ic, int slotId) {
        if (ci == null || ca == null || ir == null || context == null || fh == null
                || ic == null) {
            throw new NullPointerException(
                    "Service: Input parameters must not be null");
        }
        mCmdIf = ci;
        mContext = context;
        mSlotId = slotId;
        mHandlerThread = new HandlerThread("Cat Telephony service" + slotId);
        mHandlerThread.start();

        // Get the RilMessagesDecoder for decoding the messages.
        mMsgDecoder = new MSimRilMessageDecoder(this, fh);
        mMsgDecoder.start();

        // Register ril events handling.
        mCmdIf.setOnCatSessionEnd(this, MSG_ID_SESSION_END, null);
        mCmdIf.setOnCatProactiveCmd(this, MSG_ID_PROACTIVE_COMMAND, null);
        mCmdIf.setOnCatEvent(this, MSG_ID_EVENT_NOTIFY, null);
        mCmdIf.setOnCatCallSetUp(this, MSG_ID_CALL_SETUP, null);
        mCmdIf.setOnCatSendSmsResult(this, MSG_ID_SEND_SMS_RESULT, null); // Samsung STK
        //mCmdIf.setOnSimRefresh(this, MSG_ID_REFRESH, null);

        mCmdIf.registerForIccRefresh(this, MSG_ID_ICC_REFRESH, null);
        mCmdIf.setOnCatCcAlphaNotify(this, MSG_ID_ALPHA_NOTIFY, null);
        mIccRecords = ir;
        mUiccApplication = ca;

        // Register for SIM ready event.
        CatLog.d(this, "registerForReady slotid: " + mSlotId + "instance : " + this);
        mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);

        mUiccController = MSimUiccController.getInstance();
        if (mUiccController != null) {
            mUiccController.registerForIccChanged(this, MSG_ID_ICC_CHANGED, null);
        } else {
            CatLog.d(this, "UiccController instance is null");
        }

        // Check if STK application is availalbe
        mStkAppInstalled = isStkAppInstalled();

        CatLog.d(this, "Running CAT service on Slotid: " + mSlotId +
                ". STK app installed:" + mStkAppInstalled);
    }

    /**
     * Used for instantiating the Service from the Card.
     *
     * @param ci CommandsInterface object
     * @param context phone app context
     * @param ic Icc card
     * @param slotId to know the index of card
     * @return The only Service object in the system
     */
    public static CatService getInstance(CommandsInterface ci,
            Context context, MSimUiccCard ic, int slotId) {
        UiccCardApplication ca = null;
        IccFileHandler fh = null;
        IccRecords ir = null;
        if (ic != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            for (int i = 0; i < ic.getNumApplications(); i++) {
                ca = ic.getApplicationIndex(i);
                if (ca != null && (ca.getType() != AppType.APPTYPE_UNKNOWN)) {
                    fh = ca.getIccFileHandler();
                    ir = ca.getIccRecords();
                    break;
                }
            }
        }

        if (ci == null || ca == null || ir == null || context == null || fh == null
                || ic == null) {
            return null;
        }

        return new MSimCatService(ci, ca, ir, context, fh, ic, slotId);
    }

    public void update(CommandsInterface ci,
            Context context, MSimUiccCard ic) {
        UiccCardApplication ca = null;
        IccRecords ir = null;

        if (ic != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            ca = ic.getApplicationIndex(0);
            if (ca != null) {
                ir = ca.getIccRecords();
            }
        }

        synchronized (sInstanceLock) {
            if ((ir != null) && (mIccRecords != ir)) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }

                if (mUiccApplication != null) {
                    CatLog.d(this, "unregisterForReady slotid: " + mSlotId + "instance : " + this);
                    mUiccApplication.unregisterForReady(this);
                }
                CatLog.d(this,
                        "Reinitialize the Service with SIMRecords and UiccCardApplication");
                mIccRecords = ir;
                mUiccApplication = ca;

                // re-Register for SIM ready event.
                mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);
                CatLog.d(this, "registerForReady slotid: " + mSlotId + "instance : " + this);
            }
        }
    }

    @Override
    protected void disposeHandlerThread() {
        mHandlerThread.quit();
        mHandlerThread = null;
    }

    @Override
    protected void broadcastCatCmdIntent(CatCmdMessage cmdMsg) {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("STK CMD", cmdMsg);
        intent.putExtra("SLOT_ID", mSlotId);
        CatLog.d(this, "Sending CmdMsg: " + cmdMsg+ " on slotid:" + mSlotId);
        mContext.sendBroadcast(intent);
    }

    /**
     * Handles RIL_UNSOL_STK_SESSION_END unsolicited command from RIL.
     *
     */
    @Override
    protected void handleSessionEnd() {
        CatLog.d(this, "SESSION END on "+ mSlotId);

        mCurrntCmd = mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("SLOT_ID", mSlotId);
        mContext.sendBroadcast(intent);
    }

    /**
     ** This function sends a CARD status (ABSENT, PRESENT, REFRESH) to STK_APP.
     ** This is triggered during ICC_REFRESH or CARD STATE changes. In case
     ** REFRESH, additional information is sent in 'refresh_result'
     **
     **/
    @Override
    protected void  broadcastCardStateAndIccRefreshResp(CardState cardState,
            IccRefreshResponse IccRefreshState) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        boolean cardStatus = (cardState == CardState.CARDSTATE_PRESENT);

        if (IccRefreshState != null) {
            //This case is when MSG_ID_ICC_REFRESH is received.
            intent.putExtra(AppInterface.REFRESH_RESULT,IccRefreshState.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: "
                    + IccRefreshState.refreshResult);
        }

        // This sends an intent with CARD_ABSENT (0 - false) /CARD_PRESENT (1 - true).
        intent.putExtra(AppInterface.CARD_STATUS, cardStatus);
        intent.putExtra("SLOT_ID", mSlotId);
        CatLog.d(this, "Sending Card Status: " + cardState + " " + "cardStatus: " + cardStatus +
                " on slotid: " + mSlotId);

        mContext.sendBroadcast(intent);
    }

    @Override
    protected void updateIccAvailability() {
        CardState newState = CardState.CARDSTATE_ABSENT;
        if (null == mUiccController) {
            return;
        }
        UiccCard newCard = ((MSimUiccController) mUiccController).getUiccCard(mSlotId);
        if (newCard != null) {
            newState = newCard.getCardState();
        }
        CardState oldState = mCardState;
        mCardState = newState;
        CatLog.d(this, "New Card State = " + newState + " " + "Old Card State = " + oldState +
                " on slotid: " + mSlotId);
        if (oldState == CardState.CARDSTATE_PRESENT &&
                newState != CardState.CARDSTATE_PRESENT) {
            broadcastCardStateAndIccRefreshResp(newState, null);
        } else if (oldState != CardState.CARDSTATE_PRESENT &&
                newState == CardState.CARDSTATE_PRESENT) {
            // Card moved to PRESENT STATE.
            mCmdIf.reportStkServiceIsRunning(null);
        }
    }

    @Override
    protected void broadcastAlphaMessage(String alphaString) {
        CatLog.d(this, "Broadcasting STK Alpha message from card: " + alphaString +
                " on slotid: " + mSlotId);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(AppInterface.ALPHA_STRING, alphaString);
        intent.putExtra("SLOT_ID", mSlotId);
        mContext.sendBroadcast(intent);
    }
}
