package com.milenko.weefree;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Milenko on 16/07/2015.
 */
public class myLog {
    //    private static String fileName;
    private static String currentDateandTime;

    public static void initialize(String filePath) {
        int file_size;

        currentDateandTime = currentDate();

        File folder = new File(Environment.getExternalStorageDirectory() + "/WFLOG");
        boolean success = true;
        if (!folder.exists()) {
            //Toast.makeText(MainActivity.this, "Directory Does Not Exist, Create It", Toast.LENGTH_SHORT).show();
            success = folder.mkdir();
        }
        if (success) {
            //Toast.makeText(MainActivity.this, "Directory Created", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(MainActivity.this, "Failed - Error", Toast.LENGTH_SHORT).show();
        }

//        fileName = currentDateandTime + "_mhp.txt";
        File logFile = new File(Environment.getExternalStorageDirectory() + filePath);
        file_size = Integer.parseInt(String.valueOf(logFile.length() / 1024));
        add("++++++++++++++++++++++++Session: " + currentDateandTime + "+++++++++++++++++++++++");

    }

    private static String currentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date());
    }

    public static void add(String text) {
//        try {
//            MainActivity.writeOnScreen(text);
//        } catch (Exception e) {
//            add("No se puede escribir en la pantalla principal", "mhp");
//        }
        add(text, "mhp");
    }

    /***
     * Add the text to a file which has TAG in the name. It also prints in this tag.
     *
     * @param text
     * @param TAG
     */
    public static void add(String text, String TAG) {
        Log.d(TAG, text);
        if (currentDateandTime == null) currentDateandTime = currentDate() + "_rec";

        File logFile = new File(Environment.getExternalStorageDirectory(), "/WFLOG/" + currentDateandTime + "_" + TAG + ".txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss (dd)| ");
            String currentDateandTime = sdf.format(new Date());

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(currentDateandTime + text);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Send unhandled errors to a text file in the phone
     *
     * @param activated
     */
    public static void WriteUnhandledErrors(boolean activated) {
        if (activated) {
            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    PrintWriter pw;
                    try {
                        pw = new PrintWriter(
                                new FileWriter(Environment.getExternalStorageDirectory() + "/WFLOG//rt.txt", true));
                        pw.append("*******" + currentDate() + "\n");
                        ex.printStackTrace(pw);
                        pw.flush();
                        pw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void addError(Class<?> clase, Exception e) {
        add("-----Error en " + clase.getSimpleName() + ": " + e.getLocalizedMessage());
    }

    public static void notificationMultiple(String title, String body, String summary, String sound) {
        myLog.add("***********************************SoUND:" + sound + "\n" + title + "\n" + body + summary + "\n", "NOTI");
    }
}