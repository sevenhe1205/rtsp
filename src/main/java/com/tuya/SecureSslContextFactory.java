package com.tuya;

import io.netty.util.internal.SystemPropertyUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Created by heshaoqiong on 2017/7/25.
 */
public class SecureSslContextFactory {
    private static final String PROTOCOL = "TLS";
    private static final SSLContext SERVER_CONTEXT;


    private static String SERVER_KEY_STORE = "/root/.keystore";
    //private static String SERVER_KEY_STORE = "/root/sslserverkeys";
    private static String SERVER_KEY_STORE_PASSWORD = "123456";


    static {
        String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext;

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(SERVER_KEY_STORE), SERVER_KEY_STORE_PASSWORD.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            kmf.init(ks, SERVER_KEY_STORE_PASSWORD.toCharArray());


            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }



        SERVER_CONTEXT = serverContext;

    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }


    private SecureSslContextFactory() {
        // Unused
    }
}
