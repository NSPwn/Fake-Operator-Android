package com.nspwn.fakeoperator.tweak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.UserHandle;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;

import com.saurik.substrate.MS;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class Tweak {
    private static final String TAG = "com.nspwn.fakeoperator.tweak.Tweak";
    private static Object gsmServiceInstance = null;
    static void initialize() {
        Log.i(TAG, "tweak loaded");
        Log.i(TAG, "waiting for class load(s)...");

        hookGsmServiceStateTracker();
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

    private static Object getObjectFromField(Class<?> clazz, String name, Object instance) throws Throwable {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void hookGsmServiceStateTracker() {
        Log.d(TAG, "Attempting to hook GsmServiceStateTracker");

        MS.hookClassLoad("com.android.internal.telephony.gsm.GsmServiceStateTracker", new MS.ClassLoadHook() {
            @Override
            public void classLoaded(Class<?> resources) {
                try {
                    // Method is protected let's fix that
                    Method updateSpnDisplay = resources.getDeclaredMethod("updateSpnDisplay");
                    updateSpnDisplay.setAccessible(true);

                    Class<?> internalRString = Class.forName("com.android.internal.R$string");
                    int emergency_calls_only = 0;
                    int lockscreen_carrier_default = 0;

                    if (internalRString != null) {
                        emergency_calls_only = (Integer) getObjectFromField(internalRString, "emergency_calls_only", null);
                        lockscreen_carrier_default = (Integer) getObjectFromField(internalRString, "lockscreen_carrier_default", null);
                    }

                    if (updateSpnDisplay != null) {
                        Log.d(TAG, "hooked updateSpnDisplay");
                        final Class<?> superClazz = resources.getSuperclass();

                        final int finalLockscreen_carrier_default = lockscreen_carrier_default;
                        final int finalEmergency_calls_only = emergency_calls_only;
                        MS.hookMethod(resources, updateSpnDisplay, new MS.MethodAlteration<Object, Void>() {
                            @Override
                            public Void invoked(Object thiz, Object... args) throws Throwable {
                                Log.d(TAG, "spn display hit");
                                gsmServiceInstance = thiz;

                                Object mSS = getObjectFromField(superClazz, "mSS", thiz);
                                Object mIccRecords = getObjectFromField(superClazz, "mIccRecords", thiz);
                                String plmn = null;
                                boolean showPlmn = false;
                                int rule = 1;
                                if (mIccRecords != null) {
                                    Method getDisplayRule = mIccRecords.getClass().getDeclaredMethod("getDisplayRule", String.class);
                                    getDisplayRule.setAccessible(true);

                                    Method getOperatorNumeric = mSS.getClass().getDeclaredMethod("getOperatorNumeric");
                                    getOperatorNumeric.setAccessible(true);

                                    String operatorNumeric = (String) getOperatorNumeric.invoke(mSS);
                                    rule = (Integer) getDisplayRule.invoke(mIccRecords, operatorNumeric);
                                }

                                Method getVoiceRegState = mSS.getClass().getDeclaredMethod("getVoiceRegState");
                                getVoiceRegState.setAccessible(true);
                                int voiceRegState = (Integer) getVoiceRegState.invoke(mSS);

                                if (voiceRegState == ServiceState.STATE_OUT_OF_SERVICE || voiceRegState == ServiceState.STATE_EMERGENCY_ONLY) {
                                    boolean mEmergencyOnly = (Boolean) getObjectFromField(thiz.getClass(), "mEmergencyOnly", thiz);
                                    showPlmn = true;

                                    if (mEmergencyOnly) {
                                        plmn = Resources.getSystem().getText(finalEmergency_calls_only).toString();
                                    } else {
                                        plmn = Resources.getSystem().getText(finalLockscreen_carrier_default).toString();
                                    }

                                    Log.d(TAG, "updateSpnDisplay: radio is on but out of service, set plmn='" + plmn + "'");
                                } else if (voiceRegState == ServiceState.STATE_IN_SERVICE) {
                                    SharedPreferences preferences = Settings.getInstance().getPreferences();
                                    boolean enabled = preferences.getBoolean("tweak_enabled", false);
                                    String fakeOperator = preferences.getString("fake_operator", "NSPwn");

                                    if (enabled) {
                                        plmn = fakeOperator;
                                    } else {
                                        Method getOperatorAlphaLong = mSS.getClass().getMethod("getOperatorAlphaLong");
                                        getOperatorAlphaLong.setAccessible(true);

                                        plmn = (String) getOperatorAlphaLong.invoke(mSS);
                                    }

                                    int spnRuleShowPlmn = 0x02;
                                    showPlmn = !TextUtils.isEmpty(plmn) &&
                                            ((rule & spnRuleShowPlmn) == spnRuleShowPlmn);
                                } else {
                                    Log.d(TAG, "updateSpnDisplay: radio is off with showPlmn=" + showPlmn + " plmn=" + plmn);
                                }

                                int spnRuleShowSpn = 0x01;

                                Method getServiceProviderName = mIccRecords.getClass().getMethod("getServiceProviderName");
                                getServiceProviderName.setAccessible(true);


                                String spn = (mIccRecords != null) ? (String) getServiceProviderName.invoke(mIccRecords) : "";
                                boolean showSpn = !TextUtils.isEmpty(spn)
                                        && ((rule & spnRuleShowSpn) == spnRuleShowSpn);

                                Field mCurShowSpn = thiz.getClass().getDeclaredField("mCurShowSpn");
                                mCurShowSpn.setAccessible(true);
                                Field mCurShowPlmn = thiz.getClass().getDeclaredField("mCurShowPlmn");
                                mCurShowPlmn.setAccessible(true);
                                Field mCurSpn = thiz.getClass().getDeclaredField("mCurSpn");
                                mCurSpn.setAccessible(true);
                                Field mCurPlmn = thiz.getClass().getDeclaredField("mCurPlmn");
                                mCurPlmn.setAccessible(true);

                                if (showPlmn != mCurShowPlmn.getBoolean(thiz)
                                        || showSpn != mCurShowSpn.getBoolean(thiz)
                                        || !TextUtils.equals(spn, (String) mCurSpn.get(thiz))
                                        || !TextUtils.equals(plmn, (String) mCurPlmn.get(thiz))) {
                                    Log.d(TAG, String.format("updateSpnDisplay: changed sending intent rule="
                                            + rule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                                            showPlmn, plmn, showSpn, spn));

                                    Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                                    intent.putExtra("showSpn", showSpn);
                                    intent.putExtra("spn", spn);
                                    intent.putExtra("showPlmn", showPlmn);
                                    intent.putExtra("plmn", plmn);

                                    Object mPhone = getObjectFromField(thiz.getClass(), "mPhone", thiz);
                                    Context phoneContext = (Context) getObjectFromField(mPhone.getClass().getSuperclass(), "mContext", mPhone);
                                    UserHandle userHandle = (UserHandle) getObjectFromField(UserHandle.class, "ALL", null);
                                    phoneContext.sendStickyBroadcastAsUser(intent, userHandle);
                                }

                                mCurShowPlmn.setBoolean(thiz, showPlmn);
                                mCurShowSpn.setBoolean(thiz, showSpn);
                                mCurSpn.set(thiz, spn);
                                mCurPlmn.set(thiz, plmn);

                                return null;
                            }
                        });
                    }
                } catch (NoSuchMethodException me) {
                    Log.e(TAG, "error hooking method...", me);
                } catch (ClassNotFoundException e) {
                    Log.d(TAG, "error finding class...", e);
                } catch (Throwable throwable) {
                    Log.d(TAG, "error throwable...", throwable);
                }
            }
        });
    }
}
