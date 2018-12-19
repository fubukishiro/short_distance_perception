package com.example.fubuki.short_distance_perception;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {
    private static final String TAG = "FileLogger";

    private String filePath;
    private String fileName;
    public void initData() {
        SimpleDateFormat formatter = new SimpleDateFormat   ("yyyy-MM-dd-HH:mm:ss");
        Date curDate =  new Date(System.currentTimeMillis());

        filePath = Environment.getExternalStorageDirectory()+"/Test_Short_Distance/";
        fileName = "iLocLog_"+formatter.format(curDate)+".txt";

        writeTxtToFile(formatter.format(curDate), filePath, fileName);
        //writeTxtToFile("txt content2", filePath, fileName);
        //writeTxtToFile("txt content3", filePath, fileName);
    }

    public String getFilePath(){
        return filePath;
    }

    public String getFileName(){
        return fileName;
    }
    // 将字符串写入到文本文件中
    public void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath+fileName;
        // 每次写入时，都换行写
        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.e("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e+"");
        }
    }
}
