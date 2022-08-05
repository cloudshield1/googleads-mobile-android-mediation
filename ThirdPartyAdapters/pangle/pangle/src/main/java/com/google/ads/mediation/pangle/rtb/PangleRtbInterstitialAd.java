package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class PangleRtbInterstitialAd implements MediationInterstitialAd {

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback;
  private MediationInterstitialAdCallback interstitialAdCallback;
  private PAGInterstitialAd pagInterstitialAd;

  public PangleRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    adConfiguration = mediationInterstitialAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleMediationAdapter.setCoppa(adConfiguration.taggedForChildDirectedTreatment());
    PangleMediationAdapter.setUserData(adConfiguration.getMediationExtras());

    String placementId = adConfiguration.getServerParameters()
        .getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load interstitial ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_BID_RESPONSE,
          "Failed to load interstitial ad from Pangle. Missing or invalid bid response.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    PAGInterstitialRequest request = new PAGInterstitialRequest();
    request.setAdString(bidResponse);
    PAGInterstitialAd.loadAd(placementId, request, new PAGInterstitialAdLoadListener() {
      @Override
      public void onError(int errorCode, String errorMessage) {
        AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
        Log.w(TAG, error.toString());
        adLoadCallback.onFailure(error);
      }

      @Override
      public void onAdLoaded(PAGInterstitialAd interstitialAd) {
        interstitialAdCallback = adLoadCallback.onSuccess(PangleRtbInterstitialAd.this);
        pagInterstitialAd = interstitialAd;
      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    pagInterstitialAd.setAdInteractionListener(
        new PAGInterstitialAdInteractionListener() {
          @Override
          public void onAdShowed() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.onAdOpened();
              interstitialAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdClicked() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdDismissed() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.onAdClosed();
            }
          }
        });
    if (context instanceof Activity) {
      pagInterstitialAd.show((Activity) context);
      return;
    }
    // If the context is not an Activity, the application context will be used to render the ad.
    pagInterstitialAd.show(null);
  }
}
