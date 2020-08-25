package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Vector;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.tls.TlsProtocol;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.Arrays;

public class TlsServerProtocol extends TlsProtocol {
    protected CertificateRequest certificateRequest = null;
    protected short clientCertificateType = -1;
    protected TlsKeyExchange keyExchange = null;
    protected TlsHandshakeHash prepareFinishHash = null;
    protected TlsCredentials serverCredentials = null;
    protected TlsServer tlsServer = null;
    TlsServerContextImpl tlsServerContext = null;

    public TlsServerProtocol(InputStream inputStream, OutputStream outputStream, SecureRandom secureRandom) {
        super(inputStream, outputStream, secureRandom);
    }

    public TlsServerProtocol(SecureRandom secureRandom) {
        super(secureRandom);
    }

    public void accept(TlsServer tlsServer2) throws IOException {
        if (tlsServer2 == null) {
            throw new IllegalArgumentException("'tlsServer' cannot be null");
        } else if (this.tlsServer == null) {
            this.tlsServer = tlsServer2;
            this.securityParameters = new SecurityParameters();
            this.securityParameters.entity = 0;
            this.tlsServerContext = new TlsServerContextImpl(this.secureRandom, this.securityParameters);
            this.securityParameters.serverRandom = createRandomBlock(tlsServer2.shouldUseGMTUnixTime(), this.tlsServerContext.getNonceRandomGenerator());
            this.tlsServer.init(this.tlsServerContext);
            this.recordStream.init(this.tlsServerContext);
            tlsServer2.notifyCloseHandle(this);
            this.recordStream.setRestrictReadVersion(false);
            blockForHandshake();
        } else {
            throw new IllegalStateException("'accept' can only be called once");
        }
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public void cleanupHandshake() {
        super.cleanupHandshake();
        this.keyExchange = null;
        this.serverCredentials = null;
        this.certificateRequest = null;
        this.prepareFinishHash = null;
    }

    /* access modifiers changed from: protected */
    public boolean expectCertificateVerifyMessage() {
        short s = this.clientCertificateType;
        return s >= 0 && TlsUtils.hasSigningCapability(s);
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public TlsContext getContext() {
        return this.tlsServerContext;
    }

    /* access modifiers changed from: package-private */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public AbstractTlsContext getContextAdmin() {
        return this.tlsServerContext;
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public TlsPeer getPeer() {
        return this.tlsServer;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0020, code lost:
        if (r3 == 9) goto L_0x0029;
     */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public void handleAlertWarningMessage(short s) throws IOException {
        super.handleAlertWarningMessage(s);
        if (s == 41) {
            if (TlsUtils.isSSL(getContext()) && this.certificateRequest != null) {
                short s2 = this.connection_state;
                if (s2 == 8) {
                    this.tlsServer.processClientSupplementalData(null);
                }
                notifyClientCertificate(Certificate.EMPTY_CHAIN);
                this.connection_state = 10;
                return;
            }
            throw new TlsFatalAlert(10);
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0034  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003a  */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public void handleHandshakeMessage(short s, ByteArrayInputStream byteArrayInputStream) throws IOException {
        CertificateStatus certificateStatus;
        Certificate certificate = null;
        if (s == 1) {
            short s2 = this.connection_state;
            if (s2 == 0) {
                receiveClientHelloMessage(byteArrayInputStream);
                this.connection_state = 1;
                sendServerHelloMessage();
                this.connection_state = 2;
                this.recordStream.notifyHelloComplete();
                Vector serverSupplementalData = this.tlsServer.getServerSupplementalData();
                if (serverSupplementalData != null) {
                    sendSupplementalDataMessage(serverSupplementalData);
                }
                this.connection_state = 3;
                this.keyExchange = this.tlsServer.getKeyExchange();
                this.keyExchange.init(getContext());
                this.serverCredentials = this.tlsServer.getCredentials();
                TlsCredentials tlsCredentials = this.serverCredentials;
                if (tlsCredentials == null) {
                    this.keyExchange.skipServerCredentials();
                } else {
                    this.keyExchange.processServerCredentials(tlsCredentials);
                    certificate = this.serverCredentials.getCertificate();
                    sendCertificateMessage(certificate);
                }
                this.connection_state = 4;
                boolean z = false;
                if (certificate == null || certificate.isEmpty()) {
                    this.allowCertificateStatus = false;
                }
                if (this.allowCertificateStatus && (certificateStatus = this.tlsServer.getCertificateStatus()) != null) {
                    sendCertificateStatusMessage(certificateStatus);
                }
                this.connection_state = 5;
                byte[] generateServerKeyExchange = this.keyExchange.generateServerKeyExchange();
                if (generateServerKeyExchange != null) {
                    sendServerKeyExchangeMessage(generateServerKeyExchange);
                }
                this.connection_state = 6;
                if (this.serverCredentials != null) {
                    this.certificateRequest = this.tlsServer.getCertificateRequest();
                    if (this.certificateRequest != null) {
                        boolean isTLSv12 = TlsUtils.isTLSv12(getContext());
                        if (this.certificateRequest.getSupportedSignatureAlgorithms() != null) {
                            z = true;
                        }
                        if (isTLSv12 == z) {
                            this.keyExchange.validateCertificateRequest(this.certificateRequest);
                            sendCertificateRequestMessage(this.certificateRequest);
                            TlsUtils.trackHashAlgorithms(this.recordStream.getHandshakeHash(), this.certificateRequest.getSupportedSignatureAlgorithms());
                        } else {
                            throw new TlsFatalAlert(80);
                        }
                    }
                }
                this.connection_state = 7;
                sendServerHelloDoneMessage();
                this.connection_state = 8;
                this.recordStream.getHandshakeHash().sealHashAlgorithms();
            } else if (s2 == 16) {
                refuseRenegotiation();
            } else {
                throw new TlsFatalAlert(10);
            }
        } else if (s == 11) {
            short s3 = this.connection_state;
            if (s3 == 8) {
                this.tlsServer.processClientSupplementalData(null);
            } else if (s3 != 9) {
                throw new TlsFatalAlert(10);
            }
            if (this.certificateRequest != null) {
                receiveCertificateMessage(byteArrayInputStream);
                this.connection_state = 10;
                return;
            }
            throw new TlsFatalAlert(10);
        } else if (s == 20) {
            short s4 = this.connection_state;
            if (s4 != 11) {
                if (s4 != 12) {
                    throw new TlsFatalAlert(10);
                }
            } else if (expectCertificateVerifyMessage()) {
                throw new TlsFatalAlert(10);
            }
            processFinishedMessage(byteArrayInputStream);
            this.connection_state = 13;
            if (this.expectSessionTicket) {
                sendNewSessionTicketMessage(this.tlsServer.getNewSessionTicket());
            }
            this.connection_state = 14;
            sendChangeCipherSpecMessage();
            sendFinishedMessage();
            this.connection_state = 15;
            completeHandshake();
        } else if (s != 23) {
            if (s != 15) {
                if (s == 16) {
                    switch (this.connection_state) {
                        case 8:
                            this.tlsServer.processClientSupplementalData(null);
                            if (this.certificateRequest != null) {
                                this.keyExchange.skipClientCredentials();
                            } else if (TlsUtils.isTLSv12(getContext())) {
                                throw new TlsFatalAlert(10);
                            } else if (!TlsUtils.isSSL(getContext())) {
                                notifyClientCertificate(Certificate.EMPTY_CHAIN);
                            } else if (this.peerCertificate == null) {
                                throw new TlsFatalAlert(10);
                            }
                            receiveClientKeyExchangeMessage(byteArrayInputStream);
                            this.connection_state = 11;
                            return;
                        case 9:
                            if (this.certificateRequest != null) {
                            }
                            receiveClientKeyExchangeMessage(byteArrayInputStream);
                            this.connection_state = 11;
                            return;
                        case 10:
                            receiveClientKeyExchangeMessage(byteArrayInputStream);
                            this.connection_state = 11;
                            return;
                        default:
                            throw new TlsFatalAlert(10);
                    }
                } else {
                    throw new TlsFatalAlert(10);
                }
            } else if (this.connection_state != 11) {
                throw new TlsFatalAlert(10);
            } else if (expectCertificateVerifyMessage()) {
                receiveCertificateVerifyMessage(byteArrayInputStream);
                this.connection_state = 12;
            } else {
                throw new TlsFatalAlert(10);
            }
        } else if (this.connection_state == 8) {
            this.tlsServer.processClientSupplementalData(readSupplementalDataMessage(byteArrayInputStream));
            this.connection_state = 9;
        } else {
            throw new TlsFatalAlert(10);
        }
    }

    /* access modifiers changed from: protected */
    public void notifyClientCertificate(Certificate certificate) throws IOException {
        if (this.certificateRequest == null) {
            throw new IllegalStateException();
        } else if (this.peerCertificate == null) {
            this.peerCertificate = certificate;
            if (certificate.isEmpty()) {
                this.keyExchange.skipClientCredentials();
            } else {
                this.clientCertificateType = TlsUtils.getClientCertificateType(certificate, this.serverCredentials.getCertificate());
                this.keyExchange.processClientCertificate(certificate);
            }
            this.tlsServer.notifyClientCertificate(certificate);
        } else {
            throw new TlsFatalAlert(10);
        }
    }

    /* access modifiers changed from: protected */
    public void receiveCertificateMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        Certificate parse = Certificate.parse(byteArrayInputStream);
        assertEmpty(byteArrayInputStream);
        notifyClientCertificate(parse);
    }

    /* access modifiers changed from: protected */
    public void receiveCertificateVerifyMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        byte[] bArr;
        if (this.certificateRequest != null) {
            DigitallySigned parse = DigitallySigned.parse(getContext(), byteArrayInputStream);
            assertEmpty(byteArrayInputStream);
            try {
                SignatureAndHashAlgorithm algorithm = parse.getAlgorithm();
                if (TlsUtils.isTLSv12(getContext())) {
                    TlsUtils.verifySupportedSignatureAlgorithm(this.certificateRequest.getSupportedSignatureAlgorithms(), algorithm);
                    bArr = this.prepareFinishHash.getFinalHash(algorithm.getHash());
                } else {
                    bArr = this.securityParameters.getSessionHash();
                }
                AsymmetricKeyParameter createKey = PublicKeyFactory.createKey(this.peerCertificate.getCertificateAt(0).getSubjectPublicKeyInfo());
                TlsSigner createTlsSigner = TlsUtils.createTlsSigner(this.clientCertificateType);
                createTlsSigner.init(getContext());
                if (!createTlsSigner.verifyRawSignature(algorithm, parse.getSignature(), createKey, bArr)) {
                    throw new TlsFatalAlert(51);
                }
            } catch (TlsFatalAlert e) {
                throw e;
            } catch (Exception e2) {
                throw new TlsFatalAlert(51, e2);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    /* access modifiers changed from: protected */
    public void receiveClientHelloMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        ProtocolVersion readVersion = TlsUtils.readVersion(byteArrayInputStream);
        this.recordStream.setWriteVersion(readVersion);
        if (!readVersion.isDTLS()) {
            byte[] readFully = TlsUtils.readFully(32, byteArrayInputStream);
            if (TlsUtils.readOpaque8(byteArrayInputStream).length <= 32) {
                int readUint16 = TlsUtils.readUint16(byteArrayInputStream);
                if (readUint16 < 2 || (readUint16 & 1) != 0) {
                    throw new TlsFatalAlert(50);
                }
                this.offeredCipherSuites = TlsUtils.readUint16Array(readUint16 / 2, byteArrayInputStream);
                short readUint8 = TlsUtils.readUint8(byteArrayInputStream);
                if (readUint8 >= 1) {
                    this.offeredCompressionMethods = TlsUtils.readUint8Array(readUint8, byteArrayInputStream);
                    this.clientExtensions = readExtensions(byteArrayInputStream);
                    this.securityParameters.extendedMasterSecret = TlsExtensionsUtils.hasExtendedMasterSecretExtension(this.clientExtensions);
                    if (this.securityParameters.isExtendedMasterSecret() || !this.tlsServer.requiresExtendedMasterSecret()) {
                        getContextAdmin().setClientVersion(readVersion);
                        this.tlsServer.notifyClientVersion(readVersion);
                        this.tlsServer.notifyFallback(Arrays.contains(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV));
                        this.securityParameters.clientRandom = readFully;
                        this.tlsServer.notifyOfferedCipherSuites(this.offeredCipherSuites);
                        this.tlsServer.notifyOfferedCompressionMethods(this.offeredCompressionMethods);
                        if (Arrays.contains(this.offeredCipherSuites, 255)) {
                            this.secure_renegotiation = true;
                        }
                        byte[] extensionData = TlsUtils.getExtensionData(this.clientExtensions, EXT_RenegotiationInfo);
                        if (extensionData != null) {
                            this.secure_renegotiation = true;
                            if (!Arrays.constantTimeAreEqual(extensionData, createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
                                throw new TlsFatalAlert(40);
                            }
                        }
                        this.tlsServer.notifySecureRenegotiation(this.secure_renegotiation);
                        if (this.clientExtensions != null) {
                            TlsExtensionsUtils.getPaddingExtension(this.clientExtensions);
                            this.tlsServer.processClientExtensions(this.clientExtensions);
                            return;
                        }
                        return;
                    }
                    throw new TlsFatalAlert(40);
                }
                throw new TlsFatalAlert(47);
            }
            throw new TlsFatalAlert(47);
        }
        throw new TlsFatalAlert(47);
    }

    /* access modifiers changed from: protected */
    public void receiveClientKeyExchangeMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        this.keyExchange.processClientKeyExchange(byteArrayInputStream);
        assertEmpty(byteArrayInputStream);
        if (TlsUtils.isSSL(getContext())) {
            establishMasterSecret(getContext(), this.keyExchange);
        }
        this.prepareFinishHash = this.recordStream.prepareToFinish();
        this.securityParameters.sessionHash = getCurrentPRFHash(getContext(), this.prepareFinishHash, null);
        if (!TlsUtils.isSSL(getContext())) {
            establishMasterSecret(getContext(), this.keyExchange);
        }
        this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
    }

    /* access modifiers changed from: protected */
    public void sendCertificateRequestMessage(CertificateRequest certificateRequest2) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(this, 13);
        certificateRequest2.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    /* access modifiers changed from: protected */
    public void sendCertificateStatusMessage(CertificateStatus certificateStatus) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(this, 22);
        certificateStatus.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    /* access modifiers changed from: protected */
    public void sendNewSessionTicketMessage(NewSessionTicket newSessionTicket) throws IOException {
        if (newSessionTicket != null) {
            HandshakeMessage handshakeMessage = new HandshakeMessage(this, 4);
            newSessionTicket.encode(handshakeMessage);
            handshakeMessage.writeToRecordStream();
            return;
        }
        throw new TlsFatalAlert(80);
    }

    /* access modifiers changed from: protected */
    public void sendServerHelloDoneMessage() throws IOException {
        byte[] bArr = new byte[4];
        TlsUtils.writeUint8((short) 14, bArr, 0);
        TlsUtils.writeUint24(0, bArr, 1);
        writeHandshakeMessage(bArr, 0, bArr.length);
    }

    /* access modifiers changed from: protected */
    public void sendServerHelloMessage() throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(this, 2);
        ProtocolVersion serverVersion = this.tlsServer.getServerVersion();
        if (serverVersion.isEqualOrEarlierVersionOf(getContext().getClientVersion())) {
            this.recordStream.setReadVersion(serverVersion);
            this.recordStream.setWriteVersion(serverVersion);
            this.recordStream.setRestrictReadVersion(true);
            getContextAdmin().setServerVersion(serverVersion);
            TlsUtils.writeVersion(serverVersion, handshakeMessage);
            handshakeMessage.write(this.securityParameters.serverRandom);
            TlsUtils.writeOpaque8(TlsUtils.EMPTY_BYTES, handshakeMessage);
            int selectedCipherSuite = this.tlsServer.getSelectedCipherSuite();
            if (!Arrays.contains(this.offeredCipherSuites, selectedCipherSuite) || selectedCipherSuite == 0 || CipherSuite.isSCSV(selectedCipherSuite) || !TlsUtils.isValidCipherSuiteForVersion(selectedCipherSuite, getContext().getServerVersion())) {
                throw new TlsFatalAlert(80);
            }
            this.securityParameters.cipherSuite = selectedCipherSuite;
            short selectedCompressionMethod = this.tlsServer.getSelectedCompressionMethod();
            if (Arrays.contains(this.offeredCompressionMethods, selectedCompressionMethod)) {
                this.securityParameters.compressionAlgorithm = selectedCompressionMethod;
                TlsUtils.writeUint16(selectedCipherSuite, handshakeMessage);
                TlsUtils.writeUint8(selectedCompressionMethod, (OutputStream) handshakeMessage);
                this.serverExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(this.tlsServer.getServerExtensions());
                boolean z = false;
                if (this.secure_renegotiation) {
                    if (TlsUtils.getExtensionData(this.serverExtensions, EXT_RenegotiationInfo) == null) {
                        this.serverExtensions.put(EXT_RenegotiationInfo, createRenegotiationInfo(TlsUtils.EMPTY_BYTES));
                    }
                }
                if (TlsUtils.isSSL(this.tlsServerContext)) {
                    this.securityParameters.extendedMasterSecret = false;
                } else if (this.securityParameters.isExtendedMasterSecret()) {
                    TlsExtensionsUtils.addExtendedMasterSecretExtension(this.serverExtensions);
                }
                if (!this.serverExtensions.isEmpty()) {
                    this.securityParameters.encryptThenMAC = TlsExtensionsUtils.hasEncryptThenMACExtension(this.serverExtensions);
                    this.securityParameters.maxFragmentLength = processMaxFragmentLengthExtension(this.clientExtensions, this.serverExtensions, 80);
                    this.securityParameters.truncatedHMac = TlsExtensionsUtils.hasTruncatedHMacExtension(this.serverExtensions);
                    this.allowCertificateStatus = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(this.serverExtensions, TlsExtensionsUtils.EXT_status_request, 80);
                    if (!this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(this.serverExtensions, TlsProtocol.EXT_SessionTicket, 80)) {
                        z = true;
                    }
                    this.expectSessionTicket = z;
                    writeExtensions(handshakeMessage, this.serverExtensions);
                }
                this.securityParameters.prfAlgorithm = getPRFAlgorithm(getContext(), this.securityParameters.getCipherSuite());
                this.securityParameters.verifyDataLength = 12;
                applyMaxFragmentLengthExtension();
                handshakeMessage.writeToRecordStream();
                return;
            }
            throw new TlsFatalAlert(80);
        }
        throw new TlsFatalAlert(80);
    }

    /* access modifiers changed from: protected */
    public void sendServerKeyExchangeMessage(byte[] bArr) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(12, bArr.length);
        handshakeMessage.write(bArr);
        handshakeMessage.writeToRecordStream();
    }
}
