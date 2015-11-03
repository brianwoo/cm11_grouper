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

import com.android.internal.telephony.cat.CommandParamsFactory;
import com.android.internal.telephony.uicc.IccFileHandler;

/**
 * Factory class, used for decoding raw byte arrays, received from baseband,
 * into a CommandParams object.
 *
 */
class MSimCommandParamsFactory extends CommandParamsFactory {

    static synchronized MSimCommandParamsFactory getInstance(MSimRilMessageDecoder caller,
            IccFileHandler fh) {

        if (fh != null) {
            return new MSimCommandParamsFactory(caller, fh);
        }

        return null;
    }

    private MSimCommandParamsFactory(MSimRilMessageDecoder caller, IccFileHandler fh) {
        mCaller = caller;
        mIconLoader = new MSimIconLoader(this, fh);
    }

}
