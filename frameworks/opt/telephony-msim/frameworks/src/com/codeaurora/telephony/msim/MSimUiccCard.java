/*
 * Copyright (C) 2006, 2012 The Android Open Source Project
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

import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.UiccCard;

/**
 * {@hide}
 */
public class MSimUiccCard extends UiccCard {
    int mSlotId;

    public MSimUiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int slotId) {
        if (DBG) log("Creating MSimUiccCard");
        mCardState = ics.mCardState;
        mSlotId = slotId;
        update(c, ci, ics);
    }

    @Override
    protected void createAndUpdateCatService() {
        if (mUiccApplications.length > 0 && mUiccApplications[0] != null) {
            if (mCatService == null) {
                mCatService = MSimCatService.getInstance(mCi, mContext, this, mSlotId);
            } else {
                ((MSimCatService)mCatService).update(mCi, mContext, this);
            }
        } else {
            if (mCatService != null) {
                mCatService.dispose();
            }
            mCatService = null;
        }
    }

    public CatService getCatService() {
        return mCatService;
    }
}
