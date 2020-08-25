package jcifs.smb;

class Trans2SetFileInformationResponse extends SmbComTransactionResponse {
    Trans2SetFileInformationResponse() {
        this.subCommand = 8;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int writeSetupWireFormat(byte[] dst, int dstIndex) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int writeParametersWireFormat(byte[] dst, int dstIndex) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int writeDataWireFormat(byte[] dst, int dstIndex) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int readSetupWireFormat(byte[] buffer, int bufferIndex, int len) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int readParametersWireFormat(byte[] buffer, int bufferIndex, int len) {
        return 0;
    }

    /* access modifiers changed from: package-private */
    @Override // jcifs.smb.SmbComTransactionResponse
    public int readDataWireFormat(byte[] buffer, int bufferIndex, int len) {
        return 0;
    }

    @Override // jcifs.smb.ServerMessageBlock, jcifs.smb.SmbComTransactionResponse
    public String toString() {
        return new String("Trans2SetFileInformationResponse[" + super.toString() + "]");
    }
}
