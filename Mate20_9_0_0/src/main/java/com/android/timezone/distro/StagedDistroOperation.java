package com.android.timezone.distro;

public class StagedDistroOperation {
    private static final StagedDistroOperation UNINSTALL_STAGED = new StagedDistroOperation(true, null);
    public final DistroVersion distroVersion;
    public final boolean isUninstall;

    private StagedDistroOperation(boolean isUninstall, DistroVersion distroVersion) {
        this.isUninstall = isUninstall;
        this.distroVersion = distroVersion;
    }

    public static StagedDistroOperation install(DistroVersion distroVersion) {
        if (distroVersion != null) {
            return new StagedDistroOperation(false, distroVersion);
        }
        throw new NullPointerException("distroVersion==null");
    }

    public static StagedDistroOperation uninstall() {
        return UNINSTALL_STAGED;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StagedDistroOperation that = (StagedDistroOperation) o;
        if (this.isUninstall != that.isUninstall) {
            return false;
        }
        if (this.distroVersion != null) {
            z = this.distroVersion.equals(that.distroVersion);
        } else if (that.distroVersion != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * this.isUninstall) + (this.distroVersion != null ? this.distroVersion.hashCode() : 0);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StagedDistroOperation{isUninstall=");
        stringBuilder.append(this.isUninstall);
        stringBuilder.append(", distroVersion=");
        stringBuilder.append(this.distroVersion);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
