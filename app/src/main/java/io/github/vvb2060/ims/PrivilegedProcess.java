package io.github.vvb2060.ims;

import static rikka.shizuku.ShizukuProvider.METHOD_GET_BINDER;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

public class PrivilegedProcess extends Instrumentation {
    static final String TAG = "vvb";

    @Override
    public void onCreate(Bundle arguments) {
        var binder = new Binder() {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                if (code == 1) {
                    try {
                        var context = getContext();
                        var persistent = canPersistent(context);
                        overrideConfig(context, persistent);
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    var handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> finish(0, new Bundle()), 1000);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
        };
        var extras = new Bundle();
        extras.putBinder("binder", binder);
        var cr = getContext().getContentResolver();
        cr.call(BuildConfig.APPLICATION_ID + ".shizuku", METHOD_GET_BINDER, null, extras);
    }

    @SuppressLint("PrivateApi")
    private static boolean canPersistent(Context context) {
        try {
            var gms = context.createPackageContext("com.android.phone",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            var clazz = gms.getClassLoader().loadClass("com.android.phone.CarrierConfigLoader");
            try {
                clazz.getDeclaredMethod("isSystemApp");
            } catch (NoSuchMethodException e) {
                return true;
            }
            clazz.getDeclaredMethod("secureOverrideConfig", PersistableBundle.class, boolean.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private static void overrideConfig(Context context, boolean persistent) {
        var cm = context.getSystemService(CarrierConfigManager.class);
        var sm = context.getSystemService(SubscriptionManager.class);
        var values = getConfig();
        var subInfos = sm.getActiveSubscriptionInfoList();
        if (subInfos == null) return;
        for (var subInfo : subInfos) {
            var subId = subInfo.getSubscriptionId();
            var bundle = cm.getConfigForSubId(subId);
            if (bundle == null || bundle.getInt("vvb2060_config_version", 0) != BuildConfig.VERSION_CODE) {
                values.putInt("vvb2060_config_version", BuildConfig.VERSION_CODE);
                try {
                    cm.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class, boolean.class)
                            .invoke(cm, subId, values, persistent);
                } catch (Exception e) {
                    try {
                        cm.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class)
                                .invoke(cm, subId, values);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to override config", ex);
                    }
                }
            }
        }
    }

    private static PersistableBundle getConfig() {
        var bundle = new PersistableBundle();

        // 5G/IMS Unlock
        bundle.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA});
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true);

        // Metro signal / Handover optimization (QNS)
        bundle.putInt("qns.minimum_handover_guarding_timer_ms_int", 1000);
        bundle.putIntArray("qns.voice_ngran_ssrsrp_int_array", new int[]{-120, -124});
        bundle.putIntArray("qns.ho_restrict_time_with_low_rtp_quality_int_array", new int[]{3000, 3000});

        // Signal Display Optimization
        bundle.putIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                // Boundaries: [-140 dBm, -44 dBm]
                new int[]{
                        -125, /* SIGNAL_STRENGTH_POOR */
                        -115, /* SIGNAL_STRENGTH_MODERATE */
                        -105, /* SIGNAL_STRENGTH_GOOD */
                        -95,  /* SIGNAL_STRENGTH_GREAT */
                });

        // GPS/Location Optimization
        bundle.putString("gps.normal_psds_server", "gllto.glpals.com");
        bundle.putString("gps.longterm_psds_server_1", "gllto.glpals.com");

        // UI/Icon Enhancement
        bundle.putString("5g_icon_configuration_string", "connected_mmwave:5G_PLUS");
        bundle.putIntArray("additional_nr_advanced_bands_int_array", new int[]{78});

        // Other Enhancements
        bundle.putInt("imssms.sms_max_retry_over_ims_count_int", 3);
        bundle.putBoolean("unmetered_nr_sa_bool", true);
        bundle.putBoolean("apn_expand_bool", true);

        // Existing configurations preserved
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
        bundle.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true);
        bundle.putInt("wfc_spn_format_idx_int", 6);
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false);
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false);
        bundle.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);

        return bundle;
    }
}
