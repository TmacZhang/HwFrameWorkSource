package jcifs.smb;

import java.io.UnsupportedEncodingException;

class SmbComTreeConnectAndXResponse extends AndXServerMessageBlock {
    private static final int SMB_SHARE_IS_IN_DFS = 2;
    private static final int SMB_SUPPORT_SEARCH_BITS = 1;
    String nativeFileSystem = "";
    String service;
    boolean shareIsInDfs;
    boolean supportSearchBits;

    SmbComTreeConnectAndXResponse(ServerMessageBlock andx) {
        super(andx);
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.ServerMessageBlock
    public int writeParameterWordsWireFormat(byte[] dst, int dstIndex) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.ServerMessageBlock
    public int writeBytesWireFormat(byte[] dst, int dstIndex) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.ServerMessageBlock
    public int readParameterWordsWireFormat(byte[] buffer, int bufferIndex) {
        boolean z;
        boolean z2 = true;
        if ((buffer[bufferIndex] & 1) == 1) {
            z = true;
        } else {
            z = false;
        }
        this.supportSearchBits = z;
        if ((buffer[bufferIndex] & 2) != 2) {
            z2 = false;
        }
        this.shareIsInDfs = z2;
        return 2;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.ServerMessageBlock
    public int readBytesWireFormat(byte[] buffer, int bufferIndex) {
        int len = readStringLength(buffer, bufferIndex, 32);
        try {
            this.service = new String(buffer, bufferIndex, len, "ASCII");
            return (bufferIndex + (len + 1)) - bufferIndex;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

    @Override // jcifs.smb.AndXServerMessageBlock, jcifs.smb.ServerMessageBlock
    public String toString() {
        return new String("SmbComTreeConnectAndXResponse[" + super.toString() + ",supportSearchBits=" + this.supportSearchBits + ",shareIsInDfs=" + this.shareIsInDfs + ",service=" + this.service + ",nativeFileSystem=" + this.nativeFileSystem + "]");
    }
}
