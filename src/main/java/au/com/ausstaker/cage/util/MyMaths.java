package au.com.ausstaker.cage.util;

public class MyMaths
{
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
        //return Math.log(x + Math.sqrt(Math.pow(x,2) - 1));

        return Math.log(x + Math.sqrt(x*x - 1.0));
    }

    // sinh−1x=ln(x+x2+1) - Not used

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