package com.speedata.identity_as;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.speedata.libid2.IDInfor;
import com.speedata.libid2.IDManager;
import com.speedata.libid2.IDReadCallBack;
import com.speedata.libid2.IID2Service;
import com.speedata.libutils.ConfigUtils;
import com.speedata.libutils.ReadBean;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView tvIDInfor;
    private ImageView imgPic;


    //    private ImageView imgFinger;
    private CheckBox checkBoxFinger;
    private ToggleButton btnGet;
    private TextView tvMsg;

    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaySoundUtils.initSoundPool(this);
        initUI();
        initID();
        boolean isExit = ConfigUtils.isConfigFileExists();
        if (isExit)
            tvConfig.setText("定制配置：\n");
        else
            tvConfig.setText("标准配置：\n");
        ReadBean.Id2Bean pasm = ConfigUtils.readConfig(this).getId2();
        String gpio = "";
        List<Integer> gpio1 = pasm.getGpio();
        for (Integer s : gpio1) {
            gpio += s + ",";
        }
        tvConfig.append("串口:" + pasm.getSerialPort() + "  波特率：" + pasm.getBraut() + " 上电类型:" +
                pasm.getPowerType() + " GPIO:" + gpio);
    }


    private TextView tvConfig;
    private ImageView imageView;
    private TextView tvTime;

    private void initUI() {
        setContentView(R.layout.activity_main);
        tvTime = (TextView) findViewById(R.id.tv_time);
        imageView = (ImageView) findViewById(R.id.img_logo);
        tvConfig = (TextView) findViewById(R.id.tv_config);
        tvMsg = (TextView) findViewById(R.id.tv_msg);
        tvIDInfor = (TextView) findViewById(R.id.tv_idinfor);
        imgPic = (ImageView) findViewById(R.id.img_pic);
        btnGet = (ToggleButton) findViewById(R.id.btn_get);
        btnGet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                iid2Service.getIDInfor(false, b);
                if(b){
                    MyAnimation.showLogoAnimation(MainActivity.this,imageView);
                }else{
                    imageView.clearAnimation();
                }
            }
        });


        checkBoxFinger = (CheckBox) findViewById(R.id.checkbox_wit_finger);
    }

    private IID2Service iid2Service;


    private void clearUI() {
        tvIDInfor.setText("");
        imgPic.setImageBitmap(null);
    }


    private void initID() {
        iid2Service = IDManager.getInstance();
        try {
            boolean result = iid2Service.initDev(this, new IDReadCallBack() {
                @Override
                public void callBack(IDInfor infor) {
                    Message message = new Message();
                    message.obj = infor;
                    handler.sendMessage(message);
                }
            });

//            tvInfor.setText(String.format("s:%s b:115200 p:%s",
//                    DeviceType.getSerialPort().substring(DeviceType.getSerialPort().length() - 6,
//                            DeviceType.getSerialPort().length()),
//                    Arrays.toString(DeviceType.getGpio()).replace("[", "").replace("]", "")));
            if (!result) {
                new AlertDialog.Builder(this).setCancelable(false).setMessage("二代证模块初始化失败")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {


                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                btnGet.setEnabled(false);
                            }
                        }).show();
            } else {
                showToast("初始化成功");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long left_time = System.currentTimeMillis() - startTime;
            Log.d("Reginer", "time is: " + left_time);
            startTime = System.currentTimeMillis();
            iid2Service.getIDInfor(false, btnGet.isChecked());
//            clearUI();
            IDInfor idInfor1 = (IDInfor) msg.obj;

//            showToast("ok");
            if (idInfor1.isSuccess()) {
                Log.d("Reginer", "read success time is: " + left_time);
                PlaySoundUtils.play(1,1);
                tvTime.setText("耗时："+left_time+"ms");
                tvIDInfor.setText("姓名:" + idInfor1.getName() + "\n身份证号：" + idInfor1.getNum()
                        + "\n性别：" + idInfor1.getSex()
                        + "\n民族：" + idInfor1.getNation() + "\n住址:"
                        + idInfor1.getAddress() + "\n出生：" + idInfor1.getYear() + "年" + idInfor1
                        .getMonth() + "月" + idInfor1.getDay() + "日" + "\n有效期限：" + idInfor1
                        .getDeadLine());
                Bitmap bmps = idInfor1.getBmps();
                imgPic.setImageBitmap(bmps);
                tvMsg.setText("");
            } else {
                tvMsg.setText(String.format("ERROR:%s", idInfor1.getErrorMsg()));
            }
        }
    };

    @SuppressWarnings("unused")
    private Bitmap ShowFingerBitmap(byte[] image, int width, int height) {
        if (width == 0) return null;
        if (height == 0) return null;

        int[] RGBbits = new int[width * height];
//        viewFinger.invalidate();
        for (int i = 0; i < width * height; i++) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v = 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        return Bitmap.createBitmap(RGBbits, width, height, Bitmap.Config.RGB_565);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        try {
            if (iid2Service != null)
                iid2Service.releaseDev();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
