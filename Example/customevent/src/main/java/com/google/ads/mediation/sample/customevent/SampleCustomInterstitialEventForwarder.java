/*
 * Copyright (C) 2014 Google, Inc.
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

package com.google.ads.mediation.sample.customevent;

import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;

/**
 * A {@link SampleAdListener} that forwards events to AdMob Mediation's
 * {@link CustomEventInterstitialListener}.
 */
public class SampleCustomInterstitialEventForwarder extends SampleAdListener {
    private CustomEventInterstitialListener mInterstitialListener;

    /**
     * Creates a new {@code SampleInterstitialEventForwarder}.
     * @param listener An AdMob Mediation {@link CustomEventInterstitialListener} that should
     *                 receive forwarded events.
     */
    public SampleCustomInterstitialEventForwarder(CustomEventInterstitialListener listener) {
        this.mInterstitialListener = listener;
    }

    @Override
    public void onAdFetchSucceeded() {
        mInterstitialListener.onAdLoaded();
    }

    @Override
    public void onAdFetchFailed(SampleErrorCode errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                break;
            case BAD_REQUEST:
                mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
            case NETWORK_ERROR:
                mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NETWORK_ERROR);
                break;
            case NO_INVENTORY:
                mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
                break;
        }
    }

    @Override
    public void onAdFullScreen() {
        mInterstitialListener.onAdOpened();
        // Only call onAdLeftApplication if your ad network actually exits the developer's app.
        mInterstitialListener.onAdLeftApplication();
    }

    @Override
    public void onAdClosed() {
        mInterstitialListener.onAdClosed();
    }
}