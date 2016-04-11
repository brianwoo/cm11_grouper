/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codeaurora.bluetooth.a4wp;

import java.util.UUID;

import android.bluetooth.BluetoothManager;
import android.bluetooth.QBluetoothAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.app.Service;
import android.net.Credentials;
import java.io.OutputStream;
import android.util.Log;
import android.os.IBinder;
import android.content.Intent;
import android.os.Process;
import java.nio.ByteBuffer;
import android.wipower.WipowerManager;
import android.wipower.WipowerManagerCallback;
import android.wipower.WipowerManager.WipowerState;
import android.wipower.WipowerManager.WipowerAlert;
import android.wipower.WipowerManager.PowerApplyEvent;
import android.wipower.WipowerManager.PowerLevel;
import android.wipower.WipowerDynamicParam;

/**
 * Class which executes A4WP service
 */
public class A4wpService extends Service
{
    private static final String LOGTAG = "A4wpService";
    private static OutputStream mOutputStream = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGattServer mBluetoothGattServer = null;
    private BluetoothDevice mDevice = null;
    private static final UUID A4WP_SERVICE_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200cfffe");
    //PRU writes
    private static final UUID A4WP_PRU_CTRL_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a67");
    private static final UUID A4WP_PTU_STATIC_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a68");
    //PRU reads
    private static final UUID A4WP_PRU_ALERT_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a69");
    private static final UUID A4WP_PRU_STATIC_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a70");
    private static final UUID A4WP_PRU_DYNAMIC_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a71");

    private static final UUID A4WP_PRU_ALERT_DESC_UUID = UUID.fromString("6455e670-a146-11e2-9e96-0800200c9a69");
    //CHECK: Using the Alert UUID for now

    private static final Object mLock = new Object();
    private int mState = BluetoothProfile.STATE_DISCONNECTED;

    public final static short DEFAULT_FIELDS = 0x0000;
    public final static short DEFAULT_PROTOCOL_REV = 0x0000;
    public final static short DEFAULT_RFU = 0x0000;
    public final static byte DEFAULT_CATEGORY = 0x0003;
    public final static byte DEFAULT_CAPABILITIES = 0x0010;
    public final static byte DEFAULT_HW_VERSION = 0x0007;
    public final static byte DEFAULT_FW_VERSION = 0x0006;
    public final static byte DEFAULT_MAX_POWER_DESIRED = 0x0032;
    public final static short DEFAULT_VRECT_MIN = 0x003200;
    public final static short DEFAULT_VRECT_MAX = 0x004650;
    public final static short DEFAULT_VRECT_SET = 0x002580;
    public final static short DEFAULT_DELTA_R1 = 0x0001;
    public final static int DEFAULT_RFU_VAL = 0x0000;
    private static final int MSB_MASK = 0xFF00;
    private static final int LSB_MASK= 0x00FF;

    private class PruStaticParam {
        private byte mOptvalidity;
        private byte mProtoRevision;
        private byte  mRfu;
        private byte mCategory;
        private byte mCapabilities;
        private byte mHwRev;
        private byte mFwRev;
        private byte mMaxPowerDesired;
        private short mVrectMinStatic;
        private short mVrectMaxStatic;
        private short mVrectSet;
        private short mDeltaR1;
        private int mRfuVal;

        public PruStaticParam() {
            mOptvalidity = (byte)DEFAULT_FIELDS;
            mProtoRevision = (byte)DEFAULT_PROTOCOL_REV;
            mRfu = (byte)DEFAULT_RFU;
            mCategory = (byte)DEFAULT_CATEGORY;
            mCapabilities = (byte)DEFAULT_CAPABILITIES;
            mHwRev = (byte)DEFAULT_HW_VERSION;
            mFwRev = (byte)DEFAULT_FW_VERSION;
            mMaxPowerDesired = (byte)DEFAULT_MAX_POWER_DESIRED;
            mVrectMinStatic = (short)DEFAULT_VRECT_MIN;
            mVrectMaxStatic = (short)DEFAULT_VRECT_MAX;
            mVrectSet = (short)DEFAULT_VRECT_SET;
            mDeltaR1 = (short)DEFAULT_DELTA_R1;
            mRfuVal = (int)DEFAULT_RFU_VAL;
            Log.v(LOGTAG, "PruStaticParam initialized");
        }

        public byte[] getValue() {
            byte[] res = new byte[20];
            res[0] = mOptvalidity;
            res[1] = mProtoRevision;
            res[2] = mRfu;
            res[3] = mCategory;
            res[4] = mCapabilities;
            res[5] = mHwRev;
            res[6] = mFwRev;
            res[7] = mMaxPowerDesired;
            res[8] =  (byte)(LSB_MASK & mVrectMinStatic);
            res[9] = (byte)((MSB_MASK & mVrectMinStatic) >> 8);
            res[10] =  (byte)(LSB_MASK & mVrectMaxStatic);
            res[11] = (byte)((MSB_MASK & mVrectMaxStatic) >> 8);
            res[12] =  (byte)(LSB_MASK & mVrectSet);
            res[13] = (byte)((MSB_MASK & mVrectSet) >> 8);
            res[14] =  (byte)(LSB_MASK & mDeltaR1);
            res[15] = (byte)((MSB_MASK & mDeltaR1) >> 8);
            res[16] =  (byte)(LSB_MASK & mRfuVal);
            res[17] = (byte)((MSB_MASK & mRfuVal) >> 8);
            res[18] =  (byte)((0xFF0000 & mRfuVal) >> 16);
            res[19] = (byte)((0xFF000000 & mRfuVal) >> 24);

            return res;
        }

        /*This is used to set the charging values*/
        public void setValue(byte[] value) {
            mOptvalidity = value[0];
            mProtoRevision = value[1];
            mRfu = value[2];
            mCategory = value[3] ;
            mCapabilities = value[4];
            mHwRev = value[5];
            mFwRev = value[6];
            mMaxPowerDesired = value[7];
            mVrectMinStatic = value[8];
            mVrectMinStatic |= (short)(value[9] << 8);
            mVrectMinStatic = value[10];
            mVrectMinStatic |= (short)(value[11] << 8);
            mVrectSet = value[12];
            mVrectSet |= (short)(value[13] << 8);
            mDeltaR1 = value[14];
            mDeltaR1 |= (short)(value[15] << 8);
            mRfuVal = value[16];
            mRfuVal |= (int)(value[17] << 8);
            mRfuVal |= (int)(value[18] << 16);
            mRfuVal |= (int)(value[19] << 24);

            return;
        }

    }

    private class PruAlert {
       private byte mAlert;

       public PruAlert(byte value) {
           mAlert = value;
       }

       public void setValue(byte value) {
           mAlert = value;
       }

       public byte[] getValue() {
           byte[] res = new byte[1];
           res[0] = mAlert;
           return res;
       }
    }

    private class PtuStaticParam {
        private byte mOptValidity;
        private byte mPower;
        private byte mMaxSrcImpedence;
        private byte mMaxLoadResistance;
        private short mId;
        private byte mClass;
        private byte mHwRev;
        private byte mFwRev;
        private byte mProtocolRev;
        private byte mMaxDevicesSupported;
        private int mReserved1;
        private short mReserved2;

        public PtuStaticParam(byte[] value) {
            mOptValidity = value[0];
            mPower = value[1];
            mMaxSrcImpedence = value[2];
            mMaxLoadResistance = value[3];
            mId = (short)(value[4] & 0xff);
            mId |= (short)((value[5] & 0xff) << 8);
            mClass = value[6];
            mHwRev = value[7];
            mFwRev = value[8];
            mProtocolRev = value[9];
            mMaxDevicesSupported = value[10];
            mReserved1 = (int)(value[11] & 0xff);
            mReserved1 |= (int)((value[12] & 0xff) << 8);
            mReserved1 |= (int)((value[13] & 0xff) << 16);
            mReserved1 |= (int)((value[14] & 0xff) << 16);
            mReserved2 = (short)(value[15] & 0xff);
            mReserved2 |= (short)((value[16] & 0xff) << 8);
        }

        public void print() {
            Log.v(LOGTAG, "mOptValidity" +  toHex(mOptValidity) +  "mPower" +  toHex(mPower) + "mMaxSrcImpedence" +  toHex(mMaxSrcImpedence) + "mMaxLoadResistance" +  toHex(mMaxLoadResistance));
            Log.v(LOGTAG, "mId" +  toHex(mId) + "mClass" +  toHex(mClass) + "mHwRev" +  toHex(mHwRev) +  "mFwRev" +  toHex(mFwRev));
            Log.v(LOGTAG, "mProtocolRev" +  toHex(mProtocolRev) + "mMaxDevicesSupported" +  toHex(mMaxDevicesSupported) + "mReserved1" +  toHex(mReserved1) + "mReserved2" +  toHex(mReserved2));
        }

        public double getPower() {
            double val = ((mPower&0xfc)>>2);
            val = 0.5*(val+1);
            Log.v(LOGTAG, "getPower<=" + val);
            if (val > 22) val = 22.0;
            return val;
        }

        public double getMaxSrcImpedence() {
            double val = ((mMaxSrcImpedence&0xf8)>>3);
            val = 50 + (val*10);
            Log.v(LOGTAG, "getSrcImpedence<=" + val);
            if (val > 375) val = 375.0;
            return val;
        }

        public double getMaxLoadResistance() {
            double val = ((mMaxLoadResistance&0xf8)>>3);
            val = 5 * (val+1);
            Log.v(LOGTAG, "getMaxLoadResistance<=" + val);
            if (val > 55) val = 55.0;
            return val;
        }

        public float getMaxDevicesSupported() {
            int val = mMaxDevicesSupported +1;
            Log.v(LOGTAG, "getMaxDevicesSupported<=" + val);
            if (val > 8) val = 8;
            return val;
        }

        public short getId() {
            return mId;
        }

        public int getPtuClass() {
            return (mClass > 4) ? 5 : (mClass+1);
        }

        public byte getHwRev () {
            return mHwRev;
        }

        public byte getFwRev () {
            return mFwRev;
        }

        public byte getProtocolRev () {
            return mProtocolRev;
        }
    }

    public static String toHex(int num) {
        return String.format("0x%8s", Integer.toHexString(num)).replace(' ', '0');
    }

    private class PruControl {
         public byte mEnable;
         public byte mPermission;
         public byte mTimeSet;
         public short mReserved;
         public PruControl (byte[] value) {
             mEnable = (byte)value[0];
             mPermission = (byte)value[1];
             mTimeSet = (byte)value[2];
             mReserved = (short)(value[3] & 0xFF);
             mReserved = (short)((value[4] & 0xFF) << 8);
         }

         public void print() {
             Log.v(LOGTAG, "mEnable: " +  toHex(mEnable));
             Log.v(LOGTAG, "mPermission: " +  toHex(mPermission));
             Log.v(LOGTAG, "mTimeSet: " +  toHex(mTimeSet));
             Log.v(LOGTAG, "mReserved: " +  toHex(mReserved));
         }

         public boolean getEnablePruOutput() {
              if ((mEnable&0x80) == 0x80) return true;
              else return false;
         }

         public boolean getEnableCharger() {
              if ((mEnable&0x40) == 0x40) return true;
              else return false;
         }

         /* returns 0 Maximum power
                    1 66%
                    2 33%
          */
         public PowerLevel getReducePower() {
             PowerLevel res = PowerLevel.POWER_LEVEL_MINIMUM;
             int val = ((mEnable & 0x30) >> 4 );
             if (val == 0) {
                 res = PowerLevel.POWER_LEVEL_MAXIMUM;
             } else if (val == 1 && val == 3) {
                 res = PowerLevel.POWER_LEVEL_MEDIUM;
             } else if (val == 2) {
                 res = PowerLevel.POWER_LEVEL_MINIMUM;
             }
             return res;
         }

         /* returns 0x00 permitted without reason
                    0x01 Permitted with waiting time due to limited affordable power
                    0x80 Denied with system error 3
                    0x81 Denied due to limited affordable power
                    0x82 Denied due to limited PTU Number of Devices
                    0x83 Denied due to limited PTU Class support
          */
         public boolean getPermission() {

             Log.v(LOGTAG, "getPermission" + mPermission);
             if ((mPermission&0x80) == 0x80) return false;
             else return true;
         }

         /* returns time in ms */
         public int getSetTime() {
             return (mTimeSet*10);
         }
    };

    private PruAlert mPruAlert;
    private PruStaticParam mPruStaticParam; //20 bytes
    private PtuStaticParam mPtuStaticParam; //20 bytes
    private static WipowerDynamicParam mPruDynamicParam; //20 bytes
    private WipowerManager mWipowerManager;

    public A4wpService() {
        Log.v(LOGTAG, "A4wpService");
    }

    static private void cleanupService() {
        Log.v(LOGTAG, "cleanupService");
    }

    private int processPruControl(byte[] value) {
        int status = 0;

        Log.v(LOGTAG, "processPruControl>");
        PruControl control = new PruControl(value);
        control.print();
        if (control.getEnablePruOutput()) {
            Log.v(LOGTAG, "do Enable PruOutPut");
            mWipowerManager.startCharging();
            mWipowerManager.enableAlertNotification(false);
            mWipowerManager.enableDataNotification(true);
            stopAdvertising();
        } else {
            Log.v(LOGTAG, "do Disable PruOutPut");
            return status;
        }

        if (control.getEnableCharger()) {
            Log.v(LOGTAG, "do Enable Charging");
        } else {
            Log.v(LOGTAG, "do Disable Charging");
        }

        PowerLevel val = control.getReducePower();
        if (val == PowerLevel.POWER_LEVEL_MAXIMUM) {
            Log.v(LOGTAG, "put to Max Power");
        } else if (val == PowerLevel.POWER_LEVEL_MEDIUM){
            Log.v(LOGTAG, "put to Medium Power");
        } else if (val == PowerLevel.POWER_LEVEL_MINIMUM){
            Log.v(LOGTAG, "put to Min Power");
        }

        mWipowerManager.setPowerLevel(val);

        return status;
    }

    private int processPtuStaticParam(byte[] value) {
        int status = 0;
        Log.v(LOGTAG, "processPtuStaticParam>");
        mPtuStaticParam = new PtuStaticParam(value);
        mPtuStaticParam.print();

        return status;
    }

    private static final int OVER_VOLT_BIT = 0x80;
    private static final byte OVER_CURR_BIT = 0x40;
    private static final byte OVER_TEMP_BIT = 0x20;
    private static final byte SELF_PROT_BIT = 0x10;
    private static final byte CHARGE_COMPLETE_BIT = 0x08;
    private static final byte WIRED_CHARGE_DETECT = 0x04;
    private static final byte CHARGE_PORT = 0x02;

    /**
     * Wipower callbacks
     */
    private final WipowerManagerCallback mWipowerCallback = new WipowerManagerCallback() {

        @Override
        public void onWipowerReady() {
            Log.v(LOGTAG, "onWipowerReady");
        }

        @Override
        public void onWipowerStateChange(WipowerState state) {
            Log.v(LOGTAG, "onWipowerStateChange" + state);
        }

        @Override
        public void onPowerApply(PowerApplyEvent state) {
            Log.v(LOGTAG, "onPowerApply" + state);
            if (state == PowerApplyEvent.ON) {
                startAdvertising();
            } else {
                Log.v(LOGTAG, "Cancel connection as part of -" + state);
                if (mBluetoothGattServer != null) {
                    if (mDevice != null) {
                        mBluetoothGattServer.cancelConnection(mDevice);
                    }
                }
            }
        }

        @Override
        public void onWipowerAlert(WipowerAlert alert) {
            Log.v(LOGTAG, "onWipowerAlert");
            byte alertVal = 0;
            if (alert == WipowerAlert.ALERT_OVER_VOLTAGE) {
                Log.v(LOGTAG, "Over Voltage");
                alertVal |= OVER_VOLT_BIT&0xff;
            }
            else if (alert == WipowerAlert.ALERT_OVER_CURRENT) {
                Log.v(LOGTAG, "Over Current");
                alertVal |= OVER_CURR_BIT;
            }
            else if (alert == WipowerAlert.ALERT_OVER_TEMPERATURE) {
                Log.v(LOGTAG, "Over Temperature");
                alertVal |= OVER_TEMP_BIT;
            }
            else if (alert == WipowerAlert.ALERT_SELF_PROTECTION) {
                Log.v(LOGTAG, "PRU self protection ON");
                alertVal |= SELF_PROT_BIT;
            }
            else if (alert == WipowerAlert.ALERT_CHARGE_COMPLETE) {
                Log.v(LOGTAG, "Charge complete alert ");
                alertVal |= CHARGE_COMPLETE_BIT;
            }
            else if (alert == WipowerAlert.ALERT_WIRED_CHARGER_DETECTED) {
                Log.v(LOGTAG, "Wired charger detected");
                alertVal |= WIRED_CHARGE_DETECT;
            }
            else if (alert == WipowerAlert.ALERT_CHARGE_PORT) {
                Log.v(LOGTAG, "Alert charge port");
                alertVal |= CHARGE_PORT;
            }
        }


        @Override
        public void onWipowerData(WipowerDynamicParam data) {
            Log.v(LOGTAG, "onWipowerData Alert");
            byte[] value = data.getValue();

            Log.v(LOGTAG, "calling SetValue");
            mPruDynamicParam.setAppValue(value);
        }

    };


    /**
     * GATT callbacks
     */
    private final BluetoothGattServerCallback mGattCallbacks = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            mState = newState;
            if (mState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(LOGTAG, "onConnectionStateChange:DISCONNECTED " + device);
                if (mDevice != null) {
                    //Uncomment this later
                    //mWipowerManager.enableDataNotification(false);
                    //mWipowerManager.stopCharging();
                    mDevice = null;
                }
            } else {
                Log.v(LOGTAG, "onConnectionStateChange:CONNECTED");
                mDevice = device;
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite, boolean responseNeeded,
                int offset, byte[] value) {

                UUID id = characteristic.getUuid();
                int status =0;

                Log.v(LOGTAG, "onCharacteristicWriteRequest:" + id);
                if (id == A4WP_PRU_CTRL_UUID)
                {
                     status = processPruControl(value);
                }
                else if(id == A4WP_PTU_STATIC_UUID)
                {
                     status = processPtuStaticParam(value);
                }
                if (responseNeeded == true) {
                    mBluetoothGattServer.sendResponse(device, requestId, status,
                                       offset, value);
                }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                        int offset, BluetoothGattCharacteristic characteristic) {

                UUID id = characteristic.getUuid();
                byte[] value = {0};
                int status = 0;

                Log.v(LOGTAG, "onCharacteristicReadRequest:" + id);
                if (id == A4WP_PRU_ALERT_UUID)
                {
                    value = mPruAlert.getValue();
                }
                else if(id == A4WP_PRU_STATIC_UUID)
                {
                    value = mPruStaticParam.getValue();
                }
                else if (id == A4WP_PRU_DYNAMIC_UUID) {
                    if (mPruDynamicParam == null) {
                         Log.e(LOGTAG, "mPruDynamicParam is NULL");
                         return;
                    }
                    value = mPruDynamicParam.getValue();
                }
                if (value != null)
                {
                     Log.v(LOGTAG, "device:" + id + "requestId:" + requestId + "status:" + status + "offset:" + offset + "value" + value);
                     mBluetoothGattServer.sendResponse(device, requestId, status, offset, value);
                }
        }

        @Override
        public void onServiceAdded(final int status, BluetoothGattService service) {
                Log.i(LOGTAG, "Service added");
        }
    };

    private void closeServer() {
        if (mBluetoothGattServer != null) {
            if (mDevice != null) mBluetoothGattServer.cancelConnection(mDevice);
            mBluetoothGattServer.close();
        }
    }

    private void startAdvertising()
    {
        byte[] wipowerData=new byte[6];
        wipowerData[0]=(byte)0xFE;
        wipowerData[1]=(byte)0xFF;
        wipowerData[2]=(byte)0x28;
        wipowerData[3]=(byte)0x00;
        wipowerData[4]=(byte)0xFF;
        wipowerData[5]=(byte)0x60;
        QBluetoothAdapter mQAdapter=QBluetoothAdapter.getDefaultAdapter();
        if ((mQAdapter != null)) {
           int modeAd=mQAdapter.getLEAdvMode();
           Log.d(LOGTAG,"Adv mode is:"+ modeAd);

           mQAdapter.setLEServiceData(wipowerData);
           mQAdapter.setLEAdvMask(false, false, false, false, true);
           boolean retval=mQAdapter.setLEAdvMode(QBluetoothAdapter.ADV_IND_LIMITED_CONNECTABLE);
           Log.d(LOGTAG,"Return value of set adv enable is:"+retval);
        } else {
           boolean retval=mQAdapter.setLEAdvMode(QBluetoothAdapter.ADV_MODE_NONE);
           Log.d(LOGTAG,"Return value of set adv enable is:"+retval);
        }
    }

    private void stopAdvertising()
    {
        QBluetoothAdapter mQAdapter=QBluetoothAdapter.getDefaultAdapter();
        if ((mQAdapter != null)) {
           boolean retval=mQAdapter.setLEAdvMode(QBluetoothAdapter.ADV_MODE_NONE);
           Log.d(LOGTAG,"Return value of set adv enable is:"+retval);
        }
    }

    private boolean startServer() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return false;

        mBluetoothGattServer = bluetoothManager.openGattServer(this, mGattCallbacks);
        Log.d(LOGTAG,"calling start server......");
        if (mBluetoothGattServer == null) return false;

        BluetoothGattCharacteristic pruControl = new BluetoothGattCharacteristic(
                A4WP_PRU_CTRL_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE |
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattCharacteristic ptuStatic = new BluetoothGattCharacteristic(
                A4WP_PTU_STATIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE |
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattCharacteristic pruAlert = new BluetoothGattCharacteristic(
                A4WP_PRU_ALERT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ );

        BluetoothGattCharacteristic pruStatic = new BluetoothGattCharacteristic(
                A4WP_PRU_STATIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattCharacteristic pruDynamic = new BluetoothGattCharacteristic(
                A4WP_PRU_DYNAMIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);


        BluetoothGattDescriptor pruAlertDesc = new BluetoothGattDescriptor(
                A4WP_PRU_ALERT_DESC_UUID,
                BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        pruAlert.addDescriptor(pruAlertDesc);

        BluetoothGattService a4wpService = new BluetoothGattService(
                A4WP_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        a4wpService.addCharacteristic(pruControl);
        a4wpService.addCharacteristic(ptuStatic);
        a4wpService.addCharacteristic(pruAlert);
        a4wpService.addCharacteristic(pruStatic);
        a4wpService.addCharacteristic(pruDynamic);


        mBluetoothGattServer.addService(a4wpService);
        Log.d(LOGTAG,"time to start advertising....:");

        //startAdvertising();

        return true;
    }

    @Override
    public void onCreate() {
        Log.v(LOGTAG, "onCreate");
        super.onCreate();

        // Ensure Bluetooth is enabled
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(LOGTAG, "Bluetooth is not available or enabled - exiting...");
            return;
        }

        Log.v(LOGTAG, "calling startService");
        startServer();
        //Initialize PRU Static param
        mPruStaticParam = new PruStaticParam();
        mPruDynamicParam = new WipowerDynamicParam();

        mWipowerManager = WipowerManager.getWipowerManger(this, mWipowerCallback);
        if (mWipowerManager != null)
             mWipowerManager.registerCallback(mWipowerCallback);
    }

    @Override
    public void onDestroy() {
        Log.v(LOGTAG, "onDestroy");
        if (mWipowerManager != null)
             mWipowerManager.unregisterCallback(mWipowerCallback);
    }

    @Override
    public IBinder onBind(Intent in) {
        Log.v(LOGTAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "onStart Command called!!");
        //Make this restarable service by
        //Android app manager
        return START_STICKY;
   }
}
