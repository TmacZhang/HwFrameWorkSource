package com.android.server.wifi.hotspot2.anqp;

import android.net.wifi.WifiSsid;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HSOsuProvidersElement extends ANQPElement {
    @VisibleForTesting
    public static final int MAXIMUM_OSU_SSID_LENGTH = 32;
    private final WifiSsid mOsuSsid;
    private final List<OsuProviderInfo> mProviders;

    @VisibleForTesting
    public HSOsuProvidersElement(WifiSsid osuSsid, List<OsuProviderInfo> providers) {
        super(ANQPElementType.HSOSUProviders);
        this.mOsuSsid = osuSsid;
        this.mProviders = providers;
    }

    public static HSOsuProvidersElement parse(ByteBuffer payload) throws ProtocolException {
        int ssidLength = payload.get() & Constants.BYTE_MASK;
        if (ssidLength <= 32) {
            byte[] ssidBytes = new byte[ssidLength];
            payload.get(ssidBytes);
            List<OsuProviderInfo> providers = new ArrayList();
            for (int numProviders = payload.get() & Constants.BYTE_MASK; numProviders > 0; numProviders--) {
                providers.add(OsuProviderInfo.parse(payload));
            }
            return new HSOsuProvidersElement(WifiSsid.createFromByteArray(ssidBytes), providers);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid SSID length: ");
        stringBuilder.append(ssidLength);
        throw new ProtocolException(stringBuilder.toString());
    }

    public WifiSsid getOsuSsid() {
        return this.mOsuSsid;
    }

    public List<OsuProviderInfo> getProviders() {
        return Collections.unmodifiableList(this.mProviders);
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof HSOsuProvidersElement)) {
            return false;
        }
        HSOsuProvidersElement that = (HSOsuProvidersElement) thatObject;
        if (this.mOsuSsid != null ? !this.mOsuSsid.equals(that.mOsuSsid) : that.mOsuSsid != null) {
            if (this.mProviders != null) {
            }
        }
        z = false;
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mOsuSsid, this.mProviders});
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("OSUProviders{mOsuSsid=");
        stringBuilder.append(this.mOsuSsid);
        stringBuilder.append(", mProviders=");
        stringBuilder.append(this.mProviders);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
