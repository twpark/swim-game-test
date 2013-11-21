package edu.nclab.swimudpserver;

/**
 * Created with IntelliJ IDEA.
 * User: changhoon
 * Date: 2013. 11. 14.
 * Time: PM 1:39
 * To change this template use File | Settings | File Templates.
 */
public class Logger {
    public static void d(String msg) {
        System.out.println(msg);
    }

    // byte[] to hex
    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }
}
