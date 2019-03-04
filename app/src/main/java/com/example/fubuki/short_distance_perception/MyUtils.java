package com.example.fubuki.short_distance_perception;

import android.os.Message;
import android.text.TextUtils;

import java.util.List;
import java.util.logging.Handler;

import static com.example.fubuki.short_distance_perception.Constant.ADD_NODE;

public class MyUtils {
    //string转float
    public static float convertToFloat(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int convertToInt(String number, int defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    //判断蓝牙接收距离趋势
    /**
     *
     * @param distanceArray
     * @param mFileLogger
     * @return status: 0: 无趋势; 1: 增大趋势; 2: 减小趋势
     */
    public static int judgeTrend(List<Float> distanceArray,FileLogger mFileLogger){
        int currentNum = distanceArray.size();
        int distanceJudgeSize = 7; //判断所使用的窗口长度
        int ascendCount = 0;
        int descendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - distanceJudgeSize;i--){
            double temp1 = distanceArray.get(i);
            double temp2 = distanceArray.get(i-1);

            if((temp1 - temp2 >= 0)&&(temp2 != 0))
                ascendCount++;

            if((temp1 - temp2 < 0)&&(temp2 != 0))
                descendCount++;
        }

        if(ascendCount > Math.floor(distanceJudgeSize/2)) {
            //mFileLogger.writeTxtToFile("距离逐渐增大的提示"+distanceArray.get(currentNum-1)+"#"+distanceArray.get(currentNum-2)+"#"+distanceArray.get(currentNum-3)+"#"+distanceArray.get(currentNum-4)+"#"+distanceArray.get(currentNum-5)+"#"+distanceArray.get(currentNum-6)+"#"+distanceArray.get(currentNum-7),mFileLogger.getFilePath(),mFileLogger.getFileName());
            return 1;
        }else if(descendCount > Math.floor(distanceJudgeSize/2))
            return 2;
        else
            return 0;
    }

    public static void handleMessage(android.os.Handler handler, int command, String msg){
        Message tempMsg = new Message();
        tempMsg.what = command;
        tempMsg.obj = msg;
        handler.sendMessage(tempMsg);
    }
}
