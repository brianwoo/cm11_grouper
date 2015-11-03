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

import android.content.Context;
import android.content.Intent;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CellInfo;
import android.telephony.MSimTelephonyManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.ITelephonyRegistryMSim;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

/**
 * broadcast intents
 */
public class MSimDefaultPhoneNotifier extends DefaultPhoneNotifier {
    static final String LOG_TAG = "GSM";
    private ITelephonyRegistryMSim mMSimRegistry;

    /*package*/
    MSimDefaultPhoneNotifier() {
        mMSimRegistry = ITelephonyRegistryMSim.Stub.asInterface(ServiceManager.getService(
                "telephony.msim.registry"));
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                "telephony.registry"));
    }

    @Override
    public void notifyPhoneState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        int subscription = sender.getSubscription();
        String incomingNumber = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null){
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            mMSimRegistry.notifyCallState(
                    convertCallState(sender.getState()), incomingNumber, subscription);
        } catch (RemoteException ex) {
            // system process is dead
        }
        notifyCallStateToTelephonyRegistry(sender);
    }

    /*
     *  Suppose, some third party app e.g. FM app registers for a call state changed indication
     *  through TelephonyManager/PhoneStateListener and an incoming call is received on sub1 or
     *  sub2. Then ir-respective of sub1/sub2 FM app should be informed of call state
     *  changed(onCallStateChanged()) indication so that FM app can be paused.
     *  Hence send consolidated call state information to apps. (i.e. sub1 or sub2 active
     *  call state,  in priority order RINGING > OFFHOOK > IDLE)
     */
    public void notifyCallStateToTelephonyRegistry(Phone sender) {
        Call ringingCall = null;
        CallManager cm = CallManager.getInstance();
        PhoneConstants.State state = sender.getState();
        String incomingNumber = "";
        for (Phone phone : cm.getAllPhones()) {
            if (phone.getState() == PhoneConstants.State.RINGING) {
                ringingCall = phone.getRingingCall();
                if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
                    incomingNumber = ringingCall.getEarliestConnection().getAddress();
                }
                sender = phone;
                state = PhoneConstants.State.RINGING;
                break;
            } else if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                if (state == PhoneConstants.State.IDLE) {
                    state = PhoneConstants.State.OFFHOOK;
                    sender = phone;
                }
            }
        }
        log("notifyCallStateToTelephonyRegistry, subscription = " + sender.getSubscription()
                + " state = " + state);
        try {
            mRegistry.notifyCallState(convertCallState(state), incomingNumber);
        } catch (RemoteException ex) {
            // system process is dead
        }
        broadcastCallStateChanged(convertCallState(state), incomingNumber, sender);
    }

    private void broadcastCallStateChanged(int state, String incomingNumber, Phone phone) {
        int subscription = phone.getSubscription();
        log("broadcastCallStateChanged, subscription = " + subscription);
        Intent intent = new Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intent.putExtra(PhoneConstants.STATE_KEY,
                DefaultPhoneNotifier.convertCallState(state).toString());
        if (!TextUtils.isEmpty(incomingNumber)) {
            intent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, incomingNumber);
        }
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subscription);
        phone.getContext().sendBroadcast(intent,
                android.Manifest.permission.READ_PHONE_STATE);
    }

    @Override
    public void notifyServiceState(Phone sender) {
        ServiceState ss = sender.getServiceState();
        int subscription = sender.getSubscription();
        if (ss == null) {
            ss = new ServiceState();
            ss.setStateOutOfService();
        }
        try {
            mMSimRegistry.notifyServiceState(ss, subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyServiceState(ss);
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifySignalStrength(Phone sender) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifySignalStrength(sender.getSignalStrength(), subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifySignalStrength(sender.getSignalStrength());
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyMessageWaitingChanged(Phone sender) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifyMessageWaitingChanged(
                    sender.getMessageWaitingIndicator(),
                    subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyMessageWaitingChanged(sender.getMessageWaitingIndicator());
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyCallForwardingChanged(Phone sender) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifyCallForwardingChanged(
                    sender.getCallForwardingIndicator(),
                    subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyCallForwardingChanged(
                        sender.getCallForwardingIndicator());
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyDataActivity(Phone sender) {
        try {
            mMSimRegistry.notifyDataActivity(convertDataActivityState(
                    sender.getDataActivityState()));
            mRegistry.notifyDataActivity(convertDataActivityState(sender.getDataActivityState()));
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyDataConnection(Phone sender, String reason, String apnType,
            PhoneConstants.DataState state) {
        doNotifyDataConnection(sender, reason, apnType, state);
    }

    protected void doNotifyDataConnection(Phone sender, String reason, String apnType,
            PhoneConstants.DataState state) {
        int subscription = sender.getSubscription();
        int dds = MSimPhoneFactory.getDataSubscription();
        log("subscription = " + subscription + ", DDS = " + dds);

        // TODO
        // use apnType as the key to which connection we're talking about.
        // pass apnType back up to fetch particular for this one.
        MSimTelephonyManager telephony = MSimTelephonyManager.getDefault();
        LinkProperties linkProperties = null;
        LinkCapabilities linkCapabilities = null;
        boolean roaming = false;

        if (state == PhoneConstants.DataState.CONNECTED) {
            linkProperties = sender.getLinkProperties(apnType);
            linkCapabilities = sender.getLinkCapabilities(apnType);
        }
        ServiceState ss = sender.getServiceState();
        if (ss != null) roaming = ss.getRoaming();

        try {
            mRegistry.notifyDataConnection(
                    convertDataState(state),
                    sender.isDataConnectivityPossible(apnType), reason,
                    sender.getActiveApnHost(apnType),
                    apnType,
                    linkProperties,
                    linkCapabilities,
                    ((telephony!=null) ? telephony.getDataNetworkType(subscription) :
                    TelephonyManager.NETWORK_TYPE_UNKNOWN),
                    roaming);
            mMSimRegistry.notifyDataConnection(
                    convertDataState(state),
                    sender.isDataConnectivityPossible(apnType), reason,
                    sender.getActiveApnHost(apnType),
                    apnType,
                    linkProperties,
                    linkCapabilities,
                    ((telephony!=null) ? telephony.getDataNetworkType(subscription) :
                    TelephonyManager.NETWORK_TYPE_UNKNOWN),
                    roaming,
                    subscription);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifyDataConnectionFailed(reason, apnType, subscription);
            mRegistry.notifyDataConnectionFailed(reason, apnType);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyCellLocation(Phone sender) {
        int subscription = sender.getSubscription();
        Bundle data = new Bundle();
        sender.getCellLocation().fillInNotifierBundle(data);
        try {
            mMSimRegistry.notifyCellLocation(data, subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyCellLocation(data);
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    @Override
    public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifyCellInfo(cellInfo, subscription);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyCellInfo(cellInfo);
            }
        } catch (RemoteException ex) {

        }
    }

    @Override
    public void notifyOtaspChanged(Phone sender, int otaspMode) {
        int subscription = sender.getSubscription();
        try {
            mMSimRegistry.notifyOtaspChanged(otaspMode);
            if (MSimPhoneFactory.getDefaultSubscription() == subscription) {
                mRegistry.notifyOtaspChanged(otaspMode);
            }
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[MSimDefaultPhoneNotifier] " + s);
    }
}
