package com.speedata.libid2;

import android.content.Context;
import android.serialport.DeviceControl;
import android.serialport.SerialPort;

import com.google.gson.Gson;

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
    private SerialPort mIDDev;
    private int fd;
    private static final String FIND_CARD = "aaaaaa96690003200122";
    private static final String CHOOSE_CARD = "aaaaaa96690003200221";
    private static final String READ_CARD = "aaaaaa96690003300132";
    private static final String READ_CARD_WITH_FINGER = "aaaaaa96690003301023";

    private static final int READ_LEN_WITHOUT_FINGER = 1295;
    //    int read_len_with_finger = 1295 + 1024;
    private static final int READ_NORMAL = 1024;
    private static final byte[] CMD_FIND_CARD = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0x96, 0x69, 0x00, 0x03, 0x20, 0x01, 0x22};
    private static final byte[] CMD_CHOOSE_CARD = {(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x96, 0x69, 0x00, 0x03, 0x20, 0x02, 0x21};
    private static final byte[] CMD_READ_CARD = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0x96, 0x69,
            0x00, 0x03, 0x30, 0x01, 0x32};
    private static final byte[] CMD_READ_CARD_WITH_FINGER = {(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte)
            0x96, 0x69,
            0x00, 0x03, 0x30, 0x10, 0x23};

    private Context mContext;
    private IDReadCallBack callBack;
    private ParseIDInfor parseIDInfor;
    private DeviceControl deviceControl;
    private boolean isNeedFingerprinter;

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
        mIDDev = new SerialPort();
        mIDDev.OpenSerial(serialport, braut);
        fd = mIDDev.getFd();
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
        DeviceControl.PowerType powerType = fileExists ? mConfig.getId2().getPowerType().equals("MAIN") ? DeviceControl.PowerType.MAIN
                : DeviceControl.PowerType.MAIN_AND_EXPAND : DeviceType.getPowerType();

        deviceControl = new DeviceControl(powerType, gpio);
        deviceControl.PowerOnDevice();
        mIDDev = new SerialPort();
        mIDDev.OpenSerial(serialport, braut);
        fd = mIDDev.getFd();
        return searchCard() != STATUE_SERIAL_NULL;
    }


    @Override
    public void releaseDev() throws IOException {
        mIDDev.CloseSerial(fd);
        deviceControl.PowerOffDevice();
    }


    @Override
    public int searchCard() {
//        mIDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(FIND_CARD));
        mIDDev.WriteSerialByte(fd, CMD_FIND_CARD);
        try {
            byte[] bytes = mIDDev.ReadSerial(fd, READ_NORMAL);
            if (bytes == null) {
                return STATUE_READ_NULL;
            } else {
                return parseIDInfor.checkPackage(bytes, bytes.length, true);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return STATUE_UNSUPPORTEDENCODINGEXCEPTION;
        }
    }


    @Override
    public int selectCard() {
//        mIDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(CHOOSE_CARD));
        mIDDev.WriteSerialByte(fd, CMD_CHOOSE_CARD);
        try {
            byte[] bytes = mIDDev.ReadSerial(fd, READ_NORMAL);
            if (bytes == null) {
                return STATUE_READ_NULL;
            } else {
                return parseIDInfor.checkPackage(bytes, bytes.length, true);
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
            IDInfor idInfor = new IDInfor();
            idInfor.setErrorMsg(parseReturnState(STATUE_SERIAL_NULL));
            idInfor.setSuccess(false);
            return idInfor;
        } else {
            int result = parseIDInfor.checkPackage(bytes, bytes.length, false);
            if (result != SELECT_CARD_OK && result != STATUE_OK) {
                IDInfor idInfor = new IDInfor();
                idInfor.setErrorMsg(parseReturnState(result) + "  " + result);
                idInfor.setSuccess(false);
                return idInfor;
            } else {
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

    /**
     * 发送读卡指令.
     *
     * @param isNeedFingerprinter 是否需要指纹
     * @return byte[]
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    private byte[] sendReadCmd(boolean isNeedFingerprinter) throws UnsupportedEncodingException {
        mIDDev.clearportbuf(fd);
        if (isNeedFingerprinter) {
//            mIDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(READ_CARD_WITH_FINGER));
            mIDDev.WriteSerialByte(fd, CMD_READ_CARD_WITH_FINGER);
        } else {
//            mIDDev.WriteSerialByte(fd, DataConversionUtils.HexString2Bytes(READ_CARD));
            mIDDev.WriteSerialByte(fd, CMD_READ_CARD);
        }

        byte[] bytes;
        if (!isNeedFingerprinter) {
            bytes = mIDDev.ReadSerial(fd, READ_LEN_WITHOUT_FINGER);
        } else {
            byte[] temp0 = mIDDev.ReadSerial(fd, READ_LEN_WITHOUT_FINGER, false);
            byte[] temp1 = mIDDev.ReadSerial(fd, READ_NORMAL, false);
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


    @Override
    public void getIDInfor(final boolean isNeedFingerprinter, boolean isLoop) {
        synchronized (this) {
            parseIDInfor.isGet = false;
            //寻卡成功之后才执行选卡和读卡
            this.isNeedFingerprinter = isNeedFingerprinter;
            if (isLoop) {
                readCard();
            }
        }
    }


    private void readCard() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //寻卡成功之后才执行选卡和读卡
                IDInfor idInfor;

                if (searchCard() != STATUE_OK_SEARCH) {
                    idInfor = new IDInfor();
                    idInfor.setSuccess(false);
                    idInfor.setErrorMsg(mContext.getString(R.string.states7));
                    callBack.callBack(idInfor);
                    return;
                }

                if (selectCard() != STATUE_OK) {
                    idInfor = new IDInfor();
                    idInfor.setSuccess(false);
                    idInfor.setErrorMsg(mContext.getString(R.string.states4));
                    callBack.callBack(idInfor);
                    return;
                }

                mIDDev.clearportbuf(fd);
                idInfor = readCard(isNeedFingerprinter);
                if (idInfor != null) {
                    if (!idInfor.isSuccess()) {
                        String errorMsg = parseReturnState(parseIDInfor.currentStatue);
                        idInfor.setErrorMsg(errorMsg);
                        callBack.callBack(idInfor);
                    } else {
                        idInfor.setSuccess(true);
                        callBack.callBack(idInfor);
                    }
                }


            }
        });
        thread.start();
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
