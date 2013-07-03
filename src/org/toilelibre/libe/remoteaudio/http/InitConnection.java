package org.toilelibre.libe.remoteaudio.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class InitConnection {

    public static HttpClient createClient () {
        HttpParams httpParams = new BasicHttpParams ();
        HttpProtocolParams.setVersion (httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset (httpParams, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue (httpParams, false);
        return new DefaultHttpClient (httpParams);
    }

    public static HttpGet createHttpGet (String host, String path) {
        return InitConnection.createHttpGet (host, path, "http");
    }

    public static HttpGet createHttpGet (String host, String path,
            String protocol) {
        String url = protocol + "://" + host + path;

        HttpGet httpget = new HttpGet (url);
        httpget.addHeader ("Host", host);
        return httpget;
    }

    public static HttpPost createHttpPost (String host, String path) {
        return InitConnection.createHttpPost (host, path, "http");
    }

    public static HttpPost createHttpPost (String host, String path,
            String protocol) {
        String url = protocol + "://" + host + path;

        HttpPost httppost = new HttpPost (url);
        httppost.addHeader ("Host", host);
        return httppost;
    }

    public static String getContent (HttpEntity he) {
        StringBuffer sb = new StringBuffer ();
        try {
            InputStream is = he.getContent ();
            int cI = (char) is.read ();
            while (cI != -1 && cI != 65535) {
                char c = (char) cI;
                sb.append (c);
                cI = (char) is.read ();
            }
        } catch (IllegalStateException e) {
            e.hashCode ();
        } catch (IOException e) {
            e.hashCode ();
        }

        return sb.toString ();
    }

    public static InputStream getContentInputStream (HttpEntity he) {
        try {
            return he.getContent ();
        } catch (IllegalStateException e) {
            e.printStackTrace ();
        } catch (IOException e) {
            e.printStackTrace ();
        }
        return null;
    }

    public static InputStream getContentInputStream (String host, String path)
            throws SocketException {
        return InitConnection.getContentInputStream ("http", host, path);
    }

    public static InputStream getContentInputStream (String protocol,
            String host, String path) throws SocketException {
        URL url = null;
        InputStream is = null;
        try {
            url = new URL (protocol, host, path);
        } catch (MalformedURLException e) {
            e.printStackTrace ();
        }

        if (url != null) {
            try {
                is = url.openStream ();
            } catch (IOException e) {
                e.printStackTrace ();
            }
        }
        return is;
    }
}
