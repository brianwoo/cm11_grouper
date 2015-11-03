/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2013 The Linux Foundation. All rights reserved.
 *
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

/**
 * The intents that the telephony services broadcast.
 *
 * <p class="warning">
 * THESE ARE NOT THE API!  Use the {@link android.telephony.TelephonyManager} class.
 * DON'T LISTEN TO THESE DIRECTLY.
 */
public class MSimTelephonyIntents {

     /**
     * Broadcast Action: The default subscription has changed.  This has the following
     * extra values:</p>
     * <ul>
     *   <li><em>subscription</em> - A int, the current default subscription.</li>
     * </ul>
     */
    public static final String ACTION_DEFAULT_SUBSCRIPTION_CHANGED
            = "qualcomm.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED";
}
