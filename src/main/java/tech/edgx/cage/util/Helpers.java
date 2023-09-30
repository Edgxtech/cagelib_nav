package tech.edgx.cage.util;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import tech.edgx.cage.model.Asset;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.RefEll;
import uk.me.jstott.jcoord.UTMRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Helpers {

    public static int SPEED_OF_LIGHT = 299792458; // [m/s]

    /*  Convert lat/lon to UTM northing/easting
    /*  - Filter operates in UTM format coords
    /*  - Returns Northing,Easting
    /*  - Example UTM coords: 6470194.755756934,403548.8617473827 */
    public static double[] convertLatLngToUtmNthingEasting(double lat, double lng) {
        LatLng ltln = new LatLng(lat,lng);
        UTMRef utm = ltln.toUTMRef();
        return new double[]{utm.getNorthing(),utm.getEasting()};
    }

    public static double[] convertLatLngToUtmNthingEastingSpecificZone(double lat, double lng, char latZone, int lngZone) {
        UTMRef utm = toUTMRefSpecificZone(lat, lng, latZone, lngZone);
        return new double[]{utm.getNorthing(),utm.getEasting()};
    }

    public static Object[] getUtmLatZoneLonZone(double lat, double lng) {
        LatLng ltln = new LatLng(lat,lng);
        UTMRef utm = ltln.toUTMRef();
        return new Object[]{utm.getLatZone(),utm.getLngZone()};
    }

    public static double[] convertUtmNthingEastingToLatLng(double nthing, double easting, char latZone, int lngZone) {
        UTMRef utm = new UTMRef(nthing,easting,latZone,lngZone);
        LatLng ltln = utm.toLatLng();
        return new double[]{ltln.getLat(),ltln.getLng()};
    }

    /** ADAPTED from Jcoord master method to allow projection onto custom UTM zones */
    public static UTMRef toUTMRefSpecificZone(double lat, double lon, char UTMZone, int longitudeZone) {
        double UTM_F0 = 0.9996D;
        double a = RefEll.WGS84.getMaj();
        double eSquared = RefEll.WGS84.getEcc();
        double longitude = lon;
        double latitude = lat;
        double latitudeRad = latitude * 0.017453292519943295D;
        double longitudeRad = longitude * 0.017453292519943295D;
        if (latitude >= 56.0D && latitude < 64.0D && longitude >= 3.0D && longitude < 12.0D) {
            longitudeZone = 32;
        }

        if (latitude >= 72.0D && latitude < 84.0D) {
            if (longitude >= 0.0D && longitude < 9.0D) {
                longitudeZone = 31;
            } else if (longitude >= 9.0D && longitude < 21.0D) {
                longitudeZone = 33;
            } else if (longitude >= 21.0D && longitude < 33.0D) {
                longitudeZone = 35;
            } else if (longitude >= 33.0D && longitude < 42.0D) {
                longitudeZone = 37;
            }
        }

        double longitudeOrigin = (double)((longitudeZone - 1) * 6 - 180 + 3);
        double longitudeOriginRad = longitudeOrigin * 0.017453292519943295D;
        double ePrimeSquared = eSquared / (1.0D - eSquared);
        double n = a / Math.sqrt(1.0D - eSquared * Math.sin(latitudeRad) * Math.sin(latitudeRad));
        double t = Math.tan(latitudeRad) * Math.tan(latitudeRad);
        double c = ePrimeSquared * Math.cos(latitudeRad) * Math.cos(latitudeRad);
        double A = Math.cos(latitudeRad) * (longitudeRad - longitudeOriginRad);
        double M = a * ((1.0D - eSquared / 4.0D - 3.0D * eSquared * eSquared / 64.0D - 5.0D * eSquared * eSquared * eSquared / 256.0D) * latitudeRad - (3.0D * eSquared / 8.0D + 3.0D * eSquared * eSquared / 32.0D + 45.0D * eSquared * eSquared * eSquared / 1024.0D) * Math.sin(2.0D * latitudeRad) + (15.0D * eSquared * eSquared / 256.0D + 45.0D * eSquared * eSquared * eSquared / 1024.0D) * Math.sin(4.0D * latitudeRad) - 35.0D * eSquared * eSquared * eSquared / 3072.0D * Math.sin(6.0D * latitudeRad));
        double UTMEasting = UTM_F0 * n * (A + (1.0D - t + c) * Math.pow(A, 3.0D) / 6.0D + (5.0D - 18.0D * t + t * t + 72.0D * c - 58.0D * ePrimeSquared) * Math.pow(A, 5.0D) / 120.0D) + 500000.0D;
        double UTMNorthing = UTM_F0 * (M + n * Math.tan(latitudeRad) * (A * A / 2.0D + (5.0D - t + 9.0D * c + 4.0D * c * c) * Math.pow(A, 4.0D) / 24.0D + (61.0D - 58.0D * t + t * t + 600.0D * c - 330.0D * ePrimeSquared) * Math.pow(A, 6.0D) / 720.0D));
        if (latitude < 0.0D) {
            UTMNorthing += 1.0E7D;
        }

        return new UTMRef(UTMEasting, UTMNorthing, UTMZone, longitudeZone);
    }

    public static double[] getEigenvalues(double[][] matrix) {
        RealMatrix J2 = new Array2DRowRealMatrix(matrix);
        EigenDecomposition eig = new EigenDecomposition(J2);
        double[] evaluesC = eig.getRealEigenvalues();
        return evaluesC;
    }

    public static double[] getEigenvector(double[][] matrix, double eigenvalue) {
        double a = matrix[0][0];
        double b = matrix[0][1];
        double c = matrix[1][0];
        double d = matrix[1][1];
        double e = eigenvalue;
        double x = b; double y = e-a;
        double r = Math.sqrt(x*x+y*y);
        if( r > 0) { x /= r; y /= r; }
        else {
            x = e-d; y = c;
            r = Math.sqrt(x*x+y*y);
            if( r > 0) { x /= r; y /= r; }
            else {
                x = 1; y = 0;
            }
        }
        return new double[]{x,y};
    }

    public static int getMostPopularLonZoneFromAssets(List<Asset> assets) {
        List<Integer> zoneStats = new ArrayList<Integer>();
        for (Asset asset : assets) {
            int lonZone = (int) Helpers.getUtmLatZoneLonZone(asset.getCurrent_loc()[0], asset.getCurrent_loc()[1])[1];
            zoneStats.add(lonZone);
        }
        return Helpers.getMode(zoneStats.stream().filter(t -> t != null).mapToInt(t -> t).toArray());
    }

    public static char getMostPopularLatZoneFromAssets(List<Asset> assets) {
        char[] chars = new char[assets.size()];
        for (int i=0; i<assets.size(); i++) { //Asset asset : assets) {
            Asset asset = assets.get(i);
            char latZone = (char) Helpers.getUtmLatZoneLonZone(asset.getCurrent_loc()[0], asset.getCurrent_loc()[1])[0];
            chars[i]=latZone;
        }
        return Helpers.getMode(chars);
    }

    public static int getMode(int[] array) {
        HashMap<Integer,Integer> hm = new HashMap<Integer,Integer>();
        int max  = 1;
        int temp = array[0];

        for(int i = 0; i < array.length; i++) {

            if (hm.get(array[i]) != null) {

                int count = hm.get(array[i]);
                count++;
                hm.put(array[i], count);

                if(count > max) {
                    max  = count;
                    temp = array[i];
                }
            }

            else
                hm.put(array[i],1);
        }
        return temp;
    }

    public static char getMode(char[] array) {
        HashMap<Character,Integer> hm = new HashMap<Character, Integer>();
        int max  = 1;
        char temp = array[0];

        for(int i = 0; i < array.length; i++) {

            if (hm.get(array[i]) != null) {

                int count = hm.get(array[i]);
                count++;
                hm.put(array[i], count);

                if(count > max) {
                    max  = count;
                    temp = array[i];
                }
            }

            else
                hm.put(array[i],1);
        }
        return temp;
    }

    // tanh−1x= 1/2 ln((1−x)/(1+x))
    public static double Arctanh(double x) throws Exception
    {
        if (Math.abs(x) > 1)
            throw new ObservationException("Error computing hyperbola");
        return 0.5 * Math.log((1 + x) / (1 - x));
    }

    // cosh−1x=ln(x+sqrt(x^2−1))
    public static double Arccosh(double x) throws Exception
    {
        return Math.log(x + Math.sqrt(x*x - 1.0));
    }

    public static double getMean(double[] data) {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/data.length;
    }

    public static double getVariance(double[] data) {
        double mean = getMean(data);
        double temp = 0;
        for(double a :data)
            temp += (a-mean)*(a-mean);
        return temp/(data.length-1);
    }
}
