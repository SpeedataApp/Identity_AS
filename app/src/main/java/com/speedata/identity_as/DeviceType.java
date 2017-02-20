package com.speedata.identity_as;

import android.serialport.DeviceControl;
import android.serialport.SerialPort;

/**
 * ----------Dragon be here!----------/
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛
 * 　　　　┃　　　┃神兽保佑
 * 　　　　┃　　　┃代码无BUG！
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * ━━━━━━神兽出没━━━━━━
 * 创   建:Reginer in  2017/2/20 14:00.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
class DeviceType {
    static String getSerialPort() {
        switch (android.os.Build.MODEL) {
            case "kt45":
                return SerialPort.SERIAL_TTYMT1;
            case "kt45q":
                return SerialPort.SERIAL_TTYMT1;
            case "kt50":
                return SerialPort.SERIAL_TTYMT1;
            case "kt55":
                return SerialPort.SERIAL_TTYMT2;

        }

        return SerialPort.SERIAL_TTYMT1;

    }

    static DeviceControl.PowerType getPowerType() {
        switch (android.os.Build.MODEL) {
            case "kt45":
                return DeviceControl.PowerType.MAIN;
            case "kt45q":
                return DeviceControl.PowerType.MAIN;
            case "kt50":
                return DeviceControl.PowerType.MAIN;
            case "kt55":
                return DeviceControl.PowerType.MAIN_AND_EXPAND;
        }
        return DeviceControl.PowerType.MAIN;
    }

    static int[] getGpio() {
        switch (android.os.Build.MODEL) {
            case "kt45":
                return new int[]{106};
            case "kt45q":
                return new int[]{94};
            case "kt50":
                return new int[]{93};
            case "kt55":
                return new int[]{88, 6};
        }
        return new int[]{106};
    }
}
