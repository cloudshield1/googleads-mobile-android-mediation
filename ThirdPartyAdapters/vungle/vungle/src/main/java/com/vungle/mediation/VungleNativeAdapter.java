package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.vungle.warren.AdConfig;
import com.vungle.warren.NativeAd;
import com.vungle.warren.NativeAdLayout;
import com.vungle.warren.NativeAdListener;
import com.vungle.warren.error.VungleException;
import com.vungle.warren.ui.view.MediaView;
import java.util.ArrayList;
import java.util.Map;

public class VungleNativeAdapter extends UnifiedNativeAdMapper {

  /**
   * Native ad sponsoredBy text key.
   */
  public static final String KEY_SPONSORED_CONTEXT_ASSET = "sponsoredBy";

  /**
   * Would be only used in RecyclerView scenario.
   */
  public static final String EXTRA_DISABLE_FEED_MANAGEMENT = "disableFeedLifecycleManagement";

  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback;
  private MediationNativeAdCallback nativeAdCallback;

  /**
   * Vungle native placement ID.
   */
  private String placementId;

  /**
   * Vungle ad configuration settings.
   */
  private AdConfig adConfig;

  /**
   * Wrapper object for Vungle native ads.
   */
  private VungleNativeAd vungleNativeAd;

  public VungleNativeAdapter(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    this.adConfiguration = mediationNativeAdConfiguration;
    this.callback = callback;
  }

  public void render() {
    Bundle mediationExtras = adConfiguration.getMediationExtras();
    Bundle serverParameters = adConfiguration.getServerParameters();
    NativeAdOptions nativeAdOptions = adConfiguration.getNativeAdOptions();
    final Context context = adConfiguration.getContext();

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
      Log.d(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    placementId = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.d(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    adConfig = VungleExtrasBuilder
        .adConfigWithNetworkExtras(mediationExtras, nativeAdOptions, true);

    Log.d(TAG, "start to render native ads...");

    vungleNativeAd = new VungleNativeAd(context, placementId,
        mediationExtras.getBoolean(EXTRA_DISABLE_FEED_MANAGEMENT, false));
    VungleManager.getInstance().registerNativeAd(placementId, vungleNativeAd);

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context.getApplicationContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadNativeAd();
              }

              @Override
              public void onInitializeError(AdError error) {
                VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);
                Log.d(TAG, error.getMessage());
                callback.onFailure(error);
              }
            });
  }

  private void loadNativeAd() {
    vungleNativeAd.loadNativeAd(adConfig, new NativeListener());
  }

  private class NativeListener implements NativeAdListener {

    @Override
    public void onNativeAdLoaded(NativeAd nativeAd) {
      mapNativeAd();
      nativeAdCallback = callback.onSuccess(VungleNativeAdapter.this);
    }

    @Override
    public void onAdLoadError(String placementId, VungleException exception) {
      VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);
      AdError error = VungleMediationAdapter.getAdError(exception);
      Log.d(TAG, error.getMessage());
      callback.onFailure(error);
    }

    @Override
    public void onAdPlayError(String placementId, VungleException exception) {
      VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);

      AdError error = VungleMediationAdapter.getAdError(exception);
      Log.d(TAG, error.getMessage());
      callback.onFailure(error);
    }

    @Override
    public void onAdClick(String placementId) {
      if (nativeAdCallback != null) {
        nativeAdCallback.reportAdClicked();
        nativeAdCallback.onAdOpened();
      }
    }

    @Override
    public void onAdLeftApplication(String placementId) {
      if (nativeAdCallback != null) {
        nativeAdCallback.onAdLeftApplication();
      }
    }

    @Override
    public void creativeId(String creativeId) {
      // no-op
    }

    @Override
    public void onAdImpression(String placementId) {
      if (nativeAdCallback != null) {
        nativeAdCallback.reportAdImpression();
      }
    }
  }

  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {
    super.trackViews(view, clickableAssetViews, nonClickableAssetViews);
    Log.d(TAG, "trackViews()");
    if (!(view instanceof ViewGroup)) {
      return;
    }

    ViewGroup adView = (ViewGroup) view;

    if (vungleNativeAd.getNativeAd() == null || !vungleNativeAd.getNativeAd().canPlayAd()) {
      return;
    }

    View overlayView = adView.getChildAt(adView.getChildCount() - 1);

    if (!(overlayView instanceof FrameLayout)) {
      Log.d(TAG, "We need FrameLayout to render vungle adOptionsView!");
      return;
    }

    // We will render our privacy icon as a child of containerView
    // and place at one of the four corners.
    vungleNativeAd.getNativeAd().setAdOptionsRootView((FrameLayout) overlayView);

    View iconView = null;
    ArrayList<View> assetViews = new ArrayList<>();
    for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
      assetViews.add(clickableAssets.getValue());

      if (clickableAssets.getKey().equals(UnifiedNativeAdAssetNames.ASSET_ICON)) {
        iconView = clickableAssets.getValue();
      }
    }

    if (!(iconView instanceof ImageView)) {
      Log.d(TAG, "Native app icon asset is not of type ImageView!");
      return;
    }

    vungleNativeAd.getNativeAd()
        .registerViewForInteraction(vungleNativeAd.getNativeAdLayout(),
            vungleNativeAd.getMediaView(), (ImageView) iconView,
            assetViews);
  }

  @Override
  public void untrackView(@NonNull View view) {
    super.untrackView(view);
    Log.d(TAG, "untrackView()");
    if (vungleNativeAd.getNativeAd() == null) {
      return;
    }

    vungleNativeAd.getNativeAd().unregisterView();
  }

  private void mapNativeAd() {
    NativeAd nativeAd = vungleNativeAd.getNativeAd();
    String title = nativeAd.getAdTitle();
    if (title != null) {
      setHeadline(title);
    }
    String body = nativeAd.getAdBodyText();
    if (body != null) {
      setBody(body);
    }
    String cta = nativeAd.getAdCallToActionText();
    if (cta != null) {
      setCallToAction(cta);
    }
    Double starRating = nativeAd.getAdStarRating();
    if (starRating != null) {
      setStarRating(starRating);
    }

    String sponsored = nativeAd.getAdSponsoredText();
    if (sponsored != null) {
      Bundle extras = new Bundle();
      extras.putString(KEY_SPONSORED_CONTEXT_ASSET, sponsored);
      setExtras(extras);
    }

    NativeAdLayout nativeAdLayout = vungleNativeAd.getNativeAdLayout();
    MediaView mediaView = vungleNativeAd.getMediaView();
    nativeAdLayout.removeAllViews();
    nativeAdLayout.addView(mediaView);
    setMediaView(nativeAdLayout);

    String iconUrl = nativeAd.getAppIcon();
    if (iconUrl != null && iconUrl.startsWith("file://")) {
      iconUrl = iconUrl.substring("file://".length());
    }
    setIcon(new VungleNativeMappedImage(Uri.parse(iconUrl)));

    setOverrideImpressionRecording(true);
    setOverrideClickHandling(true);
  }

  private static class VungleNativeMappedImage extends
      com.google.android.gms.ads.formats.NativeAd.Image {

    private Drawable drawable;
    private Uri imageUri;

    public VungleNativeMappedImage(Drawable drawable) {
      this.drawable = drawable;
    }

    public VungleNativeMappedImage(Uri imageUrl) {
      this.imageUri = imageUrl;
    }

    @Override
    public Drawable getDrawable() {
      return drawable;
    }

    @Override
    public Uri getUri() {
      return imageUri;
    }

    @Override
    public double getScale() {
      return 1;
    }
  }

  @NonNull
  @Override
  public String toString() {
    return " [placementId="
        + placementId
        + " # hashcode="
        + hashCode()
        + " # vungleNativeAd="
        + vungleNativeAd
        + "] ";
  }
}
