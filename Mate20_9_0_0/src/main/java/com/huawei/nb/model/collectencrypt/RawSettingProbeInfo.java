package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawSettingProbeInfo extends AManagedObject {
    public static final Creator<RawSettingProbeInfo> CREATOR = new Creator<RawSettingProbeInfo>() {
        public RawSettingProbeInfo createFromParcel(Parcel in) {
            return new RawSettingProbeInfo(in);
        }

        public RawSettingProbeInfo[] newArray(int size) {
            return new RawSettingProbeInfo[size];
        }
    };
    private Integer mId;
    private Integer mOperateType;
    private Integer mReservedInt;
    private String mReservedText;
    private Long mSettingsStart;
    private Integer mSettingsType;
    private Date mTimeStamp;

    public RawSettingProbeInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mSettingsStart = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.mSettingsType = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.mOperateType = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        if (!cursor.isNull(6)) {
            num = Integer.valueOf(cursor.getInt(6));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(7);
    }

    public RawSettingProbeInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mSettingsStart = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.mSettingsType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mOperateType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawSettingProbeInfo(Integer mId, Date mTimeStamp, Long mSettingsStart, Integer mSettingsType, Integer mOperateType, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mSettingsStart = mSettingsStart;
        this.mSettingsType = mSettingsType;
        this.mOperateType = mOperateType;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId) {
        this.mId = mId;
        setValue();
    }

    public Date getMTimeStamp() {
        return this.mTimeStamp;
    }

    public void setMTimeStamp(Date mTimeStamp) {
        this.mTimeStamp = mTimeStamp;
        setValue();
    }

    public Long getMSettingsStart() {
        return this.mSettingsStart;
    }

    public void setMSettingsStart(Long mSettingsStart) {
        this.mSettingsStart = mSettingsStart;
        setValue();
    }

    public Integer getMSettingsType() {
        return this.mSettingsType;
    }

    public void setMSettingsType(Integer mSettingsType) {
        this.mSettingsType = mSettingsType;
        setValue();
    }

    public Integer getMOperateType() {
        return this.mOperateType;
    }

    public void setMOperateType(Integer mOperateType) {
        this.mOperateType = mOperateType;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt) {
        this.mReservedInt = mReservedInt;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText) {
        this.mReservedText = mReservedText;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mTimeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTimeStamp.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSettingsStart != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mSettingsStart.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSettingsType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mSettingsType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mOperateType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mOperateType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RawSettingProbeInfo> getHelper() {
        return RawSettingProbeInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawSettingProbeInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawSettingProbeInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mSettingsStart: ").append(this.mSettingsStart);
        sb.append(", mSettingsType: ").append(this.mSettingsType);
        sb.append(", mOperateType: ").append(this.mOperateType);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
