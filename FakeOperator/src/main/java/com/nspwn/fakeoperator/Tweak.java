package com.nspwn.fakeoperator;

import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.systemui.statusbar.policy.NetworkController;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class Tweak {
    private static final String TAG = "com.nspwn.fakeoperator.Tweak";

    static void initialize() {
        Log.i(TAG, "tweak loaded");
        Log.i(TAG, "waiting for class load(s)...");

        hookNetworkController();
        hookServiceState();
        hookTelephonyManager();
    }

    private static void hookNetworkController() {
        MS.hookClassLoad("com.android.systemui.statusbar.policy.NetworkController", new MS.ClassLoadHook() {
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "hooking updateNetworkName");
                    Method updateNetworkName = resources.getMethod("updateNetworkName", boolean.class, String.class, boolean.class, String.class);

                    if (updateNetworkName != null) {
                        Log.d(TAG, "hooking updateNetworkName");

                        //updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
                        MS.hookMethod(resources, updateNetworkName, new MS.MethodAlteration<NetworkController, Void>() {
                            public Void invoked(NetworkController networkController, Object... args) throws Throwable {
                                Log.d(TAG, "running updateNetworkName");
                                boolean showSpn = (Boolean)args[0];
                                String spn = "SPN";//(String)args[1];
                                boolean showPlmn = (Boolean)args[2];
                                String plmn = "PLMN";//(String)args[3];

                                if (BuildConfig.DEBUG) {
                                    Log.d("CarrierLabel", String.format("updateNetworkName showSpn=%b  spn=%s showPlmn=%b plmn=%s", showSpn, spn, showPlmn, plmn));
                                }

                                return invoke(networkController, showSpn, spn, showPlmn, plmn);
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
