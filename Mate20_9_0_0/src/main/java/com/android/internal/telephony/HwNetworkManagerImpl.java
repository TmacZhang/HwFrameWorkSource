package com.android.internal.telephony;

import android.content.ContentResolver;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import huawei.cust.HwCfgFilePolicy;

public class HwNetworkManagerImpl implements HwNetworkManager {
    private static final boolean DBG = true;
    private static final int DEFAULT_DELAY_POWER_OFF_MISEC = 2000;
    private static final String HWNFF_RSSI_SIM1 = "gsm.rssi.sim1";
    private static final String HWNFF_RSSI_SIM2 = "gsm.rssi.sim2";
    private static final int MAX_DELAY_POWER_OFF_MISEC = 6000;
    private static final String TAG = "HwNetworkManagerImpl";
    private static HwNetworkManager mInstance = new HwNetworkManagerImpl();
    private int hwnff_value_sim1 = 0;
    private int hwnff_value_sim2 = 0;

    private static class SaveThread extends Thread {
        ContentResolver mCr;
        String mMcc;
        String mTimeZoneId;

        public SaveThread(ContentResolver cr, String mcc, String timeZoneId) {
            this.mCr = cr;
            this.mMcc = mcc;
            this.mTimeZoneId = timeZoneId;
        }

        /* JADX WARNING: Missing block: B:24:0x005e, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            super.run();
            synchronized (SaveThread.class) {
                if (!(this.mCr == null || this.mMcc == null)) {
                    if (this.mTimeZoneId != null) {
                        String saveStr = new StringBuilder();
                        saveStr.append(this.mTimeZoneId);
                        saveStr.append("||");
                        saveStr.append(this.mMcc);
                        saveStr.append("||");
                        saveStr.append(System.currentTimeMillis());
                        try {
                            System.putString(this.mCr, "nitz_timezone_info", saveStr.toString());
                            String timezoneId = System.getString(this.mCr, "keyguard_default_time_zone");
                            if (timezoneId == null || timezoneId.length() == 0) {
                                System.putString(this.mCr, "keyguard_default_time_zone", this.mTimeZoneId);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    public static HwNetworkManager getDefault() {
        return mInstance;
    }

    public String getCdmaPlmn(ServiceStateTracker stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwCdmaServiceStateManager(stateTracker, phone).getPlmn();
    }

    public String getCdmaRplmn(ServiceStateTracker stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwCdmaServiceStateManager(stateTracker, phone).getRplmn();
    }

    public String getGsmPlmn(Object stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getPlmn();
    }

    public String getGsmRplmn(Object stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getRplmn();
    }

    public boolean getGsmRoamingState(Object stateTracker, GsmCdmaPhone phone, boolean roaming) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getRoamingStateHw(roaming);
    }

    public OnsDisplayParams getGsmOnsDisplayParams(Object stateTracker, GsmCdmaPhone phone, boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getOnsDisplayParamsHw(showSpn, showPlmn, rule, plmn, spn);
    }

    public void sendGsmDualSimUpdateSpnIntent(Object stateTracker, GsmCdmaPhone phone, boolean showSpn, String spn, boolean showPlmn, String plmn) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).sendDualSimUpdateSpnIntent(showSpn, spn, showPlmn, plmn);
    }

    public OnsDisplayParams getCdmaOnsDisplayParams(Object stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).getOnsDisplayParamsHw(false, false, 0, null, null);
    }

    public void sendCdmaDualSimUpdateSpnIntent(ServiceStateTracker stateTracker, GsmCdmaPhone phone, boolean showSpn, String spn, boolean showPlmn, String plmn) {
        HwServiceStateManager.getHwCdmaServiceStateManager(stateTracker, phone).sendDualSimUpdateSpnIntent(showSpn, spn, showPlmn, plmn);
    }

    public void delaySendDetachAfterDataOff() {
    }

    public void delaySendDetachAfterDataOff(GsmCdmaPhone phone) {
        String str;
        boolean detachSign = false;
        int delay_milsec = SystemProperties.getInt("ro.config.hw_delay_detach_time", 0) * 1000;
        try {
            int slotId = SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId());
            Integer mdelay_time = (Integer) HwCfgFilePolicy.getValue("hw_delay_detach_time", slotId, Integer.class);
            Boolean mdetachSign = (Boolean) HwCfgFilePolicy.getValue("delay_detach_switch", slotId, Boolean.class);
            if (mdelay_time != null) {
                delay_milsec = 1000 * mdelay_time.intValue();
            }
            if (mdetachSign != null) {
                detachSign = mdetachSign.booleanValue();
            }
            if (delay_milsec >= MAX_DELAY_POWER_OFF_MISEC) {
                delay_milsec = 2000;
            }
        } catch (Exception e) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: read delay_detach_switch error: ");
            stringBuilder.append(e.toString());
            stringBuilder.append(" ! ");
            Rlog.e(str, stringBuilder.toString());
        }
        if (!detachSign || delay_milsec <= 0) {
            str = "";
            str = System.getString(phone.getContext().getContentResolver(), "hw_delay_imsi_detach_plmn");
            if (TextUtils.isEmpty(str)) {
                str = SystemProperties.get("ro.config.hw_delay_detach_plmn", "");
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" delay_plmn = ");
            stringBuilder2.append(str);
            Rlog.d(str2, stringBuilder2.toString());
            String str3;
            StringBuilder stringBuilder3;
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                str2 = TelephonyManager.getDefault().getSimOperator(SubscriptionManager.getDefaultDataSubscriptionId());
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isMultiSimEnabled + sim_plmn = ");
                stringBuilder3.append(str2);
                Rlog.d(str3, stringBuilder3.toString());
            } else {
                str2 = SystemProperties.get("gsm.sim.operator.numeric", "");
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" not isMultiSimEnabled + sim_plmn = ");
                stringBuilder3.append(str2);
                Rlog.d(str3, stringBuilder3.toString());
            }
            String str4;
            if ("".equals(str) || "".equals(str2)) {
                str4 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Process pending request to turn radio off; delay_plmn:");
                stringBuilder2.append(str);
                stringBuilder2.append(" sim_plmn:");
                stringBuilder2.append(str2);
                Rlog.d(str4, stringBuilder2.toString());
            } else if (-1 == str.indexOf(str2) || delay_milsec <= 0) {
                Rlog.d(TAG, "Process pending request to turn radio off");
            } else {
                if (delay_milsec >= MAX_DELAY_POWER_OFF_MISEC) {
                    delay_milsec = 2000;
                }
                str4 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Process pending request to turn radio off ");
                stringBuilder2.append(delay_milsec);
                stringBuilder2.append(" milliseconds later.");
                Rlog.d(str4, stringBuilder2.toString());
                SystemClock.sleep((long) delay_milsec);
                str4 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(delay_milsec);
                stringBuilder2.append(" milliseconds past, Process pending request to turn radio off");
                Rlog.d(str4, stringBuilder2.toString());
            }
            return;
        }
        String str5 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("HwCfgFile: Process pending request to turn radio off ");
        stringBuilder4.append(delay_milsec);
        stringBuilder4.append(" milliseconds later.");
        Rlog.d(str5, stringBuilder4.toString());
        SystemClock.sleep((long) delay_milsec);
    }

    public void setAutoTimeAndZoneForCdma(ServiceStateTracker stateTracker, GsmCdmaPhone phone, int rt) {
        HwServiceStateManager.getHwCdmaServiceStateManager(stateTracker, phone).setAutoTimeAndZoneForCdma(rt);
    }

    public void saveNitzTimeZoneToDB(ContentResolver cr, String timeZoneId) {
        String mccMnc = SystemProperties.get("gsm.operator.numeric");
        if (mccMnc != null && mccMnc.length() > 3) {
            new SaveThread(cr, mccMnc.substring(null, 3), timeZoneId).start();
        }
    }

    public void dispose(ServiceStateTracker serviceStateTracker) {
        HwServiceStateManager.dispose(serviceStateTracker);
    }

    public int getGsmCombinedRegState(Object stateTracker, GsmCdmaPhone phone, ServiceState serviceState) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getCombinedRegState(serviceState);
    }

    public int getCdmaCombinedRegState(Object stateTracker, GsmCdmaPhone phone, ServiceState serviceState) {
        return HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).getCombinedRegState(serviceState);
    }

    public boolean needGsmUpdateNITZTime(Object stateTracker, GsmCdmaPhone phone) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).needUpdateNITZTime();
    }

    public void processCdmaCTNumMatch(Object stateTracker, GsmCdmaPhone phone, boolean roaming, UiccCardApplication uiccCardApplication) {
        HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).processCTNumMatch(roaming, uiccCardApplication);
    }

    public void processGsmCTNumMatch(Object stateTracker, GsmCdmaPhone phone, boolean roaming, UiccCardApplication uiccCardApplication) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).processCTNumMatch(roaming, uiccCardApplication);
    }

    public boolean updateCTRoaming(Object stateTracker, GsmCdmaPhone phone, ServiceState newSS, boolean cdmaRoaming) {
        return HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).updateCTRoaming(newSS, cdmaRoaming);
    }

    public boolean notifyGsmSignalStrength(Object stateTracker, GsmCdmaPhone phone, SignalStrength oldSS, SignalStrength newSS) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).notifySignalStrength(oldSS, newSS);
    }

    public boolean notifyCdmaSignalStrength(Object stateTracker, GsmCdmaPhone phone, SignalStrength oldSS, SignalStrength newSS) {
        return HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).notifySignalStrength(oldSS, newSS);
    }

    public int getCARilRadioType(Object stateTracker, GsmCdmaPhone phone, int type) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).getCARilRadioType(type);
    }

    public int updateCAStatus(Object stateTracker, GsmCdmaPhone phone, int currentType) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).updateCAStatus(currentType);
    }

    public void unregisterForSimRecordsEvents(Object stateTracker, GsmCdmaPhone phone, IccRecords r) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).unregisterForRecordsEvents(r);
    }

    public void registerForSimRecordsEvents(Object stateTracker, GsmCdmaPhone phone, IccRecords r) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).registerForRecordsEvents(r);
    }

    public boolean isCustScreenOff(GsmCdmaPhone phoneBase) {
        return HwServiceStateManager.isCustScreenOff(phoneBase);
    }

    public boolean proccessGsmDelayUpdateRegisterStateDone(Object stateTracker, GsmCdmaPhone phone, ServiceState oldSS, ServiceState newSS) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).proccessGsmDelayUpdateRegisterStateDone(oldSS, newSS);
    }

    public boolean proccessCdmaLteDelayUpdateRegisterStateDone(Object stateTracker, GsmCdmaPhone phone, ServiceState oldSS, ServiceState newSS) {
        return HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).proccessCdmaLteDelayUpdateRegisterStateDone(oldSS, newSS);
    }

    public void setGsmOOSFlag(Object stateTracker, GsmCdmaPhone phone, boolean flag) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).setOOSFlag(flag);
    }

    public void setCdmaOOSFlag(Object stateTracker, GsmCdmaPhone phone, boolean flag) {
        HwServiceStateManager.getHwCdmaServiceStateManager((ServiceStateTracker) stateTracker, phone).setOOSFlag(flag);
    }

    public boolean isUpdateLacAndCid(Object stateTracker, GsmCdmaPhone phone, int cid) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).isUpdateLacAndCid(cid);
    }

    public void sendGsmRoamingIntentIfDenied(Object stateTracker, GsmCdmaPhone phone, int regState, int rejectCode) {
        HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).sendGsmRoamingIntentIfDenied(regState, rejectCode);
    }

    public void updateHwnff(ServiceStateTracker stateTracker, SignalStrength newSS) {
        int subId = stateTracker.getPhone().getSubId();
        int simCardState = TelephonyManager.getDefault().getSimState(subId);
        if (simCardState != 1 && simCardState != 0) {
            StringBuilder stringBuilder;
            if (subId == 0) {
                try {
                    if (this.hwnff_value_sim1 != newSS.getDbm()) {
                        this.hwnff_value_sim1 = newSS.getDbm();
                        SystemProperties.set(HWNFF_RSSI_SIM1, Integer.toString(this.hwnff_value_sim1));
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("update hwnff Sub0 to ");
                        stringBuilder.append(this.hwnff_value_sim1);
                        Rlog.d(str, stringBuilder.toString());
                        return;
                    }
                } catch (RuntimeException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("write hwnff Sub");
                    stringBuilder2.append(subId);
                    stringBuilder2.append(" prop failed ");
                    Rlog.e(str2, stringBuilder2.toString());
                    return;
                }
            }
            if (1 == subId && this.hwnff_value_sim2 != newSS.getDbm()) {
                this.hwnff_value_sim2 = newSS.getDbm();
                SystemProperties.set(HWNFF_RSSI_SIM2, Integer.toString(this.hwnff_value_sim2));
                RuntimeException ex = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("update hwnff Sub1 to ");
                stringBuilder.append(this.hwnff_value_sim2);
                Rlog.d(ex, stringBuilder.toString());
            }
        }
    }

    public void setPreferredNetworkTypeSafely(Phone phoneBase, ServiceStateTracker stateTracker, int networkType, Message response) {
        HwServiceStateManager.getHwServiceStateManager(stateTracker, phoneBase).setPreferredNetworkTypeSafely(phoneBase, networkType, response);
    }

    public void getLocationInfo(ServiceStateTracker stateTracker, GsmCdmaPhone phone) {
        HwServiceStateManager.getHwGsmServiceStateManager(stateTracker, phone).getLocationInfo();
    }

    public void checkAndSetNetworkType(ServiceStateTracker stateTracker, Phone phoneBase) {
        HwServiceStateManager.getHwServiceStateManager(stateTracker, phoneBase).checkAndSetNetworkType();
    }

    public void countPackageUseCellInfo(ServiceStateTracker stateTracker, Phone phoneBase, String packageName) {
        HwServiceStateManager.getHwServiceStateManager(stateTracker, phoneBase).countPackageUseCellInfo(packageName);
    }

    public boolean isNetworkModeAsynchronized(Phone sPhone) {
        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
        if (sHwDataConnectionManager == null || !sHwDataConnectionManager.getNamSwitcherForSoftbank() || !sHwDataConnectionManager.isSoftBankCard(sPhone) || sHwDataConnectionManager.isValidMsisdn(sPhone)) {
            return false;
        }
        Rlog.d(TAG, "no msisdn softbank card");
        return true;
    }

    public void setPreferredNetworkTypeForNoMdn(Phone sPhone, int settingMode) {
        if (isNetworkModeEnableLTE(settingMode)) {
            sPhone.setPreferredNetworkType(3, null);
        } else {
            sPhone.setPreferredNetworkType(settingMode, null);
        }
    }

    public void setPreferredNetworkTypeForLoaded(Phone sPhone, int settingMode) {
        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
        if (sHwDataConnectionManager == null || !sHwDataConnectionManager.isSoftBankCard(sPhone) || sHwDataConnectionManager.isValidMsisdn(sPhone)) {
            sPhone.setPreferredNetworkType(settingMode, null);
        } else {
            setPreferredNetworkTypeForNoMdn(sPhone, settingMode);
        }
    }

    private boolean isNetworkModeEnableLTE(int iNetworkMode) {
        return iNetworkMode == 9 || iNetworkMode == 11 || iNetworkMode == 12 || iNetworkMode == 8 || iNetworkMode == 10;
    }

    public void factoryResetNetworkTypeForNoMdn(Phone sPhone) {
        setPreferredNetworkTypeForNoMdn(sPhone, Global.getInt(sPhone.getContext().getContentResolver(), "preferred_network_mode", 9));
        Global.putInt(sPhone.getContext().getContentResolver(), "preferred_network_mode", 9);
    }

    public void handle4GSwitcherForNoMdn(Phone sPhone, int nwMode) {
        Global.putInt(sPhone.getContext().getContentResolver(), "preferred_network_mode", nwMode);
        sPhone.setPreferredNetworkType(3, null);
    }

    public void sendNitzTimeZoneUpdateMessage(CellLocation cellLoc) {
        int lac = -1;
        if (cellLoc != null && (cellLoc instanceof GsmCellLocation)) {
            lac = ((GsmCellLocation) cellLoc).getLac();
            Rlog.d(TAG, "sendNitzTimeZoneUpdateMessage");
        }
        HwLocationBasedTimeZoneUpdater hwLocTzUpdater = HwLocationBasedTimeZoneUpdater.getInstance();
        if (hwLocTzUpdater != null && hwLocTzUpdater.getHandler() != null) {
            hwLocTzUpdater.getHandler().sendMessage(hwLocTzUpdater.getHandler().obtainMessage(1, Integer.valueOf(lac)));
        }
    }

    public int updateHSPAStatus(Object stateTracker, GsmCdmaPhone phone, int currentType) {
        return HwServiceStateManager.getHwGsmServiceStateManager((ServiceStateTracker) stateTracker, phone).updateHSPAStatus(currentType, phone);
    }

    public boolean isCellRequestStrategyPassed(ServiceStateTracker stateTracker, WorkSource workSource, GsmCdmaPhone phoneBase) {
        return HwServiceStateManager.getHwServiceStateManager(stateTracker, phoneBase).isCellRequestStrategyPassed(stateTracker, workSource, phoneBase);
    }

    public boolean isNeedLocationTimeZoneUpdate(Phone phone, String zoneId) {
        return HwDualCardsLocationTimeZoneUpdate.getDefault().isNeedLocationTimeZoneUpdate(phone, zoneId);
    }

    public String retToStringEx(int req, Object ret) {
        return HwResponseInfoToString.getDefault().retToStringEx(req, ret);
    }
}
