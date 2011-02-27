package com.radicaldynamic.groupinform.utilities;

import java.io.InputStream;
import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;

/*
 * Our HTTP client class for making secure connections to the Group Inform application server.
 * This class uses our trusted keystore to authenticate the public SSL certificate used by the server.
 */
public class SecureHttpClient extends DefaultHttpClient
{
    private static final String t = "SecureHttpClient: ";
    
    public SecureHttpClient()
    {
    }

    @Override protected ClientConnectionManager createClientConnectionManager() 
    {
        SchemeRegistry registry = new SchemeRegistry();
        
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", newSslSocketFactory(), 5101));
        
        return new SingleClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSslSocketFactory() 
    {
        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            
            InputStream in = Collect.getInstance().getApplicationContext().getResources().openRawResource(R.raw.groupcomplete);

            // keypass should be hardedcoded, not a string resource
            try {
                Log.d(Collect.LOGTAG, t + "loading trusted certificates from keystore");
                trusted.load(in, "e04191f2".toCharArray());
            } finally {
                in.close();
            }
            
            return new SSLSocketFactory(trusted);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
