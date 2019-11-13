package com.speedata.identity_as;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.serialport.DeviceControlSpd;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.speedata.utils.ProgressDialogUtils;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    PermissionListener listener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {

        }

        @Override
        public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
            // 用户否勾选了不再提示并且拒绝了权限，那么提示用户到设置中授权。
            if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, deniedPermissions)) {
                AndPermission.defaultSettingDialog(MainActivity.this, 300).show();
            }
        }
    };
    private TextView tvIDInfor;
    private ImageView imgPic;

    private ToggleButton btnGet;
    private TextView tvMsg;

    private long startTime;
    private TextView tvConfig;
    private ImageView imageView;
    private TextView tvTime;
    private TextView tvInitTime;
    private IID2Service iid2Service;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long left_time = System.currentTimeMillis() - startTime;
            Log.d("Reginer", "time is: " + left_time);
            startTime = System.currentTimeMillis();
            iid2Service.getIDInfor(false, btnGet.isChecked());

            IDInfor idInfor1 = (IDInfor) msg.obj;

            if (idInfor1.isSuccess()) {
                Log.d("Reginer", "read success time is: " + left_time);
                PlaySoundUtils.play(1, 1);
                tvTime.setText("耗时：" + left_time + "ms");
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
                tvMsg.setText(String.format("ERROR:%s", idInfor1.getErrorMsg()) + left_time + "ms");
            }
        }
    };
    /**
     * 清除信息
     */
    private Button mBtnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaySoundUtils.initSoundPool(this);
        permission();
        initUI();
        initView();
        boolean isExit = ConfigUtils.isConfigFileExists();
        if (isExit) {
            tvConfig.setText("定制配置：\n");
        } else {
            tvConfig.setText("标准配置：\n");
        }
        ReadBean.Id2Bean pasm = ConfigUtils.readConfig(this).getId2();
        String gpio = "";
        List<Integer> gpio1 = pasm.getGpio();
        for (Integer s : gpio1) {
            gpio += s + ",";
        }
        tvConfig.append("串口:" + pasm.getSerialPort() + "  波特率：" + pasm.getBraut() + " 上电类型:" +
                pasm.getPowerType() + " GPIO:" + gpio);
        //        tvConfig.append("串口:" + "ttyMT1" + "  波特率：" + "115200" + " 上电类型:" +
        //                "NEW_MAIN" + " GPIO:" + "28 75");
        initID();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //        initID();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initUI() {
        setContentView(R.layout.activity_main);
        tvTime = (TextView) findViewById(R.id.tv_time);
        tvInitTime = (TextView) findViewById(R.id.tv_init_time);
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
                if (b) {
                    MyAnimation.showLogoAnimation(MainActivity.this, imageView);
                    startTime = System.currentTimeMillis();
                } else {
                    imageView.clearAnimation();
                }
            }
        });
    }

    private void clearUI() {
        tvIDInfor.setText("");
        imgPic.setImageBitmap(null);
    }

    private void permission() {
        AndPermission.with(this).permission(Manifest.permission.READ_EXTERNAL_STORAGE).callback(listener).rationale(new RationaleListener() {
            @Override
            public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                AndPermission.rationaleDialog(MainActivity.this, rationale).show();
            }
        }).start();
    }

    private void initID() {
        ProgressDialogUtils.showProgressDialog(this, "正在初始化");
        new Thread(new Runnable() {
            @Override
            public void run() {
                iid2Service = IDManager.getInstance();
                try {
                    long temp = System.currentTimeMillis();

                    final boolean result = iid2Service.initDev(MainActivity.this
                            , new IDReadCallBack() {
                                @Override
                                public void callBack(IDInfor infor) {
                                    Message message = new Message();
                                    message.obj = infor;
                                    handler.sendMessage(message);
                                }
                            });
                    //                            }, "dev/ttyMT1", 115200, DeviceControlSpd.PowerType.NEW_MAIN, 28, 75);
                    //                            },"/dev/ttyMT1",115200, DeviceControlSpd.PowerType.MAIN,new int[]{93});
                    long costTime = System.currentTimeMillis() - temp;
                    showResult(result, "", costTime);

                } catch (IOException e) {
                    showResult(false, e.getMessage(), 0);
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void showResult(final boolean result, final String msg, final long time) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              ProgressDialogUtils.dismissProgressDialog();
                              if (!result) {
                                  new AlertDialog.Builder(MainActivity.this).setCancelable(false).setMessage("二代证模块初始化失败,请前往工具中修改参数" + msg)
                                          .setPositiveButton("确定", new DialogInterface.OnClickListener() {


                                              @Override
                                              public void onClick(DialogInterface dialogInterface, int i) {
                                                  btnGet.setEnabled(false);
                                                  openConfig();

                                              }
                                          }).show();
                              } else {
                                  showToast("初始化成功");
                                  btnGet.setChecked(true);
                                  tvInitTime.setText("初始化时间:" + time);
                              }
                          }
                      }
        );
    }

    /**
     * 打开调试工具  修改配置
     */
    private void openConfig() {
        //打开失败去下载
        try {
            Intent intent = new Intent();
            intent.setAction("speedata.config");
            startActivity(intent);
        } catch (Exception e) {
            //            downLoadDeviceApp();
            new AlertDialog.Builder(MainActivity.this).setCancelable(false).setMessage("请去应用市场下载思必拓调试工具进行配置")
                    .setPositiveButton("确定", null).show();
        }

    }


    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        try {
            //退出 释放二代证模块
            if (iid2Service != null) {
                iid2Service.releaseDev();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void initView() {
        mBtnClear = (Button) findViewById(R.id.btn_clear);
        mBtnClear.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_clear:
                tvTime.setText("耗时：" + 0 + "ms");
                tvIDInfor.setText("");
                imgPic.setImageBitmap(null);
                break;
        }
    }
}
