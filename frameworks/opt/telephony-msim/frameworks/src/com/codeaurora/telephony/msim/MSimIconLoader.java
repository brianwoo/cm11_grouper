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

import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.IconLoader;
import com.android.internal.telephony.uicc.IccFileHandler;

import android.graphics.Bitmap;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.HashMap;

/**
 * Class for loading icons from the SIM card. Has two states: single, for loading
 * one icon. Multi, for loading icons list.
 *
 */
class MSimIconLoader extends IconLoader {

    public MSimIconLoader(Handler caller, IccFileHandler fh) {
        HandlerThread thread = new HandlerThread("Cat Icon Loader");
        thread.start();
        mSimFH = fh;

        mIconsCache = new HashMap<Integer, Bitmap>(50);
    }
}
