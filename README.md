# Identity
-  导入依赖库
1.在项目的build.gradle中添加以下

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
 
```
module的build.gradle中的dependencies中添加

```
 dependencies {
        implementation 'com.github.SpeedataG:IdentityCard:2.6.1'
        implementation 'com.github.SpeedataG:Device:1.6.8'
  }
```
**Eclipse** 需导入libs库 LibDevice 和 LibIdentity
依赖以上两个lib库  运行时编译即可

1. initDev 初始化设备
1. releaseDev() 释放设备
1. getIDInfor 获取身份证信息


IID2Service获取对象


**获取实例**
```
 {
      IID2Service iid2Service = IDManager.getInstance();
 }
```

-  initDev

函数原型|boolean initDev(Context mContext, IDReadCallBack callBack, String serialport, int braut, DeviceControl.PowerType power_type,int… gpio) throws IOException                                  |
-------    |-------
功能描述  |初始化二代证模块
|参数描述  |Context mContext 上下文 |
|参数描述  |IDReadCallBack callBack 回调函数 |
|参数描述  |String serialport 串口 |
|参数描述  |int braut 波特率 |
|参数描述  |PowerType power_type 上电类型 |
|参数描述  |String serialport 串口 |
|返回类型  |boolean 上电结果|

 **初始化示例**

 ```
   {
   //串口2 波特率115200 主板+外部扩展上电 主板gpio=88 外部gpio=6
    boolean result = iid2Service.initDev(this, new IDReadCallBack() {
                            @Override
                            public void callBack(IDInfor infor) {
                                Message message = new Message();
                                message.obj = infor;
                                handler.sendMessage(message);
                            }
                        },
                       SerialPort.SERIAL_TTYMT1, 115200, DeviceControl.PowerType.MAIN, 94);//kt45Q 配置参数
                       //SerialPort.SERIAL_TTYMT2, 115200, DeviceControl.PowerType.MAIN_AND_EXPAND, //88,6);//kt55 背夹参数
   }
 ```
或者 ：
```
boolean result = iid2Service.initDev(this, new IDReadCallBack() {
                @Override
                public void callBack(IDInfor infor) {
                    Message message = new Message();
                    message.obj = infor;
                    handler.sendMessage(message);
                }
            });
```

 ------------


-  DeviceControl.PowerType 枚举



|字段|说明|
|:----    |:-------    |
|MAIN  |主板上电    |
|EXPAND |外部扩展 |
|MAIN_AND_EXPAND |主板+外部扩展 |

-  获取身份证信息

函数原型|void getIDInfor(boolean isNeedFingerprinter)                                  |
-------    |-------
|功能描述  |获取身份信息|
|参数描述  |boolean isNeedFingerprinter 是否需要指纹  |
|返回类型  |IDInfor 身份信息实体类|

**调用主板上电和外部扩展上电示例**

```

  iid2Service.getIDInfor(checkBoxFinger.isChecked());
```

-  释放设备

|函数原型|void releaseDev() throws IOException	                                   |
-------    |-------
|功能描述  |释放设备|
|参数描述  |无  程序退出时需调用此方法|
|返回类型  |无  |



北京思必拓科技股份有限公司

网址 http://www.speedata.cn/

技术支持 电话：155 4266 8023

QQ：2480737278
