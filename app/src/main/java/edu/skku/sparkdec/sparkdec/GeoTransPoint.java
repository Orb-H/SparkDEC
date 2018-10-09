package edu.skku.sparkdec.sparkdec;

/**
 * How to use?
 *     protected Double[] convertToGIS(String X,String Y)
 *     {
 *         GeoTransPoint oKA = new GeoTransPoint(Double.parseDouble(X),Double.parseDouble(Y));
 *         GeoTransPoint oGeo = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, oKA);
 *         Double[] p = new Double[]{oGeo.getX(),oGeo.getY()};
 *         return p;
 *     }
 *     like this.
 */

public class GeoTransPoint {

    double x;
    double y;
    double z;


    public GeoTransPoint() {
        super();
    }


    public GeoTransPoint(double x, double y) {

        super();
        this.x = x;
        this.y = y;
        this.z = 0;

    }

    public GeoTransPoint(double x, double y, double z) {
        super();
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
