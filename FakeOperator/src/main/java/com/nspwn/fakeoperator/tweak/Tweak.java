package com.nspwn.fakeoperator.tweak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.UserHandle;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;

import com.saurik.substrate.MS;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.content.res.Resources.getSystem;

@SuppressWarnings("unused")
public class Tweak {
    private static final String TAG = "com.nspwn.fakeoperator.tweak.Tweak";
    public static final int SPN_RULE_SHOW_PLMN = 0x02;
    public static final int SPN_RULE_SHOW_SPN = 0x01;
    private static Object gsmServiceInstance = null;
    private static String emergencyCallsOnly = null;
    private static String lockscreenCarrierDefault = null;
    private static boolean errorForceDisable = false;
    private static boolean useOldHooks = false;
    private static UserHandle userHandle;
    private static Method sendStickyBroadcastAsUser;

    static void initialize() {
        Log.i(TAG, "tweak loaded");
        Log.i(TAG, "waiting for class load(s)...");

        if (Build.VERSION.SDK_INT < 18) {
            useOldHooks = true;
        }

        cacheInternal();
        hookServiceStateTracker();
    }

//    public static void forceUpdateSpnDisplay() {
//        if (gsmServiceInstance == null)
//            return;
//
//        try {
//            Log.d(TAG, "forcing spn display to update");
//            Method updateSpnDisplay = gsmServiceInstance.getClass().getDeclaredMethod("updateSpnDisplay");
//            updateSpnDisplay.setAccessible(true);
//            updateSpnDisplay.invoke(gsmServiceInstance);
//        } catch (IllegalAccessException e) {
//            Log.d(TAG, "error illegal access...", e);
//        } catch (InvocationTargetException e) {
//            Log.d(TAG, "error invocation target...", e);
//        } catch (NoSuchMethodException e) {
//            Log.d(TAG, "error no such method...", e);
//        }
//    }

    private static Field findField(Class<?> clazz, String... names) {
        return findField(clazz, false, names);
    }

    private static Field findField(Class<?> clazz, boolean ignoreFatal, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);

                if (f != null) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "error no such field...", e);
            }
        }

        if (!ignoreFatal) {
            errorForceDisable = true;
        }

        return null;
    }

    private static Method findMethod(Class<?> clazz, String... names) {
        return findMethod(clazz, false, names);
    }

    private static Method findMethod(Class<?> clazz, boolean ignoreFatal, String... names) {
        for (String name : names) {
            try {
                Method m = clazz.getDeclaredMethod(name);

                if (m != null) {
                    m.setAccessible(true);
                    return m;
                }
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "error no such method...", e);
            }
        }

        if (!ignoreFatal) {
            errorForceDisable = true;
        }

        return null;
    }

    private static void cacheInternal() {
        try {
            Log.d(TAG, "Trying to hook com.android.internal.R$string");
            Class<?> internalRString;

            internalRString = Class.forName("com.android.internal.R$string");

            if (internalRString != null) {
                emergencyCallsOnly = getSystem().getText(findField(internalRString, "emergency_calls_only").getInt(null)).toString();
                lockscreenCarrierDefault = getSystem().getText(findField(internalRString, "lockscreen_carrier_default").getInt(null)).toString();
            } else {
                errorForceDisable = true;
            }

            Log.d(TAG, "hooked com.android.internal.R$string");
            Log.d(TAG, "Hooked strings lockscreen_carrier_default='" + lockscreenCarrierDefault + "' + emergency_calls_only='" + emergencyCallsOnly + "'");

            if (!useOldHooks) {
                Log.d(TAG, "Trying to hook the ALL user handle");
                userHandle =  (UserHandle) findField(UserHandle.class, "ALL").get(null);
                Log.d(TAG, "Hooked the ALL user handle");

                Log.d(TAG, "Trying to hook sendStickyBroadcastAsUser");
                sendStickyBroadcastAsUser = Context.class.getDeclaredMethod("sendStickyBroadcastAsUser", Intent.class, UserHandle.class);
                sendStickyBroadcastAsUser.setAccessible(true);
                Log.d(TAG, "Hooked sendStickyBroadcastAsUser");
            }
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "error class not found...", e);
        } catch (Throwable throwable) {
            Log.d(TAG, "error thrown...", throwable);
        }
    }

    private static void hookServiceStateTracker() {
        hookCdmaServiceStateTracker();
        hookGsmServiceStateTracker();
    }

    private static void hookGsmServiceStateTracker() {
        Log.d(TAG, "Attempting to hook GsmServiceStateTracker");

        MS.hookClassLoad("com.android.internal.telephony.gsm.GsmServiceStateTracker", new MS.ClassLoadHook() {
            @Override
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "Hooked GsmServiceStateTracker");

                    Log.d(TAG, "Trying to hook updateSpnDisplay");
                    Method updateSpnDisplay = findMethod(resources, "updateSpnDisplay");

                    if (updateSpnDisplay != null) {
                        Log.d(TAG, "hooked updateSpnDisplay");
                        final Class<?> superClazz = resources.getSuperclass();
                        final Field serviceState = findField(superClazz, "mSS", "ss");
                        final Field iccRecords = findField(superClazz, false, "mIccRecords");
                        final Field emergencyOnly = findField(resources, "mEmergencyOnly");
                        final Field curShowSpn = findField(resources, "mCurShowSpn");
                        final Field curShowPlmn = findField(resources, "mCurShowPlmn");
                        final Field curSpn = findField(resources, "mCurSpn", "curSpn");
                        final Field curPlmn = findField(resources, "mCurPlmn", "curPlmn");
                        final Field commandsInterface = useOldHooks ? findField(superClazz, "cm") : null;
                        final Field localPhone = useOldHooks ? findField(superClazz, "phone") : null;
                        final Field curSpnRule = useOldHooks ? findField(resources, "curSpnRule") : null;
                        MS.hookMethod(resources, updateSpnDisplay, new MS.MethodAlteration<Object, Void>() {
                            @Override
                            public Void invoked(Object gsmServiceStateTracker, Object... args) throws Throwable {
                                if (errorForceDisable) {
                                    Log.d(TAG, "tweak force disabled, running stock method");
                                    return invoke(gsmServiceStateTracker, args);
                                } else {
                                    Log.d(TAG, "GSM spn display hit");
                                    if (useOldHooks) {
                                        Log.d(TAG, "Using old hook API <18");
                                        //TODO old hook
                                        Field mIccRecords = findField(localPhone.getClass().getSuperclass(), "mIccRecords");
                                        Object iccRecords = mIccRecords.get(localPhone);
                                        Object mSS = serviceState.get(gsmServiceStateTracker);

                                        int rule = 0;

                                        Method getOperatorAlphaLong = findMethod(mSS.getClass(), "getOperatorAlphaLong");
                                        Method getServiceProviderName = findMethod(mIccRecords.getClass(), "getServiceProviderName");

                                        String spn = (String)getServiceProviderName.invoke(iccRecords);
                                        String plmn = (String)getOperatorAlphaLong.invoke(mSS);

                                        if (mIccRecords != null) {
                                            Method getDisplayRule = findMethod(mIccRecords.getClass(), "getDisplayRule");
                                            Method getOperatorNumeric = findMethod(mSS.getClass(), "getOperatorNumeric");

                                            String operatorNumeric = (String) getOperatorNumeric.invoke(mSS);
                                            rule = (Integer) getDisplayRule.invoke(mIccRecords, operatorNumeric);
                                        }

                                        Object cm = commandsInterface.get(gsmServiceStateTracker);
                                        Method getRadioState = findMethod(cm.getClass(), "getRadioState");
                                        Object radioState = getRadioState.invoke(cm);
                                        Method radioState$isOn = findMethod(radioState.getClass(), "isOn");
                                        boolean isOn = (Boolean) radioState$isOn.invoke(radioState);
                                        boolean mEmergencyOnly = emergencyOnly.getBoolean(gsmServiceStateTracker);

                                        if (mEmergencyOnly && isOn) {
                                            plmn = emergencyCallsOnly;
                                            Log.d(TAG, "updateSpnDisplay: emergency only and radio is on plmn='" + plmn + "'");
                                        }

                                        int mCurSpnRule = curSpnRule.getInt(gsmServiceStateTracker);

                                        if (rule != mCurSpnRule
                                            || !TextUtils.equals(spn, (String) curSpn.get(gsmServiceStateTracker))
                                            || !TextUtils.equals(plmn, (String) curPlmn.get(gsmServiceStateTracker))) {
                                            boolean showSpn = !mEmergencyOnly && !TextUtils.isEmpty(spn)
                                                    && (rule & SPN_RULE_SHOW_SPN) == SPN_RULE_SHOW_SPN;
                                            boolean showPlmn = !TextUtils.isEmpty(plmn)
                                                    && (rule & SPN_RULE_SHOW_PLMN) == SPN_RULE_SHOW_PLMN;

                                            Log.d(TAG, String.format("GSM updateSpnDisplay: changed sending intent rule="
                                                    + rule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                                                    showPlmn, plmn, showSpn, spn));

                                            broadcastSpnUpdate(gsmServiceStateTracker, plmn, showPlmn, spn, showSpn);
                                        }

                                        curSpnRule.setInt(gsmServiceStateTracker, rule);
                                        curSpn.set(gsmServiceStateTracker, spn);
                                        curPlmn.set(gsmServiceStateTracker, plmn);
                                    } else {
                                        Log.d(TAG, "Using new hook API >18");
                                        gsmServiceInstance = gsmServiceStateTracker;

                                        Object mSS = serviceState.get(gsmServiceStateTracker);
                                        Object mIccRecords = iccRecords.get(gsmServiceStateTracker);
                                        String plmn = null;
                                        boolean showPlmn = false;
                                        int rule = 1;

                                        if (mIccRecords != null) {
                                            Method getDisplayRule = findMethod(mIccRecords.getClass(), "getDisplayRule");
                                            Method getOperatorNumeric = findMethod(mSS.getClass(), "getOperatorNumeric");

                                            String operatorNumeric = (String) getOperatorNumeric.invoke(mSS);
                                            rule = (Integer) getDisplayRule.invoke(mIccRecords, operatorNumeric);
                                        }

                                        Method getVoiceRegState = findMethod(mSS.getClass(), "getVoiceRegState");
                                        int voiceRegState = (Integer) getVoiceRegState.invoke(mSS);

                                        if (voiceRegState == ServiceState.STATE_OUT_OF_SERVICE || voiceRegState == ServiceState.STATE_EMERGENCY_ONLY) {
                                            boolean mEmergencyOnly = emergencyOnly.getBoolean(gsmServiceStateTracker);
                                            showPlmn = true;

                                            if (mEmergencyOnly) {
                                                plmn = emergencyCallsOnly;
                                            } else {
                                                plmn = lockscreenCarrierDefault;
                                            }

                                            Log.d(TAG, "GSM updateSpnDisplay: radio is on but out of service, set plmn='" + plmn + "'");
                                        } else {
                                            if (voiceRegState == ServiceState.STATE_IN_SERVICE) {
                                                SharedPreferences preferences = Settings.getInstance().getPreferences();
                                                boolean enabled = preferences.getBoolean("tweak_enabled", false);
                                                String fakeOperator = preferences.getString("fake_operator", "NSPwn");

                                                if (enabled) {
                                                    plmn = fakeOperator;
                                                } else {

                                                    Method getOperatorAlphaLong = findMethod(mSS.getClass(), "getOperatorAlphaLong");
                                                    plmn = (String) getOperatorAlphaLong.invoke(mSS);
                                                }

                                                showPlmn = !TextUtils.isEmpty(plmn) &&
                                                        ((rule & SPN_RULE_SHOW_PLMN) == SPN_RULE_SHOW_PLMN);
                                            } else {
                                                Log.d(TAG, "GSM updateSpnDisplay: radio is off with showPlmn=" + showPlmn + " plmn=" + plmn);
                                            }
                                        }

                                        Method getServiceProviderName = findMethod(mIccRecords.getClass(), "getServiceProviderName");
                                        String spn = (mIccRecords != null) ? (String) getServiceProviderName.invoke(mIccRecords) : "";
                                        boolean showSpn = !TextUtils.isEmpty(spn)
                                                && ((rule & SPN_RULE_SHOW_SPN) == SPN_RULE_SHOW_SPN);

                                        if (showPlmn != curShowPlmn.getBoolean(gsmServiceStateTracker)
                                                || showSpn != curShowSpn.getBoolean(gsmServiceStateTracker)
                                                || !TextUtils.equals(spn, (String) curSpn.get(gsmServiceStateTracker))
                                                || !TextUtils.equals(plmn, (String) curPlmn.get(gsmServiceStateTracker))) {
                                            Log.d(TAG, String.format("GSM updateSpnDisplay: changed sending intent rule="
                                                    + rule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                                                    showPlmn, plmn, showSpn, spn));

                                            broadcastSpnUpdate(gsmServiceStateTracker, plmn, showPlmn, spn, showSpn);
                                        }

                                        curShowPlmn.setBoolean(gsmServiceStateTracker, showPlmn);
                                        curShowSpn.setBoolean(gsmServiceStateTracker, showSpn);
                                        curSpn.set(gsmServiceStateTracker, spn);
                                        curPlmn.set(gsmServiceStateTracker, plmn);
                                    }
                                }
                                return null;
                            }
                        });
                    }
                } catch (Throwable throwable) {
                    Log.d(TAG, "error throwable...", throwable);
                    errorForceDisable = true;
                }
            }
        });
    }

    private static void hookCdmaServiceStateTracker() {
        Log.d(TAG, "Attempting to hook CdmaServiceStateTracker");

        MS.hookClassLoad("com.android.internal.telephony.cdma.CdmaServiceStateTracker", new MS.ClassLoadHook() {
            @Override
            public void classLoaded(Class<?> resources) {
                try {
                    Log.d(TAG, "Hooked CdmaServiceStateTracker");

                    Log.d(TAG, "Trying to hook updateSpnDisplay");
                    Method updateSpnDisplay = resources.getDeclaredMethod("updateSpnDisplay");
                    updateSpnDisplay.setAccessible(true);
                    Log.d(TAG, "hooked updateSpnDisplay");

                    if (updateSpnDisplay != null) {
                        Log.d(TAG, "hooked updateSpnDisplay");
                        final Class<?> superClazz = resources.getSuperclass();
                        final Field serviceState = findField(superClazz, "mSS", "ss");
                        final Field curPlmn = findField(resources, "mCurPlmn", "curPlmn");

                        MS.hookMethod(resources, updateSpnDisplay, new MS.MethodAlteration<Object, Void>() {
                            @Override
                            public Void invoked(Object cdmaServiceStateTracker, Object... args) throws Throwable {
                                Log.d(TAG, "CDMA updateSpnDisplay hit");

                                if (errorForceDisable) {
                                    Log.d(TAG, "tweak force disabled, running stock method");
                                    return invoke(cdmaServiceStateTracker, args);
                                } else {
                                    String plmn;
                                    Object mSS = serviceState.get(cdmaServiceStateTracker);
                                    Method getOperatorAlphaLong = findMethod(mSS.getClass(), "getOperatorAlphaLong");

                                    plmn = (String) getOperatorAlphaLong.invoke(mSS);
                                    boolean showPlmn = plmn != null;


                                    SharedPreferences preferences = Settings.getInstance().getPreferences();
                                    boolean enabled = preferences.getBoolean("tweak_enabled", false);
                                    String fakeOperator = preferences.getString("fake_operator", "NSPwn");

                                    if (enabled) {
                                        plmn = fakeOperator;
                                    }

                                    if (!TextUtils.equals(plmn, (String) curPlmn.get(cdmaServiceStateTracker))) {
                                        Log.d(TAG, String.format("CDMA updateSpnDisplay: changed sending intent showPlmn='%b' plmn='%s'", showPlmn, plmn));
                                        broadcastSpnUpdate(cdmaServiceStateTracker, plmn, showPlmn, "", false);
                                    }

                                    curPlmn.set(cdmaServiceStateTracker, plmn);
                                }

                                return null;
                            }
                        });
                    }
                } catch (NoSuchMethodException e) {
                    Log.d(TAG, "error no such method...", e);
                    errorForceDisable = true;
                } catch (Throwable throwable) {
                    Log.d(TAG, "error thrown...", throwable);
                    errorForceDisable = true;
                }
            }
        });
    }

    private static void broadcastSpnUpdate(Object serviceStateTracker, String plmn, boolean showPlmn, String spn, boolean showSpn) throws Throwable {
        Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("showSpn", showSpn);
        intent.putExtra("spn", spn);
        intent.putExtra("showPlmn", showPlmn);
        intent.putExtra("plmn", plmn);

        Object mPhone = findField(serviceStateTracker.getClass(), "mPhone", "phone").get(serviceStateTracker);
        Context phoneContext = (Context) findField(mPhone.getClass().getSuperclass(), "mContext", "context").get(mPhone);

        if (useOldHooks) {
            phoneContext.sendStickyBroadcast(intent);
        } else {
            sendStickyBroadcastAsUser.invoke(phoneContext, intent, userHandle);
        }
    }
}
