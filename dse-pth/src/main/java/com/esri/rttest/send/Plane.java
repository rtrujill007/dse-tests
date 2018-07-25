package com.esri.rttest.send;

public class Plane
{
  private int id;
  private long ts;
  private double speed;
  private double dist;
  private double bearing;
  private int rtid;
  private String orig;
  private String dest;
  private int    secsToDep;
  private double longitude;
  private double latitude;

  public Plane(int id, long ts, double speed, double dist, double bearing, int rtid, String orig, String dest, int secsToDep, double longitude, double latitude) {
    this.id = id;
    this.ts = ts;
    this.speed = speed;
    this.dist = dist;
    this.bearing = bearing;
    this.rtid = rtid;
    this.orig = orig;
    this.dest = dest;
    this.secsToDep = secsToDep;
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public int getId()
  {
    return id;
  }

  public void setId(int id)
  {
    this.id = id;
  }

  public long getTs()
  {
    return ts;
  }

  public void setTs(long ts)
  {
    this.ts = ts;
  }

  public double getSpeed()
  {
    return speed;
  }

  public void setSpeed(double speed)
  {
    this.speed = speed;
  }

  public double getDist()
  {
    return dist;
  }

  public void setDist(double dist)
  {
    this.dist = dist;
  }

  public double getBearing()
  {
    return bearing;
  }

  public void setBearing(double bearing)
  {
    this.bearing = bearing;
  }

  public int getRtid()
  {
    return rtid;
  }

  public void setRtid(int rtid)
  {
    this.rtid = rtid;
  }

  public String getOrig()
  {
    return orig;
  }

  public void setOrig(String orig)
  {
    this.orig = orig;
  }

  public String getDest()
  {
    return dest;
  }

  public void setDest(String dest)
  {
    this.dest = dest;
  }

  public int getSecsToDep()
  {
    return secsToDep;
  }

  public void setSecsToDep(int secsToDep)
  {
    this.secsToDep = secsToDep;
  }

  public double getLongitude()
  {
    return longitude;
  }

  public void setLongitude(double longitude)
  {
    this.longitude = longitude;
  }

  public double getLatitude()
  {
    return latitude;
  }

  public void setLatitude(double latitude)
  {
    this.latitude = latitude;
  }

  public String getGeometry() {
    return "POINT (" + longitude + " " + latitude + ")";
  }
}
