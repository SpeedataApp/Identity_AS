package com.speedata.libid2;

import android.content.Context;
import android.os.SystemClock;
import android.serialport.DeviceControl;
import android.serialport.SerialPort;

import com.google.gson.Gson;
import com.speedata.libid2.utils.DataConversionUtils;
import com.speedata.libid2.utils.MyLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.speedata.libid2.ParseIDInfor.READ_CARD_FAILED;
import static com.speedata.libid2.ParseIDInfor.READ_CARD_NOT_SPOT;
import static com.speedata.libid2.ParseIDInfor.SELECT_CARD_OK;
import static com.speedata.libid2.ParseIDInfor.STATUE_ERROR_CHECK;
import static com.speedata.libid2.ParseIDInfor.STATUE_ERROR_HEAD;
import static com.speedata.libid2.ParseIDInfor.STATUE_ERROR_LEN;
import static com.speedata.libid2.ParseIDInfor.STATUE_ERROR_SEARCH;
import static com.speedata.libid2.ParseIDInfor.STATUE_ERROR_SELECT;
import static com.speedata.libid2.ParseIDInfor.STATUE_OK;
import static com.speedata.libid2.ParseIDInfor.STATUE_OK_SEARCH;
import static com.speedata.libid2.ParseIDInfor.STATUE_READ_NULL;
import static com.speedata.libid2.ParseIDInfor.STATUE_SERIAL_NULL;
import static com.speedata.libid2.ParseIDInfor.STATUE_UNSUPPORTEDENCODINGEXCEPTION;

/**
 * Created by brxu on 2016/12/15.
 */

public class HuaXuID implements IID2Service {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 360;
    private static final String TAG = "HuaXuID";
    private MyLogger logger = MyLogger.jLog();
    private SerialPort IDDev;
    private int fd;
    private static final String FindCard = "aaaaaa96690003200122";
    private static final String ChooseCard = "aaaaaa96690003200221";
    private static final String ReadCard = "aaaaaa96690003300132";
    private static final String ReadCardWithFinger = "aaaaaa96690003301023";
    private byte[] CMD_FIND_CARD = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0x96, 0x69, 0x00,
            0x03, 0x20, 0x01, 0x22};
    //    private byte[] CMD_CHOOSE_CARD = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0x96,
    // 0x69,
    private byte[] CMD_READ_CARD = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0x96, 0x69,
            0x00, 0x03, 0x30, 0x01, 0x32};
    private byte[] CMD_READ_CARD_WITH_FINGER = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte)
            0x96, 0x69,
            0x00, 0x03, 0x30, 0x10, 0x23};

    private Context mContext;
    private IDReadCallBack callBack;
    private ParseIDInfor parseIDInfor;
    private DeviceControl deviceControl;

    @Override
    public boolean initDev(Context mContext, IDReadCallBack callBack, String serialport, int braut,
                           DeviceControl.PowerType power_type,
                           int... gpio) throws
            IOException {

        parseIDInfor = new ParseIDInfor(mContext);
        this.mContext = mContext;
        this.callBack = callBack;
        deviceControl = new DeviceControl(power_type, gpio);
        deviceControl.PowerOnDevice();
        IDDev = new SerialPort();
        IDDev.OpenSerial(serialport, braut);
        fd = IDDev.getFd();
        logger.e("fd= " + fd);
        return searchCard() != STATUE_SERIAL_NULL;
    }

    @Override
    public boolean initDev(Context context, IDReadCallBack callBack) throws IOException {


        Config mConfig = null;
        int[] intArray = new int[0];
        parseIDInfor = new ParseIDInfor(context);
        this.mContext = context;
        this.callBack = callBack;
        boolean fileExists = FileUtils.fileExists();
        if (fileExists) {
            mConfig = new Gson().fromJson(FileUtils.readTxtFile(), Config.class);
            intArray = new int[mConfig.getId2().getGpio().size()];
            for (int i = 0; i < mConfig.getId2().getGpio().size(); i++) {
                intArray[i] = mConfig.getId2().getGpio().get(i);
            }
        }
        int[] gpio = fileExists ? intArray : DeviceType.getGpio();
        String serialport = fileExists ? mConfig.getId2().getSerialPort() : DeviceType.getSerialPort();
        int braut = fileExists ? mConfig.getId2().getBraut() : 115200;
        DeviceControl.PowerType power_type = fileExists ? mConfig.getId2().getPowerType().equals("MAIN") ? DeviceControl.PowerType.MAIN : DeviceControl.PowerType.MAIN_AND_EXPAND :
                DeviceType.getPowerType();

        deviceControl = new DeviceControl(power_type, gpio);
        deviceControl.PowerOnDevice();
        IDDev = new SerialPort();
        IDDev.OpenSerial(serialport, braut);
        fd = IDDev.getFd();
        return searchCard() != STATUE_SERIAL_NULL;
    }


    @Override
    public void releaseDev() throws IOException {
        IDDev.CloseSerial(fd);
        deviceControl.PowerOffDevice();
    }

    int read_len_without_finger = 1295;
    //    int read_len_with_finger = 1295 + 1024;
    int read_normal = 1024;

    @Override
    public int searchCard() {
        IDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(FindCard));
        try {
            SystemClock.sleep(sleep);

            byte[] bytes = IDDev.ReadSerial(fd, read_normal);
            logger.e("fd= " + fd);
            if (bytes == null) {
                logger.e("searchCard read null return");
                return STATUE_READ_NULL;
            } else {
                int result = parseIDInfor.checkPackage(bytes, bytes.length, true);
                logger.d("===searchCard result== " + result);
                if (result != STATUE_OK_SEARCH && result != SELECT_CARD_OK) {
                    logger.e("searchCard failed");
                } else {
                    logger.d("searchCard OK");
                }
                return result;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return STATUE_UNSUPPORTEDENCODINGEXCEPTION;
        }
    }

    int sleep = 300;

    @Override
    public int selectCard() {
        IDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(ChooseCard));
        try {

            SystemClock.sleep(sleep);
            byte[] bytes = IDDev.ReadSerial(fd, read_normal);
            if (bytes == null) {
                logger.e("selectCard read null return");
                return STATUE_READ_NULL;
            } else {
                int result = parseIDInfor.checkPackage(bytes, bytes.length, true);
                if (result != STATUE_OK) {
                    logger.e("selectCard failed " + result);
                } else {
                    logger.d("selectCard OK");
                }
                return result;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return STATUE_UNSUPPORTEDENCODINGEXCEPTION;
        }

    }


    @Override
    public IDInfor readCard(final boolean isNeedFingerprinter) {
        byte[] bytes = new byte[0];
        try {
            bytes = sendReadCmd(isNeedFingerprinter);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes == null || bytes.length == 0) {
            logger.e("readCard read null return");
            IDInfor idInfor = new IDInfor();
            idInfor.setErrorMsg(parseReturnState(STATUE_SERIAL_NULL));
            idInfor.setSuccess(false);
            return idInfor;
        } else {
            int result = parseIDInfor.checkPackage(bytes, bytes.length, false);
            if (result != SELECT_CARD_OK && result != STATUE_OK) {
                logger.e("readCard failed");
                IDInfor idInfor = new IDInfor();
                idInfor.setErrorMsg(parseReturnState(result) + "  " + result);
                idInfor.setSuccess(false);
                return idInfor;
            } else {
                logger.d("readCard OK");
                IDInfor idInfor;
                idInfor = parseIDInfor.parseIDInfor(bytes, isNeedFingerprinter);
                if (idInfor != null)
                    idInfor.setSuccess(true);
                else {
                    idInfor = new IDInfor();
                    idInfor.setErrorMsg("解析错误");
                    idInfor.setSuccess(false);
                }
                return idInfor;
            }
        }
    }

    private byte[] sendReadCmd(boolean isNeedFingerprinter) throws UnsupportedEncodingException {
        if (isNeedFingerprinter) {
            IDDev.clearportbuf(fd);
            IDDev.clearportbuf(fd);
            IDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(ReadCardWithFinger));
        } else {
            IDDev.clearportbuf(fd);
            IDDev.clearportbuf(fd);
            IDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(ReadCard));
        }
        SystemClock.sleep(sleep);

        byte[] bytes;
        if (!isNeedFingerprinter) {
            bytes = IDDev.ReadSerial(fd, read_len_without_finger);
//            if (bytes != null)
//                logger.e("===readcard len=" + DataConversionUtils.byteArrayToStringLog(bytes,
//                        bytes.length));
        } else {
            byte[] temp0 = IDDev.ReadSerial(fd, read_len_without_finger, false);
            byte[] temp1 = IDDev.ReadSerial(fd, read_normal, false);
            int len1 = 0;
            int len2 = 0;
            if (temp0 != null)
                len1 = temp0.length;
            if (temp1 != null)
                len2 = temp1.length;
            bytes = new byte[len1 + len2];
            if (temp0 != null) {
                System.arraycopy(temp0, 0, bytes, 0, temp0.length);
            }
            if (temp1 != null && temp0 != null)
                System.arraycopy(temp1, 0, bytes, temp0.length, temp1.length);
        }
        return bytes;
    }


    private IDInfor idInfor = null;

    @Override
    public void getIDInfor(final boolean isNeedFingerprinter) {
        synchronized (this) {
            parseIDInfor.isGet = false;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int result = searchCard();
                    //寻卡成功之后才执行选卡和读卡
                    if (result == STATUE_OK_SEARCH) {

                        if (selectCard() == STATUE_OK) {
                            SystemClock.sleep(200);
                            IDDev.clearportbuf(fd);
                            idInfor = readCard(isNeedFingerprinter);
                            if (idInfor != null) {
                                if (!idInfor.isSuccess()) {
                                    String errorMsg = parseReturnState(parseIDInfor.currentStatue);
                                    idInfor.setErrorMsg(errorMsg);
                                    logger.e("---ErrorMsg--" + errorMsg);
                                    callBack.callBack(idInfor);
                                } else {
                                    idInfor.setSuccess(true);
                                    callBack.callBack(idInfor);
                                }
                            }
                        } else {
                            idInfor = new IDInfor();
                            idInfor.setSuccess(false);
                            idInfor.setErrorMsg(mContext.getString(R.string.states4));
                            callBack.callBack(idInfor);
                        }
                    } else {
                        idInfor = new IDInfor();
                        idInfor.setSuccess(false);
                        idInfor.setErrorMsg(mContext.getString(R.string.states7));
                        callBack.callBack(idInfor);
                    }
                }
            });
            thread.start();
//            return idInfor;
        }
    }

    @Override
    public String parseReturnState(int state) {
        String result = "";
        switch (state) {
            case STATUE_ERROR_HEAD:
                result = mContext.getResources().getString(R.string.states1_heard_error);
                break;
            case STATUE_ERROR_CHECK:
                result = mContext.getResources().getString(R.string.states2_check_error);
                break;
            case STATUE_ERROR_LEN:
                result = mContext.getResources().getString(R.string.states3);
                break;
            case STATUE_ERROR_SELECT:
                result = mContext.getResources().getString(R.string.states4);
                break;
            case STATUE_OK:
                result = mContext.getResources().getString(R.string.states5);
                break;
            case STATUE_OK_SEARCH:
                result = mContext.getResources().getString(R.string.states6);
                break;
            case STATUE_ERROR_SEARCH:
                result = mContext.getResources().getString(R.string.states7);
                break;
            case SELECT_CARD_OK:
                result = mContext.getResources().getString(R.string.states8);
                break;
            case READ_CARD_FAILED:
                result = mContext.getResources().getString(R.string.states9);
                break;
            case READ_CARD_NOT_SPOT:
                result = mContext.getResources().getString(R.string.states10);
                break;
            case STATUE_READ_NULL:
                result = mContext.getResources().getString(R.string.states11);
                break;
            case STATUE_UNSUPPORTEDENCODINGEXCEPTION:
                result = mContext.getResources().getString(R.string.states12);
                break;
            default:
                result = "请确认背夹电池电量和初始化参数是否正确";
                break;
        }
        return result;
    }


}
