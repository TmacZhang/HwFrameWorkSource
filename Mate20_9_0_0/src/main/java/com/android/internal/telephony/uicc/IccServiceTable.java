package com.android.internal.telephony.uicc;

import android.telephony.Rlog;

public abstract class IccServiceTable {
    protected final byte[] mServiceTable;

    protected abstract String getTag();

    protected abstract Object[] getValues();

    protected IccServiceTable(byte[] table) {
        this.mServiceTable = table;
    }

    protected boolean isAvailable(int service) {
        int offset = service / 8;
        boolean z = false;
        if (offset >= this.mServiceTable.length) {
            String tag = getTag();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isAvailable for service ");
            stringBuilder.append(service + 1);
            stringBuilder.append(" fails, max service is ");
            stringBuilder.append(this.mServiceTable.length * 8);
            Rlog.e(tag, stringBuilder.toString());
            return false;
        }
        if ((this.mServiceTable[offset] & (1 << (service % 8))) != 0) {
            z = true;
        }
        return z;
    }

    public String toString() {
        Object[] values = getValues();
        int numBytes = this.mServiceTable.length;
        StringBuilder builder = new StringBuilder(getTag());
        builder.append('[');
        builder.append(numBytes * 8);
        builder = builder.append("]={ ");
        boolean addComma = false;
        int i = 0;
        while (i < numBytes) {
            byte currentByte = this.mServiceTable[i];
            boolean addComma2 = addComma;
            for (int bit = 0; bit < 8; bit++) {
                if (((1 << bit) & currentByte) != 0) {
                    if (addComma2) {
                        builder.append(", ");
                    } else {
                        addComma2 = true;
                    }
                    int ordinal = (i * 8) + bit;
                    if (ordinal < values.length) {
                        builder.append(values[ordinal]);
                    } else {
                        builder.append('#');
                        builder.append(ordinal + 1);
                    }
                }
            }
            i++;
            addComma = addComma2;
        }
        builder.append(" }");
        return builder.toString();
    }
}
