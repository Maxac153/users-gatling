package ru.gatling.helpers;

public class DataFormatHelper {
    public static String format(long timeSec) {
        long days = timeSec / 86_400;
        long remainder = timeSec % 86_400;
        long hours = remainder / 3_600;
        remainder %= 3_600;
        long min = remainder / 60;
        long sec = remainder % 60;

        return String.format("%02d d. %02d h. %02d m. %02d s.", days, hours, min, sec);
    }
}
