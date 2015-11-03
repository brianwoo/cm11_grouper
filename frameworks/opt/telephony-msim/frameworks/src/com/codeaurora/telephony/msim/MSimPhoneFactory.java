/*
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.SystemProperties;
import android.telephony.MSimTelephonyManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.content.Intent;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.PhoneConstants;
import com.codeaurora.telephony.msim.MSimTelephonyIntents;

import java.lang.reflect.Constructor;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_DEFAULT_SUBSCRIPTION;

/**
 * {@hide}
 */
public class MSimPhoneFactory extends PhoneFactory {
    //***** Class Variables
    static final String LOG_TAG = "PHONE";
    static private Phone[] sProxyPhones = null;
    static private CommandsInterface[] sCommandsInterfaces = null;
    static private boolean sMadeMultiSimDefaults = false;

    static private MSimProxyManager mMSimProxyManager;
    static private CardSubscriptionManager mCardSubscriptionManager;
    static private SubscriptionManager mSubscriptionManager;
    static private MSimUiccController mUiccController;

    static private DefaultPhoneProxy sDefaultPhoneProxy = null;

    //***** Class Methods

    public static void makeMultiSimDefaultPhones(Context context) {
        makeMultiSimDefaultPhone(context);
    }

    public static void makeMultiSimDefaultPhone(Context context) {
        synchronized(Phone.class) {
            if (!sMadeMultiSimDefaults) {
                sLooper = Looper.myLooper();
                sContext = context;

                if (sLooper == null) {
                    throw new RuntimeException(
                        "MSimPhoneFactory.makeDefaultPhone must be called from Looper thread");
                }

                int retryCount = 0;
                for(;;) {
                    boolean hasException = false;
                    retryCount ++;

                    try {
                        // use UNIX domain socket to
                        // prevent subsequent initialization
                        new LocalServerSocket("com.android.internal.telephony");
                    } catch (java.io.IOException ex) {
                        hasException = true;
                    }

                    if ( !hasException ) {
                        break;
                    } else if (retryCount > SOCKET_OPEN_MAX_RETRY) {
                        throw new RuntimeException("MSimPhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }
                    }
                }

                sPhoneNotifier = new MSimDefaultPhoneNotifier();

                // Get preferred network mode
                int preferredNetworkMode = RILConstants.PREFERRED_NETWORK_MODE;
                if (TelephonyManager.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    preferredNetworkMode = Phone.NT_MODE_GLOBAL;
                }

               int cdmaSubscription = CdmaSubscriptionSourceManager.getDefault(context);

                Rlog.i(LOG_TAG, "Cdma Subscription set to " + cdmaSubscription);

                /* In case of multi SIM mode two instances of PhoneProxy, RIL are created,
                   where as in single SIM mode only instance. isMultiSimEnabled() function checks
                   whether it is single SIM or multi SIM mode */
                int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
                int[] networkModes = new int[numPhones];
                sProxyPhones = new MSimPhoneProxy[numPhones];
                sCommandsInterfaces = new RIL[numPhones];

                // Get RIL class name
                String sRILClassname = SystemProperties.get("ro.telephony.ril_class", "RIL").trim();
                Rlog.i(LOG_TAG, "RILClassname is " + sRILClassname);

                for (int i = 0; i < numPhones; i++) {
                    //reads the system properties and makes commandsinterface
                    try {
                        networkModes[i]  = MSimTelephonyManager.getIntAtIndex(
                                context.getContentResolver(),
                                Settings.Global.PREFERRED_NETWORK_MODE, i);
                    } catch (SettingNotFoundException snfe) {
                        Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for"+
                                " Settings.Global.PREFERRED_NETWORK_MODE");
                        networkModes[i] = preferredNetworkMode;
                    }

                    if (SystemProperties.getBoolean("persist.env.phone.global", false) &&
                            i == MSimConstants.SUB1) {
                        networkModes[i] = Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA;
                        MSimTelephonyManager.putIntAtIndex( context.getContentResolver(),
                            Settings.Global.PREFERRED_NETWORK_MODE, i, networkModes[i]);
                    }

                    Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                    // Use reflection to construct the RIL class (defaults to RIL)
                    try {
                        sCommandsInterfaces[i] = instantiateCustomRIL(
                                sRILClassname, context, networkModes[i], cdmaSubscription, i);
                    } catch (Exception e) {
                        // 6 different types of exceptions are thrown here that it's
                        // easier to just catch Exception as our "error handling" is the same.
                        // Yes, we're blocking the whole thing and making the radio unusable. That's by design.
                        // The log message should make it clear why the radio is broken
                        while (true) {
                            Rlog.e(LOG_TAG, "Unable to construct custom RIL class", e);
                            try {Thread.sleep(10000);} catch (InterruptedException ie) {}
                        }
                    }
                }

                // Instantiate MSimUiccController so that all other classes can just
                // call getInstance()
                mUiccController = MSimUiccController.make(context, sCommandsInterfaces);

                mCardSubscriptionManager = CardSubscriptionManager.getInstance(context,
                        mUiccController, sCommandsInterfaces);
                mSubscriptionManager = SubscriptionManager.getInstance(context,
                        mUiccController, sCommandsInterfaces);

                for (int i = 0; i < numPhones; i++) {
                    PhoneBase phone = null;
                    int phoneType = TelephonyManager.getPhoneType(networkModes[i]);
                    if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        phone = new MSimGSMPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i);
                    } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        phone = new MSimCDMALTEPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i);
                    }
                    Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " sub = " + i);

                    if (sDefaultPhoneProxy == null) {
                        sDefaultPhoneProxy = new DefaultPhoneProxy(phone);
                    }

                    sProxyPhones[i] = new MSimPhoneProxy(phone);
                }
                mMSimProxyManager = MSimProxyManager.getInstance(context, sProxyPhones,
                        mUiccController, sCommandsInterfaces);

                sMadeMultiSimDefaults = true;

                // Set the default phone in base class
                sProxyPhone = sProxyPhones[MSimConstants.DEFAULT_SUBSCRIPTION];
                sCommandsInterface = sCommandsInterfaces[MSimConstants.DEFAULT_SUBSCRIPTION];
                sMadeDefaults = true;

                sDefaultPhoneProxy.updateDefaultPhoneInSubInfo(sProxyPhone);
                sDefaultPhoneProxy.updateDefaultSMSIntfManager(getSMSSubscription());

                updatePhoneSubInfo();
                // Ensure that we have a default SMS app. Requesting the app with
                // updateIfNeeded set to true is enough to configure a default SMS app.
                ComponentName componentName =
                        SmsApplication.getDefaultSmsApplication(context, true /* updateIfNeeded */);
                String packageName = "NONE";
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                }
                Rlog.i(LOG_TAG, "defaultSmsApplication: " + packageName);

                // Set up monitor to watch for changes to SMS packages
                SmsApplication.initSmsPackageMonitor(context);



            }
        }
    }

    private static <T> T instantiateCustomRIL(String sRILClassname, Context context,
            int networkMode, int cdmaSubscription, Integer instanceId) throws Exception {
        Class<?> clazz = Class.forName("com.android.internal.telephony." + sRILClassname);
        Constructor<?> constructor = clazz.getConstructor(
                Context.class, int.class, int.class,Integer.class);
        return (T) clazz.cast(constructor.newInstance(
                context, networkMode, cdmaSubscription, instanceId));
    }

    public static Phone getMSimCdmaPhone(int subscription) {
        Phone phone;
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            phone = new MSimCDMALTEPhone(sContext, sCommandsInterfaces[subscription],
                    sPhoneNotifier, subscription);
        }
        return phone;
    }

    public static Phone getMSimGsmPhone(int subscription) {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            Phone phone = new MSimGSMPhone(sContext, sCommandsInterfaces[subscription],
                    sPhoneNotifier, subscription);
            return phone;
        }
    }

    public static Phone getPhone(int subscription) {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "MSimPhoneFactory.getPhone must be called from Looper thread");
        }
        if (!sMadeMultiSimDefaults) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            } else if (subscription == MSimConstants.DEFAULT_SUBSCRIPTION) {
                return sProxyPhone;
            }
        }
        return sProxyPhones[subscription];
    }

    public static void updatePhoneSubInfo() {
        int defaultSubId = getDefaultSubscription();
        if (defaultSubId < 0 && defaultSubId >= sProxyPhones.length) {
            defaultSubId = 0;
        }
        sDefaultPhoneProxy.updatePhoneSubInfo(sProxyPhones[defaultSubId].getPhoneSubInfo());
    }

    // TODO: move the get/set subscription APIs to SubscriptionManager

    /* Sets the default subscription. If only one phone instance is active that
     * subscription is set as default subscription. If both phone instances
     * are active the first instance "0" is set as default subscription
     */
    public static void setDefaultSubscription(int subscription) {
        SystemProperties.set(PROPERTY_DEFAULT_SUBSCRIPTION, Integer.toString(subscription));

        // Set the default phone in base class
        if (subscription >= 0 && subscription < sProxyPhones.length) {
            sProxyPhone = sProxyPhones[subscription];
            sCommandsInterface = sCommandsInterfaces[subscription];
            sMadeDefaults = true;
        }

        // Update the subinfo corresponds to the current defaut phone
        sDefaultPhoneProxy.updatePhoneSubInfo(sProxyPhone.getPhoneSubInfo());

        // Update MCC MNC device configuration information
        String defaultMccMnc = MSimTelephonyManager.getDefault().getSimOperator(subscription);
        MccTable.updateMccMncConfiguration(sContext, defaultMccMnc, false);

        // Broadcast an Intent for default sub change
        Intent intent = new Intent(MSimTelephonyIntents.ACTION_DEFAULT_SUBSCRIPTION_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subscription);
        Rlog.d(LOG_TAG, "setDefaultSubscription : " + subscription
                + " Broadcasting Default Subscription Changed...");
        sContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* Gets the default subscription */
    public static int getDefaultSubscription() {
        return SystemProperties.getInt(PROPERTY_DEFAULT_SUBSCRIPTION, 0);
    }

    /* Gets User preferred Voice subscription setting*/
    public static int getVoiceSubscription() {
        int subscription = 0;

        try {
            subscription = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_VOICE_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Call Values");
        }

        // Set subscription to 0 if current subscription is invalid.
        // Ex: multisim.config property is TSTS and subscription is 2.
        // If user is trying to set multisim.config to DSDS and reboots
        // in this case index 2 is invalid so need to set to 0.
        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Rlog.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setVoiceSubscription(subscription);
        }

        return subscription;
    }

    /* Returns User Prompt property,  enabed or not */
    public static boolean isPromptEnabled() {
        boolean prompt = false;
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_VOICE_PROMPT);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Prompt Values");
        }
        prompt = (value == 0) ? false : true ;
        Rlog.d(LOG_TAG, "Prompt option:" + prompt);

       return prompt;
    }

    /*Sets User Prompt property,  enabed or not */
    public static void setPromptEnabled(boolean enabled) {
        int value = (enabled == false) ? 0 : 1;
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_VOICE_PROMPT, value);
        Rlog.d(LOG_TAG, "setVoicePromptOption to " + enabled);
    }

    /* Returns User SMS Prompt property,  enabled or not */
    public static boolean isSMSPromptEnabled() {
        boolean prompt = false;
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_SMS_PROMPT);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        prompt = (value == 0) ? false : true ;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + prompt);

       return prompt;
    }

    /*Sets User SMS Prompt property,  enable or not */
    public static void setSMSPromptEnabled(boolean enabled) {
        int value = (enabled == false) ? 0 : 1;
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_SMS_PROMPT, value);
        Rlog.d(LOG_TAG, "setSMSPromptOption to " + enabled);
    }

    /* Gets current Data subscription setting*/
    public static int getDataSubscription() {
        int subscription = 0;

        try {
            subscription = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Data Call Values");
        }

        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Rlog.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setDataSubscription(subscription);
        }

        return subscription;
    }

    /* Gets default/user preferred Data subscription setting*/
    public static int getDefaultDataSubscription() {
        int subscription = 0;
        try {
            subscription = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_DEFAULT_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Multi Sim Default Data Call Values");
        }
        return subscription;
    }


    /* Gets User preferred SMS subscription setting*/
    public static int getSMSSubscription() {
        int subscription = 0;
        try {
            subscription = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_SMS_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Values");
        }

        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Rlog.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setSMSSubscription(subscription);
        }

        return subscription;
    }

    /* Gets User preferred Priority subscription setting*/
    public static int getPrioritySubscription() {
        int subscription = 0;
        try {
            subscription = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_PRIORITY_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Priority Subscription Values");
        }

        return subscription;
    }

    static public void setVoiceSubscription(int subscription) {
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_VOICE_CALL_SUBSCRIPTION, subscription);
        Rlog.d(LOG_TAG, "setVoiceSubscription : " + subscription);
    }

    static public void setDataSubscription(int subscription) {
        boolean enabled;

        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION, subscription);
        Rlog.d(LOG_TAG, "setDataSubscription: " + subscription);

        // Update the current mobile data flag
        enabled = Settings.Global.getInt(sContext.getContentResolver(),
                Settings.Global.MOBILE_DATA + subscription, 0) != 0;
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MOBILE_DATA, enabled ? 1 : 0);
        Rlog.d(LOG_TAG, "set mobile_data: " + enabled);

        // Update the current data roaming flag
        enabled = Settings.Global.getInt(sContext.getContentResolver(),
                Settings.Global.DATA_ROAMING + subscription, 0) != 0;
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.DATA_ROAMING, enabled ? 1 : 0);
        Rlog.d(LOG_TAG, "set data_roaming: " + enabled);
    }

    static public void setDefaultDataSubscription(int subscription) {
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_DEFAULT_DATA_CALL_SUBSCRIPTION, subscription);
        Rlog.d(LOG_TAG, "setUserPrefDataSubscription: " + subscription);
    }

    static public void setSMSSubscription(int subscription) {
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_SMS_SUBSCRIPTION, subscription);

        Intent intent = new Intent("com.android.mms.transaction.SEND_MESSAGE");
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subscription);
        sContext.sendBroadcast(intent);

        // Change occured in SMS preferred sub, update the default
        // SMS interface Manager object with the new SMS preferred subscription.
        sDefaultPhoneProxy.updateDefaultSMSIntfManager(subscription);
        Rlog.d(LOG_TAG, "setSMSSubscription : " + subscription);
    }

    static public void setPrioritySubscription(int subscription) {
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.MULTI_SIM_PRIORITY_SUBSCRIPTION, subscription);
        Rlog.d(LOG_TAG, "setPrioritySubscription: " + subscription);
    }

    static public void setTuneAway(boolean tuneAway) {
        Settings.Global.putInt(sContext.getContentResolver(),
                Settings.Global.TUNE_AWAY_STATUS, tuneAway ? 1 : 0);
        Rlog.d(LOG_TAG, "setTuneAway: " + tuneAway);
    }

}
