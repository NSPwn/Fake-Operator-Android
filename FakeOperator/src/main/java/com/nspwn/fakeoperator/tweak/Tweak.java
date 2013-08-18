package com.nspwn.fakeoperator.tweak;

import android.content.SharedPreferences;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nspwn.fakeoperator.BuildConfig;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class Tweak {
    private static final String TAG = "com.nspwn.fakeoperator.tweak.Tweak";

    static void initialize() {
        Log.i(TAG, "tweak loaded");
        Log.i(TAG, "waiting for class load(s)...");

        hookNetworkController();
    }

    private static void hookNetworkController() {
        MS.hookClassLoad("com.android.systemui.statusbar.policy.NetworkController", new MS.ClassLoadHook() {
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "hooking updateNetworkName");
                    Method updateNetworkName = resources.getDeclaredMethod("updateNetworkName", boolean.class, String.class, boolean.class, String.class);

                    if (updateNetworkName != null) {
                        Log.d(TAG, "hooking updateNetworkName");
                        //updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {

                        MS.hookMethod(resources, updateNetworkName, new MS.MethodAlteration<Object, Void>() {
                            public Void invoked(Object networkController, Object... args) throws Throwable {
                                Log.d(TAG, "running updateNetworkName");
                                boolean showSpn = (Boolean)args[0];
                                String spn = (String)args[1];
                                boolean showPlmn = (Boolean)args[2];
                                String plmn = (String)args[3];

                                SharedPreferences preferences = Settings.getInstance().getPreferences();

                                if (preferences != null) {
                                    boolean enabled = preferences.getBoolean("tweak_enabled", false);
                                    String fakeOperator = preferences.getString("fake_operator", "NSPwn");

                                    Log.d(TAG, String.format("enabled=%b;fake_operator=%s;", enabled, fakeOperator));

                                    if (enabled) {
                                        spn = fakeOperator;
                                    }
                                }

                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, String.format("updateNetworkName showSpn=%b  spn=%s showPlmn=%b plmn=%s", showSpn, spn, showPlmn, plmn));
                                }


                                return invoke(networkController,  showSpn, spn, showPlmn, plmn);
                            }
                        });
                    }
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "error hooking method...", e);
                }
            }
        });
    }

    private static void hookTelephonyManager() {
        MS.hookClassLoad("android.telephony.TelephonyManager", new MS.ClassLoadHook() {
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "hooking getNetworkOperatorName");
                    Method getNetworkOperatorName = resources.getMethod("getNetworkOperatorName");

                    if (getNetworkOperatorName != null) {
                        Log.d(TAG, "got getNetworkOperatorName");
                        MS.hookMethod(resources, getNetworkOperatorName, new MS.MethodAlteration<TelephonyManager, String>() {
                            public String invoked(TelephonyManager telephonyManager, Object... args) throws Throwable {
                                String real = invoke(telephonyManager, args);
                                Log.d(TAG, String.format("running getNetworkOperatorName (real: %s)", real));

                                if (real != null) {
                                    return "TEST";// String.format("FO:A - %s", real);
                                }

                                return null;
                            }
                        });
                    }

                    Log.d(TAG, "hooking getSimOperatorName");
                    Method getSimOperatorName = resources.getMethod("getSimOperatorName");

                    if (getSimOperatorName != null) {
                        Log.d(TAG, "got getSimOperatorName");
                        MS.hookMethod(resources, getSimOperatorName, new MS.MethodAlteration<TelephonyManager, String>() {
                            public String invoked(TelephonyManager telephonyManager, Object... args) throws Throwable {
                                String real = invoke(telephonyManager, args);
                                Log.d(TAG, String.format("running getSimOperatorName (real: %s)", real));

                                if (real != null) {
                                    return "TEST";// String.format("FO:A - %s", real);
                                }

                                return null;
                            }
                        });
                    }
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "error hooking method...", e);
                }
            }
        });
    }

    private static void hookServiceState() {
        MS.hookClassLoad("android.telephony.ServiceState", new MS.ClassLoadHook() {
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "hooking getOperatorAlphaLong");
                    Method getOperatorAlphaLong = resources.getMethod("getOperatorAlphaLong");

                    if (getOperatorAlphaLong != null) {
                        Log.d(TAG, "got getOperatorAlphaLong");

                        MS.hookMethod(resources, getOperatorAlphaLong, new MS.MethodAlteration<ServiceState, String>() {
                            public String invoked(ServiceState serviceState, Object... args) throws Throwable {
                                String real = invoke(serviceState, args);
                                Log.d(TAG, String.format("running getOperatorAlphaLong (real: %s)", real));

                                if (real != null) {
                                    return "TEST";// String.format("FO:A - %s", real);
                                }

                                return null;
                            }
                        });
                    }

                    Method getOperatorAlphaShort = resources.getMethod("getOperatorAlphaShort");
                    if (getOperatorAlphaShort != null) {
                        Log.i(TAG, "got getOperatorAlphaShort");

                        MS.hookMethod(resources, getOperatorAlphaShort, new MS.MethodAlteration<ServiceState, String>() {
                            public String invoked(ServiceState serviceState, Object... args) throws Throwable {
                                String real = invoke(serviceState, args);
                                Log.d(TAG, String.format("running getOperatorAlphaShort (real: %s)", real));

                                if (real != null) {
                                    return "TEST";// String.format("FO:A - %s", real);
                                }

                                return null;
                            }
                        });
                    }
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "error hooking method...", e);
                }
            }
        });
    }
}