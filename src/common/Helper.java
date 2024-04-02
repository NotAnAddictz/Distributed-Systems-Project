package common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Helper {
    public static void printFileLastModifiedTime(Map<String, Long> fileLastModifiedTime) {
        // Print all key-value pairs with time formatted to 24-hour clock
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for (Map.Entry<String, Long> entry : fileLastModifiedTime.entrySet()) {
            String fileName = entry.getKey();
            long timeInMillis = entry.getValue();

            // Convert milliseconds to Date object
            Date date = new Date(timeInMillis);

            // Format Date to 24-hour clock time
            String formattedTime = sdf.format(date);

            System.out.println("File: " + fileName + ", Last Modified Time: " + formattedTime);
        }
    }

    public static void printLastModifiedTime(Long lastModifiedTime) {
        // Print all key-value pairs with time formatted to 24-hour clock
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        Date date = new Date(lastModifiedTime);
        String formattedTime = sdf.format(date);

        System.out.println("Last Modified Time: " + formattedTime);
    }

    public static String convertLastModifiedTime(Long lastModifiedTime) {
        // Print all key-value pairs with time formatted to 24-hour clock
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        Date date = new Date(lastModifiedTime);
        String formattedTime = sdf.format(date);

        return formattedTime;
    }
}