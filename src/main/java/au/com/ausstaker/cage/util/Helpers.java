package au.com.ausstaker.cage.util;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import au.com.ausstaker.cage.model.Asset;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.RefEll;
import uk.me.jstott.jcoord.UTMRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
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
        //UTMRef utm = latLonToSpecificZoneUTMRef(lat, lng, lngZone);
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
        //int longitudeZone = (int)Math.floor((longitude + 180.0D) / 6.0D) + 1; // REMOVED, USED CUSTOM
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
        //char UTMZone_orig = UTMRef.getUTMLatitudeZoneLetter(latitude); // REMOVED, USED CUSTOM
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
//        double a = matrix[0][0];
//        double b = matrix[0][1];
//        double c = matrix[1][0];
//        double d = matrix[1][1];
//        System.out.println("a: "+a+", d: "+d);
//        if (a==d) { // Shitty Fix. This manual approach suffers from producing NaN when a==d, prefer the approach below
//            d=d+1.0e-18;
//        }
//        double e1 = ((a+d) + Math.sqrt( Math.pow(a-d,2) + 4*b*c))/2;
//        double e2 = ((a+d) - Math.sqrt( Math.pow(a-d,2) + 4*b*c))/2;
//        System.out.println("e1: "+e1+", e2: "+e2);
//        return new double[]{e1,e2};

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
//        e = eigenvalue2;
//        x = b; y = e-a;
//        r = Math.sqrt(x*x+y*y);
//        if( r > 0) { x /= r; y /= r; }
//        else {
//            x = e-d; y = c;
//            r = Math.sqrt(x*x+y*y);
//            if( r > 0) { x /= r; y /= r; }
//            else {
//                x = 0; y = 1;
//            }
//        }
//        System.out.println("Eigenvector2: (" + x + "," + y + ")");
//
//
//        double[][] basis = new double[2][2];
//
//        for (double y = -1000; y <= 1000; y++) {
//            for (double x = -1000; x <= 1000; x++) {
//                if (((a-eigenvalue1)*x + b*y == 0) && (c*x + (d-eigenvalue1)*y == 0)) {
//                    System.out.println("Eigenvector1: (" + x + "," + y + ")");
//                    basis[0] = eigenvalue1;
//                }
//            }
//        }
//
//        for (double y = -10; y <= 10; y++) {
//            for (double x = -10; x <= 10; x++) {
//                if (((a-eigenvalue2)*x + b*y == 0) && (c*x + (d-eigenvalue2)*y == 0)) {
//                    System.out.println("Eigenvector2: (" + x + "," + y + ")");
//                    basis[1] = eigenvalue2;
//                }
//            }
//        }
//
//        return basis;
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
}
