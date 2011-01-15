package com.radicaldynamic.groupinform.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import com.radicaldynamic.groupinform.application.Collect;

import android.util.Log;

public class HttpUtils
{
    private static final String t = "HttpUtils: ";
    
    /*
     * TODO: implement cookie handling
     */
    public static String getUrlData(String url)
    {
        String websiteData = null;
        
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            
            // Load any cookies that have been stored
            if (Collect.getInstance().getInformOnlineState().getSession() == null) 
                Log.w(Collect.LOGTAG, t + "connection without session");
            else
                client.setCookieStore(Collect.getInstance().getInformOnlineState().getSession());
            
            URI uri = new URI(url);            
            HttpGet method = new HttpGet(uri);
            HttpResponse response = client.execute(method);
            InputStream data = response.getEntity().getContent();            
            websiteData = generateString(data);
            
            // Remember any session cookies that may have been returned
            List<Cookie> cookies = client.getCookieStore().getCookies();
            
            if (cookies.isEmpty())
                Log.d(Collect.LOGTAG, t + "GET resulted in no cookies");
            else {
                Log.i(Collect.LOGTAG, t + "GET resulted in " + cookies.size() + " cookies");
                Collect.getInstance().getInformOnlineState().setSession(client.getCookieStore());
                
                for (int i = 0; i < cookies.size(); i++)
                    Log.d(Collect.LOGTAG, t + "parsed cookie " + cookies.get(i).toString());
            }
            
            // Shutdown client manager to ensure deallocation of all system resources
            client.getConnectionManager().shutdown();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return websiteData;
    }
    
    /*
     * TODO: implement cookie handling
     */
    public static String postUrlData(String url, List<NameValuePair> params) 
    {
        String websiteData = null;
        
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            
            // Load any cookies that have been stored
            if (Collect.getInstance().getInformOnlineState().getSession() == null) 
                Log.w(Collect.LOGTAG, t + "connection without session");
            else
                client.setCookieStore(Collect.getInstance().getInformOnlineState().getSession());
            
            URI uri = new URI(url);
            HttpPost method = new HttpPost(uri);
            method.setEntity(new UrlEncodedFormEntity(params));            
            HttpResponse response = client.execute(method);
            InputStream data = response.getEntity().getContent();
            websiteData = generateString(data);
            
            // Remember any session cookies that may have been returned
            List<Cookie> cookies = client.getCookieStore().getCookies();
            
            if (cookies.isEmpty())
                Log.d(Collect.LOGTAG, t + "GET resulted in no cookies");
            else {
                Log.i(Collect.LOGTAG, t + "GET resulted in " + cookies.size() + " cookies");
                Collect.getInstance().getInformOnlineState().setSession(client.getCookieStore());
                
                for (int i = 0; i < cookies.size(); i++)
                    Log.d(Collect.LOGTAG, t + "parsed cookie " + cookies.get(i).toString());
            }
            
            // Shutdown client manager to ensure deallocation of all system resources
            client.getConnectionManager().shutdown();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return websiteData;
    }
    
    private static String generateString(InputStream stream) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader, 8192);
        StringBuilder sb = new StringBuilder();
        
        String cur;

        while ((cur = buffer.readLine()) != null) {
            sb.append(cur + "\n");
        }

        stream.close();
        
        return sb.toString();    
    }
}