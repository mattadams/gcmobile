package com.radicaldynamic.groupinform.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class to store persistent HTTP cookies (sessions)
 * 
 * Real cookie example
 * 
 * [version: 0]
 * [name: connect.sid]
 * [value: f1c2a4np39.yy%2FOkH4JWBUaRKgbIxfYmQ]
 * [domain: myhost]
 * [path: /]
 * [expiry: Sat Jan 15 14:29:44 MST 2011]
 */
public class HttpCookieStore implements Serializable
{
    private static final long serialVersionUID = 1463065884577681730L;

    private List<HttpCookieStore> cookies = new ArrayList<HttpCookieStore>();
    
    private String domain;
    private Date expiryDate;
    private String name;
    private String path;
    private String value;
    private int version;  
    
    public HttpCookieStore()
    {
        
    }
    
    public HttpCookieStore(String domain, Date expiryDate, String name, String path, String value, int version)
    {
        setDomain(domain);
        setExpiryDate(expiryDate);
        setName(name);
        setPath(path);
        setValue(value);
        setVersion(version);
    }

    public void setCookies(List<HttpCookieStore> cookies)
    {
        this.cookies = cookies;
    }

    public List<HttpCookieStore> getCookies()
    {
        return cookies;
    }

    public void setDomain(String domain) { this.domain = domain; }
    public String getDomain() { return domain; }

    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public Date getExpiryDate() { return expiryDate; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setPath(String path) { this.path = path; }
    public String getPath() { return path; }

    public void setValue(String value) { this.value = value; }
    public String getValue() { return value; }

    public void setVersion(int version) { this.version = version; }
    public int getVersion() { return version; }
}