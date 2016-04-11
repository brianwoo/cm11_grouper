/*
 * Copyright (C) 2011, The Android Open Source Project
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
/*
 * Contributed by: Giesecke & Devrient GmbH.
 */

package org.simalliance.openmobileapi.service;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemProperties;

import android.net.Uri;

import com.android.internal.telephony.TelephonyProperties;

import android.nfc.INfcAdapterExtras;
import android.nfc.NfcAdapter;
import com.android.qcom.nfc_extras.*;

import org.simalliance.openmobileapi.service.Channel;
import org.simalliance.openmobileapi.service.Channel.SmartcardServiceChannel;
import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.Terminal.SmartcardServiceReader;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;



import org.simalliance.openmobileapi.service.security.AccessControlEnforcer;
import org.simalliance.openmobileapi.service.security.ChannelAccess;


/**
 * The smartcard service is setup with privileges to access smart card hardware.
 * The service enforces the permission
 * 'org.simalliance.openmobileapi.service.permission.BIND'.
 */
public final class SmartcardService extends Service {

    public static final String _TAG = "SmartcardService";
    public static final String _UICC_TERMINAL = "SIM";
    public static final String _eSE_TERMINAL = "eSE";
    public static final String _SD_TERMINAL = "SD";

    //Open Mobile API Service package
    private static final String OPENMOBILE_API_PACKAGE =
            "org.simalliance.openmobileapi.service";

    //RF actions
    private static final String ACTION_RF_FIELD_ON_DETECTED =
            "com.android.nfc_extras.action.RF_FIELD_ON_DETECTED";
    private static final String ACTION_RF_FIELD_OFF_DETECTED =
            "com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED";
    private static final String ACTION_CHECK_CERT =
            "org.simalliance.openmobileapi.service.ACTION_CHECK_CERT";
    private static final String ACTION_AID_SELECTED =
            "com.android.nfc_extras.action.AID_SELECTED";

    //State changes
    private static final String ACTION_SIM_STATE_CHANGED =
            "android.intent.action.SIM_STATE_CHANGED";
    private static final String ACTION_ADAPTER_STATE_CHANGED =
            "android.nfc.action.ADAPTER_STATE_CHANGED";
    private static final String ACTION_MEDIA_MOUNTED =
            "android.intent.action.MEDIA_MOUNTED";

    public static String _UICC_TERMINAL_EXT[] = new String[] {"1", "2"};
    public static String _eSE_TERMINAL_EXT[] = new String[] {"1", "2"};
    public static String _SD_TERMINAL_EXT[] = new String[] {"1", "2"};

    public static boolean mIsMultiSimEnabled;

    static void clearError(SmartcardError error) {
        if (error != null) {
            error.clear();
        }
    }

    @SuppressWarnings({ "rawtypes" })
    static void setError(SmartcardError error, Class clazz, String message) {
        if (error != null) {
            error.setError(clazz, message);
        }
    }

    static void setError(SmartcardError error, Exception e) {
        if (error != null) {
            error.setError(e.getClass(), e.getMessage());
        }
    }

    /**
     * For now this list is setup in onCreate(), not changed later and therefore
     * not synchronized.
     */
    private Map<String, ITerminal> mTerminals = new TreeMap<String, ITerminal>();

    /**
     * For now this list is setup in onCreate(), not changed later and therefore
     * not synchronized.
     */
    private Map<String, ITerminal> mAddOnTerminals = new TreeMap<String, ITerminal>();

    /* Broadcast receivers */
    private BroadcastReceiver mSimReceiver;
    private BroadcastReceiver mNfcReceiver;
    private BroadcastReceiver mNfcEventReceiver;
    private BroadcastReceiver mPackageUpdateReceiver;
    private BroadcastReceiver mMediaReceiver;

    /* Async task */
    InitialiseTask mInitialiseTask;

    /**
     * ServiceHandler use to load rules from the terminal
     */
    private ServiceHandler mServiceHandler;

    List<PackageInfo> mInstalledPackages; // cached version of installed packages

    void updatePackageCache() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0, UserHandle.USER_OWNER);
        synchronized (this) {
            mInstalledPackages = packages;
        }
    }

    public SmartcardService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(_TAG, Thread.currentThread().getName()
                        + " smartcard service onBind");
        if (ISmartcardService.class.getName().equals(intent.getAction())) {
            return mSmartcardBinder;
        }
        return null;
    }

    @Override
    public void onCreate() {
        Log.v(_TAG, Thread.currentThread().getName()
                + " smartcard service onCreate");

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("SmartCardServiceHandler");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());

        String multiSimConfig = SystemProperties.get(TelephonyProperties.PROPERTY_MULTI_SIM_CONFIG);
        Log.v(_TAG, "multiSimConfig = " + multiSimConfig);

        mIsMultiSimEnabled = (multiSimConfig.equals("dsds") ||
                              multiSimConfig.equals("dsda") ||
                              multiSimConfig.equals("tsts"));

        updatePackageCache();

        createTerminals();
        mInitialiseTask = new InitialiseTask();
        mInitialiseTask.execute();
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("SMARTCARD SERVICE (dumpsys activity service org.simalliance.openmobileapi)");
        writer.println();

        String prefix = "  ";

        if(!Build.IS_DEBUGGABLE) {
            writer.println(prefix + "Your build is not debuggable!");
            writer.println(prefix + "Smartcard service dump is only available for userdebug and eng build");
        } else {
            writer.println(prefix + "List of terminals:");
            for (ITerminal terminal : mTerminals.values()) {
               writer.println(prefix + "  " + terminal.getName());
            }
            writer.println();

            writer.println(prefix + "List of add-on terminals:");
            for (ITerminal terminal : mAddOnTerminals.values()) {
               writer.println(prefix + "  " + terminal.getName());
            }
            writer.println();

            for (ITerminal terminal : mTerminals.values()) {
               terminal.dump(writer, prefix);
            }
            for (ITerminal terminal : mAddOnTerminals.values()) {
               terminal.dump(writer, prefix);
            }
        }
    }


    private class InitialiseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                initializeAccessControl(null, null);
            } catch( Exception e ){
                // do nothing since this is called were nobody can react.
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.i(_TAG, "OnPostExecute()");
            registerSimStateChangedEvent(getApplicationContext()) ;
            registerAdapterStateChangedEvent(getApplicationContext());
            registerNfcEvent(getApplicationContext());
            registerPackageUpdateEvent(getApplicationContext());
            registerMediaMountedEvent(getApplicationContext());
            mInitialiseTask = null;
        }
    }

    private void registerSimStateChangedEvent(Context context) {
        Log.v(_TAG, "register SIM_STATE_CHANGED event");

        IntentFilter intentFilter = new IntentFilter(ACTION_SIM_STATE_CHANGED);
        mSimReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                    final Bundle  extras    = intent.getExtras();
                    final boolean simReady  = (extras != null) && "READY".equals(extras.getString("ss"));
                    final boolean simLoaded = (extras != null) && "LOADED".equals(extras.getString("ss"));
                    if( simReady ){
                        Log.i(_TAG, "SIM is ready. Checking access rules for updates.");
                    }
                    else if( simLoaded){
                        Log.i(_TAG, "SIM is loaded. Checking access rules for updates.");
                    }
                    if (simReady || simLoaded) {
                        mServiceHandler.sendMessage(MSG_LOAD_UICC_RULES, 0); // without retry
                        unregisterSimStateChangedEvent(getApplicationContext()) ;
                    }
                }
            }
        };

        context.registerReceiver(mSimReceiver, intentFilter);
    }
    private void unregisterSimStateChangedEvent(Context context) {
        if(mSimReceiver!= null) {
            Log.v(_TAG, "unregister SIM_STATE_CHANGED event");
            context.unregisterReceiver(mSimReceiver);
            mSimReceiver = null;
        }
    }


    private void registerAdapterStateChangedEvent(Context context) {
        Log.v(_TAG, "register ADAPTER_STATE_CHANGED event");

        IntentFilter intentFilter = new IntentFilter(ACTION_ADAPTER_STATE_CHANGED);
        mNfcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final boolean nfcAdapterAction = ACTION_ADAPTER_STATE_CHANGED.equals(intent.getAction());
                final boolean nfcAdapterOn = nfcAdapterAction && intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1) == 3; // is NFC Adapter turned on ?
                if( nfcAdapterOn){
                    Log.i(_TAG, "NFC Adapter is ON. Checking access rules for updates.");
                    mServiceHandler.sendMessage(MSG_LOAD_ESE_RULES, 5);
                }
            }
        };
        context.registerReceiver(mNfcReceiver, intentFilter);
    }

    private void unregisterAdapterStateChangedEvent(Context context) {
        if(mNfcReceiver!= null) {
            Log.v(_TAG, "unregister ADAPTER_STATE_CHANGED event");
            context.unregisterReceiver(mNfcReceiver);
            mNfcReceiver = null;
        }
     }

    private void registerPackageUpdateEvent(Context context) {
        Log.v(_TAG, "register Package Update event");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        intentFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        mPackageUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_PACKAGE_REMOVED) ||
                    action.equals(Intent.ACTION_PACKAGE_ADDED) ||
                    action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE) ||
                    action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)) {
                    updatePackageCache();
                }
            };
        };
        context.registerReceiver(mPackageUpdateReceiver, intentFilter);
    }

    private void unregisterPackageUpdateEvent(Context context) {
        if(mPackageUpdateReceiver!= null) {
            Log.v(_TAG, "unregister Package Update event");
            context.unregisterReceiver(mPackageUpdateReceiver);
            mPackageUpdateReceiver = null;
        }
    }

    private void registerNfcEvent(Context context) {
        Log.v(_TAG, "register NFC event");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RF_FIELD_ON_DETECTED);
        intentFilter.addAction(ACTION_RF_FIELD_OFF_DETECTED);
        intentFilter.addAction(ACTION_AID_SELECTED);
        intentFilter.addAction(ACTION_CHECK_CERT);

        mNfcEventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean nfcAdapterExtraActionRfFieldOn = false;
                boolean nfcAdapterExtraActionRfFieldOff = false;
                boolean nfcAdapterExtraActionAidSelected = false;
                byte[] aid = null;
                byte[] data = null;
                String seName = null;

                String action = intent.getAction();

                if (ACTION_RF_FIELD_ON_DETECTED.equals(action)){
                    nfcAdapterExtraActionRfFieldOn = true;
                    aid = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00 };
                    Log.i(_TAG, "got RF_FIELD_ON_DETECTED");
                }
                else if (ACTION_RF_FIELD_OFF_DETECTED.equals(action)){
                    nfcAdapterExtraActionRfFieldOff = true;
                    aid = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00 };
                    Log.i(_TAG, "got RF_FIELD_OFF_DETECTED");
                }
                else if (ACTION_CHECK_CERT.equals(action)){
                    Log.i(_TAG, "got ACTION_CHECK_CERT");
                    seName = intent.getStringExtra("org.simalliance.openmobileapi.service.EXTRA_SE_NAME");
                    String pkg = intent.getStringExtra("org.simalliance.openmobileapi.service.EXTRA_PKG");
                    Log.i(_TAG, "SE_NAME : " + seName + ", PKG : " + pkg);

                    NfcQcomAdapter nfcQcomAdapter = NfcQcomAdapter.getNfcQcomAdapter(context);
                    if (nfcQcomAdapter == null) {
                        Log.i(_TAG, "Couldn't get NfcQcomAdapter");
                        return;
                    }

                    SmartcardError error = new SmartcardError();
                    ITerminal terminal = getTerminal(seName, error);
                    if (terminal == null) {
                        Log.i(_TAG, "Couldn't get terminal for " + seName);
                        return;
                    }

                    AccessControlEnforcer acEnforcer;
                    acEnforcer = terminal.getAccessControlEnforcer();
                    if( acEnforcer == null ) {
                        Log.i(_TAG, "Couldn't get AccessControlEnforcer for " + seName);
                        nfcQcomAdapter.notifyCheckCertResult(false);
                        return;
                    }

                    Log.i(_TAG, "Checking access rules for " + seName);

                    try {
                        if (acEnforcer.hasCertificate(pkg)) {
                            nfcQcomAdapter.notifyCheckCertResult(true);
                        } else {
                            nfcQcomAdapter.notifyCheckCertResult(false);
                        }
                    } catch (Exception e) {
                        nfcQcomAdapter.notifyCheckCertResult(false);
                    }
                    return;
                }
                else if (ACTION_AID_SELECTED.equals(action)){
                    nfcAdapterExtraActionAidSelected = true;
                    aid = intent.getByteArrayExtra("com.android.nfc_extras.extra.AID");
                    data = intent.getByteArrayExtra("com.android.nfc_extras.extra.DATA");
                    seName = intent.getStringExtra("com.android.nfc_extras.extra.SE_NAME");

                    if ((aid == null)||(seName == null)) {
                        Log.i(_TAG, "got AID_SELECTED AID without AID or SE Name");
                        return;
                    }
                }
                else {
                    Log.i(_TAG, "mNfcEventReceiver got unexpected intent:" + intent.getAction());
                    return;
                }

                try
                {
                    NfcQcomAdapter nfcQcomAdapter = NfcQcomAdapter.getNfcQcomAdapter(context);
                    if (nfcQcomAdapter == null) {
                        Log.i(_TAG, "Couldn't get NfcQcomAdapter");
                        return;
                    }

                    String [] packageNames = new String[mInstalledPackages.size()];
                    {
                        int i = 0;
                        synchronized(this) {
                            for (PackageInfo pkg : mInstalledPackages) {
                                if (pkg != null && pkg.applicationInfo != null) {
                                    packageNames[i++] = new String(pkg.packageName);
                                }
                            }
                        }
                    }

                    boolean [] nfcEventAccessFinal = null;

                    if (nfcAdapterExtraActionRfFieldOn || nfcAdapterExtraActionRfFieldOff) {
                        SmartcardError error = new SmartcardError();
                        String readers[] = updateTerminals();
                        AccessControlEnforcer acEnforcer;
                        for (int i = 0; i < readers.length; i++){
                            acEnforcer = getTerminal(readers[i], error).getAccessControlEnforcer();
                            if( acEnforcer == null ) {
                                Log.i(_TAG, "Couldn't get AccessControlEnforcer for " + readers[i]);
                                continue;
                            }

                            Log.i(_TAG, "Checking access rules for RF Field On/Off for " + readers[i]);

                            acEnforcer.setPackageManager(getPackageManager());
                            boolean [] nfcEventAccess = acEnforcer.isNFCEventAllowed(aid, packageNames,
                                                                                     (ISmartcardServiceCallback)null );
                            // RF Field ON/OFF doesn't belong to any SE, so allow access to NFC Event if any SE allows
                            if (nfcEventAccessFinal == null) {
                                nfcEventAccessFinal = nfcEventAccess;
                            } else {
                                for (int j = 0; j < mInstalledPackages.size(); j++) {
                                    if (nfcEventAccess[j] == true) {
                                        nfcEventAccessFinal[j] = true;
                                    }
                                }
                            }
                        }
                    } else if (nfcAdapterExtraActionAidSelected) {
                        SmartcardError error = new SmartcardError();
                        AccessControlEnforcer acEnforcer;

                        ITerminal terminal = getTerminal(seName, error);
                        if (terminal == null) {
                            Log.i(_TAG, "Couldn't get terminal for " + seName);
                            return;
                        }

                        acEnforcer = terminal.getAccessControlEnforcer();

                        if( acEnforcer == null ) {
                            Log.i(_TAG, "Couldn't get AccessControlEnforcer for " + seName);
                            return;
                        }

                        Log.i(_TAG, "Checking access rules for AID Selected for " + seName);

                        acEnforcer.setPackageManager(getPackageManager());
                        nfcEventAccessFinal = acEnforcer.isNFCEventAllowed(aid, packageNames,
                                                                           (ISmartcardServiceCallback)null );
                    }

                    if (nfcEventAccessFinal != null) {
                        synchronized(this) {
                            for (int i = 0; i < mInstalledPackages.size(); i++) {
                                if (nfcEventAccessFinal[i]) {
                                    if (packageNames[i].equals(OPENMOBILE_API_PACKAGE))
                                        continue;
                                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    intent.setPackage(packageNames[i]);
                                    try {
                                        nfcQcomAdapter.deliverSeIntent(intent);
                                    } catch (Exception ignore) {
                                        //ignore
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.v(_TAG, "NFC Event AC Exception: " + e.getMessage() );
                }
            }
        };
        context.registerReceiver(mNfcEventReceiver, intentFilter);
    }

    private void unregisterNfcEvent(Context context) {
        if(mNfcEventReceiver!= null) {
            Log.v(_TAG, "unregister NFC event");
            context.unregisterReceiver(mNfcEventReceiver);
            mNfcEventReceiver = null;
        }
    }

    private void registerMediaMountedEvent(Context context) {
        Log.v(_TAG, "register MEDIA_MOUNTED event");

        IntentFilter intentFilter = new IntentFilter(ACTION_MEDIA_MOUNTED);
        mMediaReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final boolean mediaMounted = ACTION_MEDIA_MOUNTED.equals(intent.getAction());
                if( mediaMounted){
                    Log.i(_TAG, "New Media is mounted. Checking access rules for updates.");
                     mServiceHandler.sendMessage(MSG_LOAD_SD_RULES, 5);
                 }
            }
        };
        context.registerReceiver(mMediaReceiver, intentFilter);
    }

    private void unregisterMediaMountedEvent(Context context) {
        if(mMediaReceiver != null) {
            Log.v(_TAG, "unregister MEDIA_MOUNTED event");
            context.unregisterReceiver(mMediaReceiver);
            mMediaReceiver = null;
        }
     }

    /**
     * Initalizes Access Control.
     * At least the refresh tag is read and if it differs to the previous one (e.g. is null) the all
     * access rules are read.
     *
     * @param se
     */
    public boolean initializeAccessControl(String se, ISmartcardServiceCallback callback ) {
        return initializeAccessControl(false, se, callback);
    }

    public synchronized boolean initializeAccessControl(boolean reset, String se, ISmartcardServiceCallback callback ) {
        boolean result = true;
        Log.i(_TAG, "Initializing Access Control");

        if( callback == null ) {
            callback = new ISmartcardServiceCallback.Stub(){};
        }

        Collection<ITerminal>col = mTerminals.values();
        Iterator<ITerminal> iter = col.iterator();
        while(iter.hasNext()){
            ITerminal terminal = iter.next();
            if( terminal == null ){

                continue;
            }

                if( se == null || terminal.getName().startsWith(se)) {
                    boolean isCardPresent = false;
                    try {
                        isCardPresent = terminal.isCardPresent();
                    } catch (CardException e) {
                        isCardPresent = false;

                    }

                    if(isCardPresent) {
                        Log.i(_TAG, "Initializing Access Control for " + terminal.getName());
                        if(reset) terminal.resetAccessControl();
                        result &= terminal.initializeAccessControl(true, callback);
                    } else {
                        Log.i(_TAG, "NOT initializing Access Control for " + terminal.getName() + " SE not present.");
                    }
                }
        }
        col = this.mAddOnTerminals.values();
        iter = col.iterator();
        while(iter.hasNext()){
            ITerminal terminal = iter.next();
            if( terminal == null ){

                continue;
            }

            if( se == null || terminal.getName().startsWith(se)) {
                boolean isCardPresent = false;
                try {
                    isCardPresent = terminal.isCardPresent();
                } catch (CardException e) {
                    isCardPresent = false;

                }

                if(isCardPresent) {
                    Log.i(_TAG, "Initializing Access Control for " + terminal.getName());
                    if(reset) terminal.resetAccessControl();
                    result &= terminal.initializeAccessControl(true, callback);
                } else {
                    Log.i(_TAG, "NOT initializing Access Control for " + terminal.getName() + " SE not present.");
                }
            }
        }
        return result;
    }

    public void onDestroy() {
        Log.v(_TAG, " smartcard service onDestroy ...");
        for (ITerminal terminal : mTerminals.values())
            terminal.closeChannels();
        for (ITerminal terminal : mAddOnTerminals.values())
            terminal.closeChannels();

        // Cancel the inialization background task if still running
        if(mInitialiseTask != null) mInitialiseTask.cancel(true);
        mInitialiseTask = null;

        // Unregister all the broadcast receivers
        unregisterSimStateChangedEvent(getApplicationContext()) ;
        unregisterAdapterStateChangedEvent(getApplicationContext());
        unregisterNfcEvent(getApplicationContext());
        unregisterPackageUpdateEvent(getApplicationContext());
        unregisterMediaMountedEvent(getApplicationContext());

        mServiceHandler = null;

        Log.v(_TAG, Thread.currentThread().getName()
                + " ... smartcard service onDestroy");

    }

    private ITerminal getTerminal(String reader, SmartcardError error) {
        if (reader == null) {
            setError(error, NullPointerException.class, "reader must not be null");
            return null;
        }
        ITerminal terminal = mTerminals.get(reader);
        if (terminal == null) {
            terminal = mAddOnTerminals.get(reader);
            if (terminal == null) {
                setError(error, IllegalArgumentException.class, "unknown reader");
            }
        }
        return terminal;
    }

    private String[] createTerminals() {
        createBuildinTerminals();

        Set<String> names = mTerminals.keySet();
        ArrayList<String> list = new ArrayList<String>(names);
        Collections.sort(list);

        // set UICC on the top , SIM1(or SIM - UICC)/SIM2 and then eSE1/eSE2
        if(list.remove(_eSE_TERMINAL + _eSE_TERMINAL_EXT[1]))
            list.add(0, _eSE_TERMINAL + _eSE_TERMINAL_EXT[1]);
        if(list.remove(_eSE_TERMINAL + _eSE_TERMINAL_EXT[0]))
            list.add(0, _eSE_TERMINAL + _eSE_TERMINAL_EXT[0]);
        if(list.remove(_UICC_TERMINAL + _UICC_TERMINAL_EXT[1]))
            list.add(0, _UICC_TERMINAL + _UICC_TERMINAL_EXT[1]);
        if(list.remove(_UICC_TERMINAL + _UICC_TERMINAL_EXT[0]))
            list.add(0, _UICC_TERMINAL + _UICC_TERMINAL_EXT[0]);

        createAddonTerminals();
        names = mAddOnTerminals.keySet();
        for (String name : names) {
            if (!list.contains(name)) {
                list.add(name);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    private String[] updateTerminals() {
        Set<String> names = mTerminals.keySet();
        ArrayList<String> list = new ArrayList<String>(names);
        Collections.sort(list);

        // set UICC on the top , SIM1(or SIM - UICC)/SIM2 and then eSE1/eSE2
        if(list.remove(_eSE_TERMINAL + _eSE_TERMINAL_EXT[1]))
            list.add(0, _eSE_TERMINAL + _eSE_TERMINAL_EXT[1]);
        if(list.remove(_eSE_TERMINAL + _eSE_TERMINAL_EXT[0]))
            list.add(0, _eSE_TERMINAL + _eSE_TERMINAL_EXT[0]);
        if(list.remove(_UICC_TERMINAL + _UICC_TERMINAL_EXT[1]))
            list.add(0, _UICC_TERMINAL + _UICC_TERMINAL_EXT[1]);
        if(list.remove(_UICC_TERMINAL + _UICC_TERMINAL_EXT[0]))
            list.add(0, _UICC_TERMINAL + _UICC_TERMINAL_EXT[0]);

        updateAddonTerminals();
        names = mAddOnTerminals.keySet();
        for (String name : names) {
            if (!list.contains(name)) {
                list.add(name);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createBuildinTerminals() {
        Class[] types = new Class[] {
            Context.class, int.class
        };
        Object[] args = new Object[] {
            this, 0
        };
        Object[] classes = getBuildinTerminalClasses();

        String smartcardConfig = SystemProperties.get("persist.nfc.smartcard.config");
        Log.v(_TAG, "smartcardConfig = " + smartcardConfig);
        String[] terminals = smartcardConfig.split(",");
        int numUiccTerminal = 0;
        int numSmartMxTerminal = 0;
        int numASSDTerminal = 0;
        for (int i = 0; i < terminals.length; i++) {
            if (terminals[i].startsWith("SIM")) {
                if (numUiccTerminal < 2) {
                    _UICC_TERMINAL_EXT[numUiccTerminal] = terminals[i].substring(3);
                    numUiccTerminal++;
                }
            }
            else if (terminals[i].startsWith("eSE")) {
                if (numSmartMxTerminal < 2) {
                    _eSE_TERMINAL_EXT[numSmartMxTerminal] = terminals[i].substring(3);
                    numSmartMxTerminal++;
                }
            }
            else if (terminals[i].startsWith("SD")) {
                if (numASSDTerminal < 2) {
                    _SD_TERMINAL_EXT[numASSDTerminal] = terminals[i].substring(2);
                    numASSDTerminal++;
                }
            }
        }

        if ((!mIsMultiSimEnabled) && (numUiccTerminal > 1))
            numUiccTerminal = 1;

        for (Object clazzO : classes) {
            try {
                Class clazz = (Class) clazzO;
                Constructor constr = clazz.getDeclaredConstructor(types);

                int numSlots;
                if (constr.getName().endsWith("UiccTerminal")) {
                    numSlots = numUiccTerminal;
                } else if (constr.getName().endsWith("SmartMxTerminal")) {
                    numSlots = numSmartMxTerminal;
                } else if (constr.getName().endsWith("ASSDTerminal")) {
                    numSlots = numASSDTerminal;
                } else {
                    numSlots = 1;
                }
                for (int slot = 0; slot < numSlots; slot++) {
                    args[1] = slot;
                    ITerminal terminal = (ITerminal) constr.newInstance(args);
                    mTerminals.put(terminal.getName(), terminal);
                    Log.v(_TAG, Thread.currentThread().getName() + " adding "
                            + terminal.getName());
                }
            } catch (Throwable t) {
                Log.e(_TAG, Thread.currentThread().getName()
                        + " CreateReaders Error: "
                        + ((t.getMessage() != null) ? t.getMessage() : "unknown"));
            }
        }
    }

    private void createAddonTerminals() {
        String[] packageNames = AddonTerminal.getPackageNames(this);
        for (String packageName : packageNames) {
            try {
                String apkName = getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
                DexFile dexFile = new DexFile(apkName);
                Enumeration<String> classFileNames = dexFile.entries();
                while (classFileNames.hasMoreElements()) {
                    String className = classFileNames.nextElement();
                    if (className.endsWith("Terminal")) {
                        ITerminal terminal = new AddonTerminal(this, packageName, className);
                        mAddOnTerminals.put(terminal.getName(), terminal);
                        Log.v(_TAG, Thread.currentThread().getName() + " adding "
                                + terminal.getName());
                    }
                }
            } catch (Throwable t) {
                Log.e(_TAG, Thread.currentThread().getName()
                        + " CreateReaders Error: "
                        + ((t.getMessage() != null) ? t.getMessage() : "unknown"));
            }
        }
    }

    private void updateAddonTerminals() {
        Set<String> names = mAddOnTerminals.keySet();
        ArrayList<String> namesToRemove = new ArrayList<String>();
        for (String name : names) {
            ITerminal terminal = mAddOnTerminals.get(name);
            if (!terminal.isConnected()) {
                namesToRemove.add(terminal.getName());
            }
        }
        for (String name : namesToRemove) {
            mAddOnTerminals.remove(name);
        }

        String[] packageNames = AddonTerminal.getPackageNames(this);
        for (String packageName : packageNames) {
            try {
                String apkName = getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
                DexFile dexFile = new DexFile(apkName);
                Enumeration<String> classFileNames = dexFile.entries();
                while (classFileNames.hasMoreElements()) {
                    String className = classFileNames.nextElement();
                    if (className.endsWith("Terminal")) {
                        ITerminal terminal = new AddonTerminal(this, packageName, className);
                        if (!mAddOnTerminals.containsKey(terminal.getName())) {
                            mAddOnTerminals.put(terminal.getName(), terminal);
                            Log.v(_TAG, Thread.currentThread().getName()
                                    + " adding " + terminal.getName());
                        }
                    }
                }

            } catch (Throwable t) {
                Log.e(_TAG, Thread.currentThread().getName()
                        + " CreateReaders Error: "
                        + ((t.getMessage() != null) ? t.getMessage() : "unknown"));
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object[] getBuildinTerminalClasses() {
        ArrayList classes = new ArrayList();
        try {
            String apkName = getPackageManager().getApplicationInfo(OPENMOBILE_API_PACKAGE, 0).sourceDir;
            DexClassLoader dexClassLoader = new DexClassLoader(apkName, getApplicationContext().getFilesDir().getAbsolutePath(), null, getClass()
                    .getClassLoader());

            Class terminalClass = Class.forName("org.simalliance.openmobileapi.service.Terminal", true, dexClassLoader);
            if (terminalClass == null) {
                return classes.toArray();
            }

            DexFile dexFile = new DexFile(apkName);
            Enumeration<String> classFileNames = dexFile.entries();
            while (classFileNames.hasMoreElements()) {
                String className = classFileNames.nextElement();
                Class clazz = Class.forName(className);
                Class superClass = clazz.getSuperclass();
                if (superClass != null && superClass.equals(terminalClass)
                        && !className.equals("org.simalliance.openmobileapi.service.AddonTerminal")) {
                    classes.add(clazz);
                }
            }
        } catch (Throwable exp) {
            // nothing to to
        }
        return classes.toArray();
    }

    /**
     * Get package name from the user id.
     *
     * This shall fix the problem the issue that process name != package name
     * due to anndroid:process attribute in manifest file.
     *
     * But this call is not really secure either since a uid can be shared between one
     * and more apks
     *
     * @param uid
     * @return The first package name associated with this uid.
     */
    public String getPackageNameFromCallingUid(int uid ){
       PackageManager packageManager = getPackageManager();
       if(packageManager != null){
               String packageName[] = packageManager.getPackagesForUid(uid);
               if( packageName != null && packageName.length > 0 ){
                       return packageName[0];
               }
       }
       throw new AccessControlException("Caller PackageName can not be determined");
    }

    /**
     * The smartcard service interface implementation.
     */
    private final ISmartcardService.Stub mSmartcardBinder = new ISmartcardService.Stub() {

        @Override
        public String[] getReaders(SmartcardError error) throws RemoteException {
            clearError(error);
            Log.v(_TAG, "getReaders()");
            return updateTerminals();
        }

        @Override
        public ISmartcardServiceReader getReader(String reader,
                SmartcardError error) throws RemoteException {
            clearError(error);
            Terminal terminal = (Terminal)getTerminal(reader, error);
            if( terminal != null ){
                return terminal.new SmartcardServiceReader(SmartcardService.this);
            }
            setError(error, IllegalArgumentException.class, "invalid reader name");
            return null;
        }


        @Override
        public synchronized boolean[] isNFCEventAllowed(
                String reader,
                byte[] aid,
                String[] packageNames,
                ISmartcardServiceCallback callback,
                SmartcardError error)
                        throws RemoteException
        {
            clearError(error);
            try
            {
                if (callback == null) {
                    setError(error, NullPointerException.class, "callback must not be null");
                    return null;
                }
                ITerminal terminal = getTerminal(reader, error);
                if (terminal == null) {
                    return null;
                }
                if (aid == null || aid.length == 0) {
                    aid = new byte[] {
                            0x00, 0x00, 0x00, 0x00, 0x00
                    };
                }
                if (aid.length < 5 || aid.length > 16) {
                     setError(error, IllegalArgumentException.class, "AID out of range");
                     return null;
                }
                if (packageNames == null || packageNames.length == 0) {
                     setError(error, IllegalArgumentException.class, "process names not specified");
                     return null;
                }
                AccessControlEnforcer ac = null;
                if( terminal.getAccessControlEnforcer() == null ) {
                    ac = new AccessControlEnforcer( terminal );
                } else {
                    ac = terminal.getAccessControlEnforcer();
                }
                ac.setPackageManager(getPackageManager());
                ac.initialize(true, callback);
                return ac.isNFCEventAllowed(aid, packageNames, callback );
            } catch (Exception e) {
                setError(error, e);
                Log.v(_TAG, "isNFCEventAllowed Exception: " + e.getMessage() );
                return null;
            }
        }
    };

    /**
     * The smartcard service interface implementation.
     */
    final class SmartcardServiceSession extends ISmartcardServiceSession.Stub {

        private final SmartcardServiceReader mReader;
        /** List of open channels in use of by this client. */
        private final Set<Channel> mChannels = new HashSet<Channel>();

        private final Object mLock = new Object();

        private boolean mIsClosed;

        private byte[] mAtr;

        public SmartcardServiceSession(SmartcardServiceReader reader){
            mReader = reader;
            mAtr = mReader.getAtr();
            mIsClosed = false;
        }

        @Override
        public ISmartcardServiceReader getReader() throws RemoteException {
            return mReader;
        }

        @Override
        public byte[] getAtr() throws RemoteException {
            return mAtr;
        }

        @Override
        public void close(SmartcardError error) throws RemoteException {
            clearError(error);
            if (mReader == null) {
                return;
            }
            try {
                mReader.closeSession(this);


            } catch (CardException e) {
                setError(error,e);
            }
        }

        @Override
        public void closeChannels(SmartcardError error) throws RemoteException {
            synchronized (mLock) {

                Iterator<Channel>iter = mChannels.iterator();
                try {
                    while(iter.hasNext()) {
                        Channel channel = iter.next();
                        if (channel != null && !channel.isClosed()) {
                            try {
                                channel.close();
                                // close changes indirectly mChannels, so we need a new iterator.
                                iter = mChannels.iterator();
                            } catch (Exception ignore) {
                                Log.e(_TAG, "ServiceSession channel - close Exception " + ignore.getMessage());
                            }
                            channel.setClosed();
                        }
                    }
                    mChannels.clear();
                } catch( Exception e ) {
                    Log.e(_TAG, "ServiceSession closeChannels Exception " + e.getMessage());
                }
            }
        }

        @Override
        public boolean isClosed() throws RemoteException {

            return mIsClosed;
        }

        @Override
        public ISmartcardServiceChannel openBasicChannel(
                ISmartcardServiceCallback callback, SmartcardError error)
                throws RemoteException {
            return openBasicChannelAid( null, callback, error);
        }

        @Override
        public ISmartcardServiceChannel openBasicChannelAid(byte[] aid,
                ISmartcardServiceCallback callback, SmartcardError error)
                throws RemoteException {
            clearError(error);
            if ( isClosed() ) {
                setError( error, IllegalStateException.class, "session is closed");
                return null;
            }
            if (callback == null) {
                setError(error, NullPointerException.class, "callback must not be null");
                return null;
            }
            if (mReader == null) {
                setError(error, NullPointerException.class, "reader must not be null");
                return null;
            }

            try {
                boolean noAid = false;
                if (aid == null || aid.length == 0) {
                    aid = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00 };
                    noAid = true;
                }

                if (aid.length < 5 || aid.length > 16) {
                    setError(error, IllegalArgumentException.class, "AID out of range");
                    return null;
                }


                String packageName = getPackageNameFromCallingUid( Binder.getCallingUid());
                Log.v(_TAG, "Enable access control on basic channel for " + packageName);
                ChannelAccess channelAccess = mReader.getTerminal().setUpChannelAccess(
                        getPackageManager(),
                        aid,
                        packageName,
                        callback );
                Log.v(_TAG, "Access control successfully enabled.");

                channelAccess.setCallingPid(Binder.getCallingPid());

                if (true) {
                    Log.v(_TAG, "OpenBasicChannel(AID): not allowed");
                    return null;
                }

                Log.v(_TAG, "OpenBasicChannel(AID)");
                Channel channel = null;
                if (noAid) {
                    channel = mReader.getTerminal().openBasicChannel(this, callback);
                } else {
                    channel = mReader.getTerminal().openBasicChannel(this, aid, callback);
                }

                channel.setChannelAccess(channelAccess);

                Log.v(_TAG, "Open basic channel success. Channel: " + channel.getChannelNumber() );

                SmartcardServiceChannel basicChannel = channel.new SmartcardServiceChannel(this);
                mChannels.add(channel);
                return basicChannel;

            } catch (Exception e) {
                setError(error, e);
                Log.v(_TAG, "OpenBasicChannel Exception: " + e.getMessage());
                return null;
            }
        }

        @Override
        public ISmartcardServiceChannel openLogicalChannel(byte[] aid,
                ISmartcardServiceCallback callback, SmartcardError error)
                throws RemoteException {
            clearError(error);

            if ( isClosed() ) {
                setError( error, IllegalStateException.class, "session is closed");
                return null;
            }

            if (callback == null) {
                setError(error, NullPointerException.class, "callback must not be null");
                return null;
            }
            if (mReader == null) {
                setError(error, NullPointerException.class, "reader must not be null");
                return null;
            }

            try {
                boolean noAid = false;
                if (aid == null || aid.length == 0) {
                    aid = new byte[] {
                            0x00, 0x00, 0x00, 0x00, 0x00
                    };
                    noAid = true;
                }

                if (aid.length < 5 || aid.length > 16) {
                    setError(error, IllegalArgumentException.class, "AID out of range");
                    return null;
                }


                String packageName = getPackageNameFromCallingUid( Binder.getCallingUid());
                Log.v(_TAG, "Enable access control on logical channel for " + packageName);
                ChannelAccess channelAccess = mReader.getTerminal().setUpChannelAccess(
                        getPackageManager(),
                        aid,
                        packageName,
                        callback );
                Log.v(_TAG, "Access control successfully enabled.");
               channelAccess.setCallingPid(Binder.getCallingPid());


                Log.v(_TAG, "OpenLogicalChannel");
                Channel channel = null;
                if (noAid) {
                    channel = mReader.getTerminal().openLogicalChannel(this, callback);
                } else {
                    channel = mReader.getTerminal().openLogicalChannel(this, aid, callback);
                }

                channel.setChannelAccess(channelAccess);

                Log.v(_TAG, "Open logical channel successfull. Channel: " + channel.getChannelNumber());
                SmartcardServiceChannel logicalChannel = channel.new SmartcardServiceChannel(this);
                mChannels.add(channel);
                return logicalChannel;
            } catch (Exception e) {
                setError(error, e);
                Log.v(_TAG, "OpenLogicalChannel Exception: " + e.getMessage());
                return null;
            }
        }

        void setClosed(){
            mIsClosed = true;

        }

        /**
         * Closes the specified channel. <br>
         * After calling this method the session can not be used for the
         * communication with the secure element any more.
         *
         * @param hChannel the channel handle obtained by an open channel command.
         */
        void removeChannel(Channel channel) {
            if (channel == null) {
                return;
            }
            mChannels.remove(channel);
        }
    }

    /*
     * Handler Thread used to load and initiate ChannelAccess condition
     */
    public final static int MSG_LOAD_UICC_RULES  = 1;
    public final static int MSG_LOAD_ESE_RULES   = 2;
    public final static int MSG_LOAD_SD_RULES    = 3;

    public final static int NUMBER_OF_TRIALS      = 3;
    public final static long WAIT_TIME            = 1000;

    private final class ServiceHandler extends Handler {

        @SuppressLint("HandlerLeak")
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        public void sendMessage(int what, int nbTries) {
           mServiceHandler.removeMessages(what);
           Message newMsg = mServiceHandler.obtainMessage(what, nbTries, 0);
           mServiceHandler.sendMessage(newMsg);
        }

        @Override
        public void handleMessage(Message msg) {
           boolean result = true;

           Log.i(_TAG, "Handle msg: what=" + msg.what + " nbTries=" + msg.arg1);

           switch(msg.what) {
           case MSG_LOAD_UICC_RULES:
               try {
                   result = initializeAccessControl(true, _UICC_TERMINAL, null );
               } catch (Exception e) {
                   Log.e(_TAG, "Got exception:" + e);
               }
               break;

           case MSG_LOAD_ESE_RULES:
               try {
                   result = initializeAccessControl(true, _eSE_TERMINAL, null );
               } catch (Exception e) {
                   Log.e(_TAG, "Got exception:" + e);
               }
               break;

           case MSG_LOAD_SD_RULES:
               try {
                   result = initializeAccessControl(true, _SD_TERMINAL, null );
               } catch (Exception e) {
                   Log.e(_TAG, "Got exception:" + e);
               }
               break;
           }

           if(!result && msg.arg1 > 0) {
               // Try to re-post the message
               Log.e(_TAG, "Fail to load rules: Let's try another time (" + msg.arg1 + " remaining attempt");
               Message newMsg = mServiceHandler.obtainMessage(msg.what, msg.arg1 - 1, 0);
               mServiceHandler.sendMessageDelayed(newMsg, WAIT_TIME);
           }
        }
    }
}
