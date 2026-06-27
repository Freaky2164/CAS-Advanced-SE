/*
 * Created on 02.02.2006
 */
package compucrash;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Peter
 */
public class Utilities {
    public static final int FIRST_OF_MONTH = 1;
    public static final int LAST_OF_MONTH = 2;

    public static String convertDateStringToDay(String MM_YY, int pos) {
        /* Konvertierung eines Formats aus MM/YYYY, MM/YY, M/YY, M/YYYY, YYYY in DD.MM.YYYY
         * Ein korrektes Datum soll so bleiben.
         */
        String MM = "";
        String YYYY = "";
        int slash = MM_YY.indexOf('/');
        if (slash == 1) {
            MM = "0" + MM_YY.charAt(0);
            YYYY = MM_YY.substring(2);
        } else if (slash == 2) {
            MM = MM_YY.substring(0, 2);
            YYYY = MM_YY.substring(3);
        } else {
            return convertDateStringToDay_noSlash(MM_YY, pos);
        }
        YYYY = expandTwoDigitYear(YYYY);
        String DD = (pos == 2) ? getLastDayOfMonth(MM, YYYY) : "01";
        return DD + "." + MM + "." + YYYY;
    }

    private static String convertDateStringToDay_noSlash(String MM_YY, int pos) {
        if (MM_YY.indexOf('.') > 0) {
            return MM_YY;
        }
        return (pos == 2) ? "31.12." + MM_YY : "01.01." + MM_YY;
    }

    private static String expandTwoDigitYear(String YYYY) {
        if (YYYY.length() == 2) {
            if (Integer.parseInt(YYYY) > 50) {
                return getActualYearMinus1().substring(0, 2) + YYYY;
            } else {
                return getActualYear().substring(0, 2) + YYYY;
            }
        }
        return YYYY;
    }

    public static String getLastDayOfMonth(String MM, String YYYY) {
        GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(YYYY), Integer.parseInt(MM), 1);
        int dd = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String DD = Integer.toString(dd);
        return DD;
    }

    public static String convertDate(String MM_YY) {
        /* Konvertierung eines Formats aus MM/YYYY, MM/YY, M/YY, M/YYYY in YYYY/MM */
        String YYYY_MM = "";
        String MM = "";
        String YYYY = "";
        int slash = MM_YY.indexOf('/');
        if (slash == 1) {
            MM = "0" + MM_YY.charAt(0);
            YYYY = MM_YY.substring(2);
        } else if (slash == 2) {
            MM = MM_YY.substring(0, 2);
            YYYY = MM_YY.substring(3);
        }
        if (YYYY.length() == 2) {
            if (Integer.parseInt(YYYY) > 50) {
                YYYY = getActualYearMinus1().substring(0, 2) + YYYY;
            } else {
                YYYY = getActualYear().substring(0, 2) + YYYY;
            }
        }
        YYYY_MM = YYYY + "/" + MM;
        return YYYY_MM;
    }

    public static String getActualYear() {
        GregorianCalendar cal = new GregorianCalendar();
        String YYYY = Integer.toString(cal.get(Calendar.YEAR));
        return YYYY;
    }

    public static String getActualYearMinus1() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        String YYYY = Integer.toString(cal.get(Calendar.YEAR) - 1);
        return YYYY;
    }
}
