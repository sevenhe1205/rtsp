package com.tuya;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * Created by heshaoqiong on 2017/7/26.
 */
public class SSlUtil {
    private static final String CERTIFICATE="certificate";
    private static final String CA_CERTIFICATE="ca-certificate";
    private static final String PRIVATE_KEY="private-key";
    static SSLContext getSSLContext(String caCrtFile, String crtFile,  String keyFile, String password)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        CertificateFactory cf = new CertificateFactory();

        // load CA certificate
        InputStream is = new FileInputStream(ResourceUtils.getFile(caCrtFile));
        X509Certificate caCert = (X509Certificate) cf.engineGenerateCertificate(is);
        is.close();

        // load client certificate
        is = new FileInputStream(ResourceUtils.getFile(crtFile));
        X509Certificate cert = (X509Certificate) cf.engineGenerateCertificate(is);
        is.close();

        // load client private key
        is = new FileInputStream(ResourceUtils.getFile(keyFile));
        KeyPair key = KeyPairGenerator.getInstance("RSA").genKeyPair();
        is.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry(CA_CERTIFICATE, caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry(CERTIFICATE, cert);
        ks.setKeyEntry(PRIVATE_KEY, key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context;
    }
}
