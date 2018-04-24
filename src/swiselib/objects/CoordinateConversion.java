/*
 * Author: Sami Salkosuo, sami.salkosuo@fi.ibm.com
 *
 * (c) Copyright IBM Corp. 2007
 */
package swiselib.objects;

import java.util.Hashtable;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

public class CoordinateConversion
{

  public CoordinateConversion()
  {

  }

  public double[] utm2LatLon(String UTM)
  {
    UTM2LatLon c = new UTM2LatLon();
    return c.convertUTMToLatLong(UTM);
  }

  public String latLon2UTM(double latitude, double longitude)
  {
    LatLon2UTM c = new LatLon2UTM();
    return c.convertLatLonToUTM(latitude, longitude);

  }

  public Coordinate latLon2UTMCoord(double latitude, double longitude)
  {
    LatLon2UTM c = new LatLon2UTM();
    return c.convertLatLonToUTMCoord(latitude, longitude);

  }

  private void validate(double latitude, double longitude)
  {
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0
        || longitude >= 180.0)
    {
      throw new IllegalArgumentException(
          "Legal ranges: latitude [-90,90], longitude [-180,180).");
    }

  }

  public String latLon2MGRUTM(double latitude, double longitude)
  {
    LatLon2MGRUTM c = new LatLon2MGRUTM();
    return c.convertLatLonToMGRUTM(latitude, longitude);

  }

  public double[] mgrutm2LatLon(String MGRUTM)
  {
    MGRUTM2LatLon c = new MGRUTM2LatLon();
    return c.convertMGRUTMToLatLong(MGRUTM);
  }

  public double degreeToRadian(double degree)
  {
    return degree * Math.PI / 180;
  }

  public double radianToDegree(double radian)
  {
    return radian * 180 / Math.PI;
  }

  private double POW(double a, double b)
  {
    return Math.pow(a, b);
  }

  private double SIN(double value)
  {
    return Math.sin(value);
  }

  private double COS(double value)
  {
    return Math.cos(value);
  }

  private double TAN(double value)
  {
    return Math.tan(value);
  }

  private class LatLon2UTM
  {
    public String convertLatLonToUTM(double latitude, double longitude)
    {
      validate(latitude, longitude);
      String UTM = "";

      setVariables(latitude, longitude);

      String longZone = getLongZone(longitude);
      LatZones latZones = new LatZones();
      String latZone = latZones.getLatZone(latitude);

      double _easting = getEasting();
      double _northing = getNorthing(latitude);

      UTM = longZone + " " + latZone + " " + ((int) _easting) + " "
          + ((int) _northing);
      // UTM = longZone + " " + latZone + " " + decimalFormat.format(_easting) +
      // " "+ decimalFormat.format(_northing);

      return UTM;

    }
    
    public Coordinate convertLatLonToUTMCoord(double latitude, double longitude)
    {
      validate(latitude, longitude);
      String UTM = "";

      setVariables(latitude, longitude);

      String longZone = getLongZone(longitude);
      LatZones latZones = new LatZones();
      String latZone = latZones.getLatZone(latitude);

      double _easting = getEasting();
      double _northing = getNorthing(latitude);
     
      return new Coordinate(_easting, _northing);
/*      UTM = longZone + " " + latZone + " " + ((int) _easting) + " "
          + ((int) _northing);
      // UTM = longZone + " " + latZone + " " + decimalFormat.format(_easting) +
      // " "+ decimalFormat.format(_northing);

      return UTM;
*/
    }

    protected void setVariables(double latitude, double longitude)
    {
      latitude = degreeToRadian(latitude);
      rho = equatorialRadius * (1 - e * e)
          / POW(1 - POW(e * SIN(latitude), 2), 3 / 2.0);

      nu = equatorialRadius / POW(1 - POW(e * SIN(latitude), 2), (1 / 2.0));

      double var1;
      if (longitude < 0.0)
      {
        var1 = ((int) ((180 + longitude) / 6.0)) + 1;
      }
      else
      {
        var1 = ((int) (longitude / 6)) + 31;
      }
      double var2 = (6 * var1) - 183;
      double var3 = longitude - var2;
      p = var3 * 3600 / 10000;

      S = A0 * latitude - B0 * SIN(2 * latitude) + C0 * SIN(4 * latitude) - D0
          * SIN(6 * latitude) + E0 * SIN(8 * latitude);

      K1 = S * k0;
      K2 = nu * SIN(latitude) * COS(latitude) * POW(sin1, 2) * k0 * (100000000)
          / 2;
      K3 = ((POW(sin1, 4) * nu * SIN(latitude) * Math.pow(COS(latitude), 3)) / 24)
          * (5 - POW(TAN(latitude), 2) + 9 * e1sq * POW(COS(latitude), 2) + 4
              * POW(e1sq, 2) * POW(COS(latitude), 4))
          * k0
          * (10000000000000000L);

      K4 = nu * COS(latitude) * sin1 * k0 * 10000;

      K5 = POW(sin1 * COS(latitude), 3) * (nu / 6)
          * (1 - POW(TAN(latitude), 2) + e1sq * POW(COS(latitude), 2)) * k0
          * 1000000000000L;

      A6 = (POW(p * sin1, 6) * nu * SIN(latitude) * POW(COS(latitude), 5) / 720)
          * (61 - 58 * POW(TAN(latitude), 2) + POW(TAN(latitude), 4) + 270
              * e1sq * POW(COS(latitude), 2) - 330 * e1sq
              * POW(SIN(latitude), 2)) * k0 * (1E+24);

    }

    protected String getLongZone(double longitude)
    {
      double longZone = 0;
      if (longitude < 0.0)
      {
        longZone = ((180.0 + longitude) / 6) + 1;
      }
      else
      {
        longZone = (longitude / 6) + 31;
      }
      String val = String.valueOf((int) longZone);
      if (val.length() == 1)
      {
        val = "0" + val;
      }
      return val;
    }

    protected double getNorthing(double latitude)
    {
      double northing = K1 + K2 * p * p + K3 * POW(p, 4);
      if (latitude < 0.0)
      {
        northing = 10000000 + northing;
      }
      return northing;
    }

    protected double getEasting()
    {
      return 500000 + (K4 * p + K5 * POW(p, 3));
    }

    // Lat Lon to UTM variables

    // equatorial radius
    double equatorialRadius = 6378137;

    // polar radius
    double polarRadius = 6356752.314;

    // flattening
    double flattening = 0.00335281066474748;// (equatorialRadius-polarRadius)/equatorialRadius;

    // inverse flattening 1/flattening
    double inverseFlattening = 298.257223563;// 1/flattening;

    // Mean radius
    double rm = POW(equatorialRadius * polarRadius, 1 / 2.0);

    // scale factor
    double k0 = 0.9996;

    // eccentricity
    double e = Math.sqrt(1 - POW(polarRadius / equatorialRadius, 2));

    double e1sq = e * e / (1 - e * e);

    double n = (equatorialRadius - polarRadius)
        / (equatorialRadius + polarRadius);

    // r curv 1
    double rho = 6368573.744;

    // r curv 2
    double nu = 6389236.914;

    // Calculate Meridional Arc Length
    // Meridional Arc
    double S = 5103266.421;

    double A0 = 6367449.146;

    double B0 = 16038.42955;

    double C0 = 16.83261333;

    double D0 = 0.021984404;

    double E0 = 0.000312705;

    // Calculation Constants
    // Delta Long
    double p = -0.483084;

    double sin1 = 4.84814E-06;

    // Coefficients for UTM Coordinates
    double K1 = 5101225.115;

    double K2 = 3750.291596;

    double K3 = 1.397608151;

    double K4 = 214839.3105;

    double K5 = -2.995382942;

    double A6 = -1.00541E-07;

  }

  private class LatLon2MGRUTM extends LatLon2UTM
  {
    public String convertLatLonToMGRUTM(double latitude, double longitude)
    {
      validate(latitude, longitude);
      String mgrUTM = "";

      setVariables(latitude, longitude);

      String longZone = getLongZone(longitude);
      LatZones latZones = new LatZones();
      String latZone = latZones.getLatZone(latitude);

      double _easting = getEasting();
      double _northing = getNorthing(latitude);
      Digraphs digraphs = new Digraphs();
      String digraph1 = digraphs.getDigraph1(Integer.parseInt(longZone),
          _easting);
      String digraph2 = digraphs.getDigraph2(Integer.parseInt(longZone),
          _northing);

      String easting = String.valueOf((int) _easting);
      if (easting.length() < 5)
      {
        easting = "00000" + easting;
      }
      easting = easting.substring(easting.length() - 5);

      String northing;
      northing = String.valueOf((int) _northing);
      if (northing.length() < 5)
      {
        northing = "0000" + northing;
      }
      northing = northing.substring(northing.length() - 5);

      mgrUTM = longZone + latZone + digraph1 + digraph2 + easting + northing;
      return mgrUTM;
    }
  }

  private class MGRUTM2LatLon extends UTM2LatLon
  {
    public double[] convertMGRUTMToLatLong(String mgrutm)
    {
      double[] latlon = { 0.0, 0.0 };
      // 02CNR0634657742
      int zone = Integer.parseInt(mgrutm.substring(0, 2));
      String latZone = mgrutm.substring(2, 3);

      String digraph1 = mgrutm.substring(3, 4);
      String digraph2 = mgrutm.substring(4, 5);
      easting = Double.parseDouble(mgrutm.substring(5, 10));
      northing = Double.parseDouble(mgrutm.substring(10, 15));

      LatZones lz = new LatZones();
      double latZoneDegree = lz.getLatZoneDegree(latZone);

      double a1 = latZoneDegree * 40000000 / 360.0;
      double a2 = 2000000 * Math.floor(a1 / 2000000.0);

      Digraphs digraphs = new Digraphs();

      double digraph2Index = digraphs.getDigraph2Index(digraph2);

      double startindexEquator = 1;
      if ((1 + zone % 2) == 1)
      {
        startindexEquator = 6;
      }

      double a3 = a2 + (digraph2Index - startindexEquator) * 100000;
      if (a3 <= 0)
      {
        a3 = 10000000 + a3;
      }
      northing = a3 + northing;

      zoneCM = -183 + 6 * zone;
      double digraph1Index = digraphs.getDigraph1Index(digraph1);
      int a5 = 1 + zone % 3;
      double[] a6 = { 16, 0, 8 };
      double a7 = 100000 * (digraph1Index - a6[a5 - 1]);
      easting = easting + a7;

      setVariables();

      double latitude = 0;
      latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

      if (latZoneDegree < 0)
      {
        latitude = 90 - latitude;
      }

      double d = _a2 * 180 / Math.PI;
      double longitude = zoneCM - d;

      if (getHemisphere(latZone).equals("S"))
      {
        latitude = -latitude;
      }

      latlon[0] = latitude;
      latlon[1] = longitude;
      return latlon;
    }
  }

  private class UTM2LatLon
  {
    double easting;

    double northing;

    int zone;

    String southernHemisphere = "ACDEFGHJKLM";

    protected String getHemisphere(String latZone)
    {
      String hemisphere = "N";
      if (southernHemisphere.indexOf(latZone) > -1)
      {
        hemisphere = "S";
      }
      return hemisphere;
    }

    public double[] convertUTMToLatLong(String UTM)
    {
      double[] latlon = { 0.0, 0.0 };
      String[] utm = UTM.split(" ");
      zone = Integer.parseInt(utm[0]);
      String latZone = utm[1];
      easting = Double.parseDouble(utm[2]);
      northing = Double.parseDouble(utm[3]);
      String hemisphere = getHemisphere(latZone);
      double latitude = 0.0;
      double longitude = 0.0;

      if (hemisphere.equals("S"))
      {
        northing = 10000000 - northing;
      }
      setVariables();
      latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

      if (zone > 0)
      {
        zoneCM = 6 * zone - 183.0;
      }
      else
      {
        zoneCM = 3.0;

      }

      longitude = zoneCM - _a3;
      if (hemisphere.equals("S"))
      {
        latitude = -latitude;
      }

      latlon[0] = latitude;
      latlon[1] = longitude;
      return latlon;

    }

    protected void setVariables()
    {
      arc = northing / k0;
      mu = arc
          / (a * (1 - POW(e, 2) / 4.0 - 3 * POW(e, 4) / 64.0 - 5 * POW(e, 6) / 256.0));

      ei = (1 - POW((1 - e * e), (1 / 2.0)))
          / (1 + POW((1 - e * e), (1 / 2.0)));

      ca = 3 * ei / 2 - 27 * POW(ei, 3) / 32.0;

      cb = 21 * POW(ei, 2) / 16 - 55 * POW(ei, 4) / 32;
      cc = 151 * POW(ei, 3) / 96;
      cd = 1097 * POW(ei, 4) / 512;
      phi1 = mu + ca * SIN(2 * mu) + cb * SIN(4 * mu) + cc * SIN(6 * mu) + cd
          * SIN(8 * mu);

      n0 = a / POW((1 - POW((e * SIN(phi1)), 2)), (1 / 2.0));

      r0 = a * (1 - e * e) / POW((1 - POW((e * SIN(phi1)), 2)), (3 / 2.0));
      fact1 = n0 * TAN(phi1) / r0;

      _a1 = 500000 - easting;
      dd0 = _a1 / (n0 * k0);
      fact2 = dd0 * dd0 / 2;

      t0 = POW(TAN(phi1), 2);
      Q0 = e1sq * POW(COS(phi1), 2);
      fact3 = (5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * POW(dd0, 4)
          / 24;

      fact4 = (61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - 3 * Q0
          * Q0)
          * POW(dd0, 6) / 720;

      //
      lof1 = _a1 / (n0 * k0);
      lof2 = (1 + 2 * t0 + Q0) * POW(dd0, 3) / 6.0;
      lof3 = (5 - 2 * Q0 + 28 * t0 - 3 * POW(Q0, 2) + 8 * e1sq + 24 * POW(t0, 2))
          * POW(dd0, 5) / 120;
      _a2 = (lof1 - lof2 + lof3) / COS(phi1);
      _a3 = _a2 * 180 / Math.PI;

    }

    double arc;

    double mu;

    double ei;

    double ca;

    double cb;

    double cc;

    double cd;

    double n0;

    double r0;

    double _a1;

    double dd0;

    double t0;

    double Q0;

    double lof1;

    double lof2;

    double lof3;

    double _a2;

    double phi1;

    double fact1;

    double fact2;

    double fact3;

    double fact4;

    double zoneCM;

    double _a3;

    double b = 6356752.314;

    double a = 6378137;

    double e = 0.081819191;

    double e1sq = 0.006739497;

    double k0 = 0.9996;

  }

  private class Digraphs
  {
    private Map digraph1 = new Hashtable();

    private Map digraph2 = new Hashtable();

    private String[] digraph1Array = { "A", "B", "C", "D", "E", "F", "G", "H",
        "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
        "Y", "Z" };

    private String[] digraph2Array = { "V", "A", "B", "C", "D", "E", "F", "G",
        "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V" };

    public Digraphs()
    {
      digraph1.put(new Integer(1), "A");
      digraph1.put(new Integer(2), "B");
      digraph1.put(new Integer(3), "C");
      digraph1.put(new Integer(4), "D");
      digraph1.put(new Integer(5), "E");
      digraph1.put(new Integer(6), "F");
      digraph1.put(new Integer(7), "G");
      digraph1.put(new Integer(8), "H");
      digraph1.put(new Integer(9), "J");
      digraph1.put(new Integer(10), "K");
      digraph1.put(new Integer(11), "L");
      digraph1.put(new Integer(12), "M");
      digraph1.put(new Integer(13), "N");
      digraph1.put(new Integer(14), "P");
      digraph1.put(new Integer(15), "Q");
      digraph1.put(new Integer(16), "R");
      digraph1.put(new Integer(17), "S");
      digraph1.put(new Integer(18), "T");
      digraph1.put(new Integer(19), "U");
      digraph1.put(new Integer(20), "V");
      digraph1.put(new Integer(21), "W");
      digraph1.put(new Integer(22), "X");
      digraph1.put(new Integer(23), "Y");
      digraph1.put(new Integer(24), "Z");

      digraph2.put(new Integer(0), "V");
      digraph2.put(new Integer(1), "A");
      digraph2.put(new Integer(2), "B");
      digraph2.put(new Integer(3), "C");
      digraph2.put(new Integer(4), "D");
      digraph2.put(new Integer(5), "E");
      digraph2.put(new Integer(6), "F");
      digraph2.put(new Integer(7), "G");
      digraph2.put(new Integer(8), "H");
      digraph2.put(new Integer(9), "J");
      digraph2.put(new Integer(10), "K");
      digraph2.put(new Integer(11), "L");
      digraph2.put(new Integer(12), "M");
      digraph2.put(new Integer(13), "N");
      digraph2.put(new Integer(14), "P");
      digraph2.put(new Integer(15), "Q");
      digraph2.put(new Integer(16), "R");
      digraph2.put(new Integer(17), "S");
      digraph2.put(new Integer(18), "T");
      digraph2.put(new Integer(19), "U");
      digraph2.put(new Integer(20), "V");

    }

    public int getDigraph1Index(String letter)
    {
      for (int i = 0; i < digraph1Array.length; i++)
      {
        if (digraph1Array[i].equals(letter))
        {
          return i + 1;
        }
      }

      return -1;
    }

    public int getDigraph2Index(String letter)
    {
      for (int i = 0; i < digraph2Array.length; i++)
      {
        if (digraph2Array[i].equals(letter))
        {
          return i;
        }
      }

      return -1;
    }

    public String getDigraph1(int longZone, double easting)
    {
      int a1 = longZone;
      double a2 = 8 * ((a1 - 1) % 3) + 1;

      double a3 = easting;
      double a4 = a2 + ((int) (a3 / 100000)) - 1;
      return (String) digraph1.get(new Integer((int) Math.floor(a4)));
    }

    public String getDigraph2(int longZone, double northing)
    {
      int a1 = longZone;
      double a2 = 1 + 5 * ((a1 - 1) % 2);
      double a3 = northing;
      double a4 = (a2 + ((int) (a3 / 100000)));
      a4 = (a2 + ((int) (a3 / 100000.0))) % 20;
      a4 = Math.floor(a4);
      if (a4 < 0)
      {
        a4 = a4 + 19;
      }
      return (String) digraph2.get(new Integer((int) Math.floor(a4)));

    }

  }

  private class LatZones
  {
    private char[] letters = { 'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
        'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Z' };

    private int[] degrees = { -90, -84, -72, -64, -56, -48, -40, -32, -24, -16,
        -8, 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84 };

    private char[] negLetters = { 'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
        'L', 'M' };

    private int[] negDegrees = { -90, -84, -72, -64, -56, -48, -40, -32, -24,
        -16, -8 };

    private char[] posLetters = { 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
        'X', 'Z' };

    private int[] posDegrees = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84 };

    private int arrayLength = 22;

    public LatZones()
    {
    }

    public int getLatZoneDegree(String letter)
    {
      char ltr = letter.charAt(0);
      for (int i = 0; i < arrayLength; i++)
      {
        if (letters[i] == ltr)
        {
          return degrees[i];
        }
      }
      return -100;
    }

    public String getLatZone(double latitude)
    {
      int latIndex = -2;
      int lat = (int) latitude;

      if (lat >= 0)
      {
        int len = posLetters.length;
        for (int i = 0; i < len; i++)
        {
          if (lat == posDegrees[i])
          {
            latIndex = i;
            break;
          }

          if (lat > posDegrees[i])
          {
            continue;
          }
          else
          {
            latIndex = i - 1;
            break;
          }
        }
      }
      else
      {
        int len = negLetters.length;
        for (int i = 0; i < len; i++)
        {
          if (lat == negDegrees[i])
          {
            latIndex = i;
            break;
          }

          if (lat < negDegrees[i])
          {
            latIndex = i - 1;
            break;
          }
          else
          {
            continue;
          }

        }

      }

      if (latIndex == -1)
      {
        latIndex = 0;
      }
      if (lat >= 0)
      {
        if (latIndex == -2)
        {
          latIndex = posLetters.length - 1;
        }
        return String.valueOf(posLetters[latIndex]);
      }
      else
      {
        if (latIndex == -2)
        {
          latIndex = negLetters.length - 1;
        }
        return String.valueOf(negLetters[latIndex]);

      }
    }

  }
  
  public static double[] wgs84toOSGB36(double lat, double lon) {

	  double[] retVals = new double[2];

	  double lat_1 = (lat * Math.PI) / 180;
	  double lon_1 = (lon * Math.PI) / 180;

	  double a_1 = 6378137.0d;
	  double b_1 = 6356752.3141d;
	  double e2_1 = 1 - (b_1 * b_1) / (a_1 * a_1);
	  double nu_1 = a_1
	  / (Math.sqrt(1 - e2_1 * (Math.sin(lat_1) * (Math.sin(lat_1)))));

	  double H = 0;
	  double x_1 = (nu_1 + H) * Math.cos(lat_1) * Math.cos(lon_1);
	  double y_1 = (nu_1 + H) * Math.cos(lat_1) * Math.sin(lon_1);
	  double z_1 = ((1 - e2_1) * nu_1 + H) * Math.sin(lat_1);

	  double s = 20.4894 * Math.pow(10, -6);
	  double tx = -446.448;
	  double ty = 125.157;
	  double tz = -542.060;
	  double rxs = -0.1502;
	  double rys = -0.2470;
	  double rzs = -0.8421;
	  double rx = rxs * Math.PI / (180 * 3600.);
	  double ry = rys * Math.PI / (180 * 3600.);
	  double rz = rzs * Math.PI / (180 * 3600.);
	  double x_2 = tx + (1 + s) * x_1 + (-rz) * y_1 + (ry) * z_1;
	  double y_2 = ty + (rz) * x_1 + (1 + s) * y_1 + (-rx) * z_1;
	  double z_2 = tz + (-ry) * x_1 + (rx) * y_1 + (1 + s) * z_1;

	  double a = 6377563.396;
	  double b = 6356256.909;
	  double e2 = 1 - (b * b) / (a * a);
	  double p = Math.sqrt(Math.pow(x_2, 2) + Math.pow(y_2, 2));

	  lat = Math.atan2(z_2, (p * (1 - e2)));
	  double latold = 2 * Math.PI;
	  double nu = 0;

	  int numLoops = 0; // counter to limit number of loops
	  while (numLoops < Math.pow(10, -16)) {
	  latold = lat;
	  nu = a / Math.sqrt(1 - e2 * Math.pow(Math.sin(latold), 2));
	  lat = Math.atan2(z_2 + e2 * nu * Math.sin(latold), p);
	  numLoops++;
	  }

	  lon = Math.atan2(y_2, x_2);
	  H = p / Math.cos(lat) - nu;

	  double F0 = 0.9996012717;
	  double lat0 = 49 * Math.PI / 180;
	  double lon0 = -2 * Math.PI / 180;
	  double N0 = -100000;
	  double E0 = 400000;
	  double n = (a - b) / (a + b);

	  double rho = a * F0 * (1 - e2) * (1 - e2 * Math.pow(Math.pow(Math.sin(lat), 2), (-1.5)));
	  double eta2 = nu * F0 / rho - 1;

	  double M1 = (1 + n + (5 / 4) * Math.pow(n, 2) + (5 / 4) * Math.pow(n, 3)) * (lat - lat0);
	  double M2 = (3 * n + 3 * Math.pow(n, 2) + (21 / 8) * Math.pow(n, 3)) * Math.sin(lat - lat0) * Math.cos(lat + lat0);
	  double M3 = ((15 / 8) * Math.pow(n, 2) + (15 / 8) * Math.pow(n, 3)) * Math.sin(2 * (lat - lat0)) * Math.cos(2 * (lat + lat0));
	  double M4 = (35 / 24) * Math.pow(n, 3) * Math.sin(3 * (lat - lat0)) * Math.cos(3 * (lat + lat0));

	  double M = b * F0 * (M1 - M2 + M3 - M4);

	  double I = M + N0;
	  double II = nu * F0 * Math.sin(lat) * Math.cos(lat) / 2;
	  double III = nu * F0 * Math.sin(lat) * Math.pow(Math.cos(lat), 3) * (5 - Math.pow(Math.tan(lat), 2) + 9 * eta2) / 24;
	  double IIIA = nu * F0 * Math.sin(lat) * Math.pow(Math.cos(lat), 5) * (61 - 58 * Math.pow(Math.tan(lat), 2) + Math.pow(Math
	  .tan(lat), 4)) / 720;
	  double IV = nu * F0 * Math.cos(lat);
	  double V = nu * F0 * Math.pow(Math.cos(lat), 3) * (nu / rho - Math.pow(Math.tan(lat), 2)) / 6;
	  double VI = nu * F0 * Math.pow(Math.cos(lat), 5) * (5 - 18 * Math.pow(Math.tan(lat), 2)
	  + Math.pow(Math.tan(lat), 4) + 14 * eta2 - 58 * eta2 * Math.pow(Math.tan(lat), 2)) / 120;

	  double N = I + II * Math.pow((lon - lon0), 2) + III * Math.pow((lon - lon0), 4) + IIIA * Math.pow((lon - lon0), 6);
	  double E = E0 + IV * (lon - lon0) + V * Math.pow((lon - lon0), 3) + VI * Math.pow((lon - lon0), 5);

	  retVals[0] = E;
	  retVals[1] = N;

//	  System.out.println(E + ", " + N);

	  return retVals;
	  }

}
