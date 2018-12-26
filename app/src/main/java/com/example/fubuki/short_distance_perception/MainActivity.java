package com.example.fubuki.short_distance_perception;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.fubuki.short_distance_perception.Constant.ADD_NODE;
import static com.example.fubuki.short_distance_perception.Constant.DISCONN_BLE;
import static com.example.fubuki.short_distance_perception.Constant.UPDATE_LIST;
import static com.example.fubuki.short_distance_perception.Constant.UPDATE_STATUS;
import static com.example.fubuki.short_distance_perception.MyUtils.convertToFloat;
import static com.example.fubuki.short_distance_perception.MyUtils.convertToInt;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    //语音合成接口相关
    protected String appId = "15163915";
    protected String appKey = "KorgAFZSjtfzG7KUdkKKxwQv";
    protected String secretKey = "ltHdCCYX6wGAinIHmLNACGV9EBCjZTOd";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    //目前没有离线包，仅使用在线的
    private TtsMode ttsMode = TtsMode.ONLINE;

    private Thread ttsThread; //声明一个子线程

    protected SpeechSynthesizer mSpeechSynthesizer;
    private SpeechSynthesizerListener mSpeechSynthesizerListener;

    private static final String TAG = "Main Activity";

    private static final String TEXT = "你好，这是播放测试";

    private BLE mBLE = new BLE();

    private List<String> bluetoothDevices = new ArrayList<String>(); //保存搜索到的列表
    private ArrayAdapter<String> arrayAdapter; //ListView的适配器

    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    private TextView statusText;

    private PaintBoard paintBoard;

    private Vibrator distanceVibrator;

    private float saveDistance; //设定的安全距离

    private float rcvDis; //从终端接收回来的距离

    private List<Float> distanceArray = new ArrayList<Float>();//存放接收的距离序列

    private FileLogger mFileLogger = new FileLogger();

    private boolean isRecord = false;

    private int statusCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPermission();
        initBLE();
        initPaintBoard();
        //语音线程
        ttsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initTTS();
            }
        });
        ttsThread.start(); //启动线程
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    private void  initPaintBoard(){
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        int paintHeight = (int) (height*0.5);
        paintBoard = findViewById(R.id.paint_board);

        paintBoard.setWindow(width,height);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,paintHeight);
        paintBoard.setLayoutParams(layoutParams);

        saveDistance = 10;

        distanceVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
    }
    private void initView() {

        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.speak:
                        speak(TEXT);
                        break;
                    case R.id.stop:
                        stop();
                        break;
                    case R.id.searchBtn:
                        if(mBLE.bluetoothGatt == null){
                            actionAlertDialog();
                        }else{
                            mBLE.disconnect_BLE(handler);
                            mBLE.bluetoothGatt = null;
                        }
                        break;
                    case R.id.addNode:
                        addNode();
                        break;
                    case R.id.setDistance:
                        setDistance();
                        break;
                    case R.id.startRecord:
                        isRecord = true;
                        mFileLogger.initData();
                        break;
                    default:
                        break;
                }
            }
        };

        //设定播放按钮
        Button speakBtn = findViewById(R.id.speak);
        speakBtn.setOnClickListener(listener);

        //设定暂停按钮
        Button stopBtn = findViewById(R.id.stop);
        stopBtn.setOnClickListener(listener);

        //设定记录按钮
        Button recordBtn = findViewById(R.id.startRecord);
        recordBtn.setOnClickListener(listener);

        Button searchBtn = findViewById(R.id.searchBtn);
        Button addNodeBtn = findViewById(R.id.addNode);
        Button setDistBtn = findViewById(R.id.setDistance);

        searchBtn.setOnClickListener(listener);
        addNodeBtn.setOnClickListener(listener);
        setDistBtn.setOnClickListener(listener);

        statusText = (TextView) findViewById(R.id.statusText);
    }

    private void initTTS(){

        // 1. 获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);

        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener(mSpeechSynthesizerListener);

        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setApiKey");

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        mSpeechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL);

        // 6. 初始化
        result = mSpeechSynthesizer.initTts(ttsMode);
        checkResult(result, "initTts");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.d(TAG,"error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    private void speak(String speakText) {
        /* 以下参数每次合成时都可以修改
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
         *  设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5"); 设置合成的音量，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5"); 设置合成的语速，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5"); 设置合成的语调，0-9 ，默认 5
         *
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
         *  MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         */

        if (mSpeechSynthesizer == null) {
            Log.e(TAG,"[ERROR], 初始化失败");
            return;
        }
        int result = mSpeechSynthesizer.speak(speakText);
        Log.e(TAG,"合成并播放 按钮已经点击");
        checkResult(result, "speak");
    }

    private void stop() {
        Log.e(TAG,"停止合成引擎 按钮已经点击");
        int result = mSpeechSynthesizer.stop();
        checkResult(result, "stop");
    }

    private void initBLE(){
        // 检查手机是否支持BLE，不支持则退出
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBLE.mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBLE.mBluetoothAdapter = mBLE.mBluetoothManager.getAdapter();
    }

    private void actionAlertDialog(){

        View bottomView = View.inflate(MainActivity.this,R.layout.ble_devices,null);//填充ListView布局
        ListView lvDevices = (ListView) bottomView.findViewById(R.id.device_list);//初始化ListView控件
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        builder= new AlertDialog.Builder(MainActivity.this)
                .setTitle("蓝牙列表").setView(bottomView);//在这里把写好的这个listview的布局加载dialog中
        alertDialog = builder.create();
        alertDialog.show();

        mBLE.bluetoothLeScanner = mBLE.mBluetoothAdapter.getBluetoothLeScanner();
        mBLE.bluetoothLeScanner.startScan(scanCallback);//android5.0把扫描方法单独弄成一个对象了（alt+enter添加），扫描结果储存在devices数组中。最好在startScan()前调用stopScan()。

        handler.postDelayed(runnable, 10000);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult results) {
            super.onScanResult(callbackType, results);
            BluetoothDevice device = results.getDevice();
            if (!mBLE.devices.contains(device)) {  //判断是否已经添加
                mBLE.devices.add(device);//也可以添加devices.getName()到列表，这里省略            }
                // callbackType：回调类型
                // result：扫描的结果，不包括传统蓝牙        }
                bluetoothDevices.add(device.getName() + ":"
                        + device.getAddress() + "\n");
                //更新字符串数组适配器，显示到listview中
                // arrayAdapter.notifyDataSetChanged();
                Message tempMsg = new Message();
                tempMsg.what = UPDATE_LIST;
                handler.sendMessage(tempMsg);
            }
        }
    };

    private Handler handler = new Handler(){

        public void handleMessage(Message msg){
            Button searchBtn = findViewById(R.id.searchBtn);
            switch (msg.what){
                case UPDATE_STATUS:
                    statusText.setText(msg.obj.toString());
                    searchBtn.setText("断开蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.cancelbutton);
                    break;
                case DISCONN_BLE:
                    statusText.setText("未连接到蓝牙");
                    searchBtn.setText("搜索蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.buttonshape);
                    break;
                case UPDATE_LIST:
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case ADD_NODE:
                    paintBoard.addNode(msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    };

    private void addNode(){
        EditText msg = findViewById(R.id.editText);
        String tmpStr = "addr"+msg.getText().toString()+"end";
        byte[] msgBytes = tmpStr.getBytes();
        try {
            mBLE.bluetoothGattCharacteristic.setValue(msgBytes);
            mBLE.bluetoothGatt.writeCharacteristic(mBLE.bluetoothGattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDistance(){
        EditText msg = findViewById(R.id.editText);
        String tmpStr = "dis"+msg.getText().toString()+"end";
        saveDistance = convertToFloat(msg.getText().toString(),0);
        byte[] msgBytes = tmpStr.getBytes();
        try {
            Log.e(TAG,"Distance is "+ saveDistance);
            if(saveDistance > rcvDis)
                distanceVibrator.cancel();
            else{
                long [] vibratePattern = {100,400,100,400}; // 停止 开启 停止 开启
                //第二个参数表示使用pattern第几个参数作为震动时间重复震动，如果是-1就震动一次
                distanceVibrator.vibrate(vibratePattern,2);
            }
            paintBoard.reDraw(saveDistance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mBLE.bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    @Override
    public void onItemClick(AdapterView<?>parent, View view, int position, long id) {
        mBLE.bluetoothDevice = mBLE.devices.get(position);
        mBLE.bluetoothGatt = mBLE.bluetoothDevice.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    alertDialog.dismiss();

                    Message tempMsg = new Message();
                    tempMsg.what = UPDATE_STATUS;
                    tempMsg.obj = "当前连接设备:"+mBLE.bluetoothDevice.getName();
                    handler.sendMessage(tempMsg);

                    //setTitle("成功建立连接");
                    //gatt.discoverServices(); //连接成功，开始搜索服务
                    try {
                        Thread.sleep(600);
                        Log.i(TAG, "Attempting to start service discovery:"
                                + gatt.discoverServices());
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Log.i(TAG, "Fail to start service discovery:");
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //setTitle("连接断开");
                }
                return;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                //此函数用于接收数据
                super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "Hi discovered!");
                String service_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
                String characteristic_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

                mBLE.bluetoothGattService = mBLE.bluetoothGatt.getService(UUID.fromString(service_UUID));
                mBLE.bluetoothGattCharacteristic = mBLE.bluetoothGattService.getCharacteristic(UUID.fromString(characteristic_UUID));

                if (mBLE.bluetoothGattCharacteristic != null) {
                    gatt.setCharacteristicNotification(mBLE.bluetoothGattCharacteristic, true); //用于接收数据
                    //Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                    for (BluetoothGattDescriptor dp : mBLE.bluetoothGattCharacteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((mBLE.bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((mBLE.bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                    Log.d(TAG, "服务连接成功");
                } else {
                    //Toast.makeText(MainActivity.this, "发现服务失败", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "服务失败");
                    return;
                }
                return;
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt,characteristic);
                //发现服务后的响应函数
                Log.e(TAG,"真的有收到东西！");
                byte[] bytesReceive = characteristic.getValue();
                String msgStr = new String(bytesReceive);

                String[] strs = msgStr.split("#");


                switch(strs[0]){
                    case "dis":
                        rcvDis = convertToFloat(strs[1],0);
                        Log.e(TAG,"接收到距离："+rcvDis);
                        break;
                    case "state":
                        statusCode = convertToInt(strs[1],0);
                        Log.e(TAG,"接收到状态:"+statusCode);
                        break;
                    default:
                        System.out.println("unknown");
                        break;
                }

                if(rcvDis > 0){
                    distanceArray.add(rcvDis);
                }
                if(isRecord){
                    mFileLogger.writeTxtToFile("当前距离:"+rcvDis,mFileLogger.getFilePath(),mFileLogger.getFileName());
                }
                //TODO:可以在此根据接收到的不同距离播放不同的内容
                /*if(rcvDis > 25 && rcvDis < 35){
                    String text="你好！这是30米";
                    speak(text);
                }*/
                switch(statusCode){
                    case 1:
                        String text="你好！";
                        speak(text);
                        break;
                    case 2:
                        String text1="正在远离";
                        speak(text1);
                        break;
                    case 3:
                        String text2="正在靠近";
                        speak(text2);
                        break;
                    case 4:
                        String text3="警报";
                        speak(text3);
                        break;
                    default:
                        System.out.println("unknown");
                        break;
                }
                //if(rcvDis <4){
                //   String text="你好！这是4米";
                //   speak(text);
                //}
                //if(rcvDis >20){
                //   String text="你好！这是20米";
                //   speak(text);
                // }
                if(rcvDis > saveDistance) {
                    long [] vibratePattern = {100,400,100,400}; // 停止 开启 停止 开启
                    //第二个参数表示使用pattern第几个参数作为震动时间重复震动，如果是-1就震动一次
                    distanceVibrator.vibrate(vibratePattern,2);
                }else{
                    distanceVibrator.cancel();
                }
                Message tempMsg = new Message();
                tempMsg.what = ADD_NODE;
                tempMsg.obj = strs[1];
                handler.sendMessage(tempMsg);
                // Log.e(TAG,msgStr);
                return;
            }
        });
    }
}
