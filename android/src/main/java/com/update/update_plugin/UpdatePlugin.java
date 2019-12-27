package com.update.update_plugin;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;

import com.update.update_plugin.entity.AppUpdate;
import com.update.update_plugin.listener.MainPageExtraListener;
import com.update.update_plugin.manage.ReceiveManage;
import com.update.update_plugin.utils.UpdateManager;
import com.update.update_plugin.utils.PermissionsResult;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static com.update.update_plugin.utils.DownloadHandler.DOWNLOAD_STATUS;

/**
 * UpdatePlugin
 */
public class UpdatePlugin implements FlutterPlugin, MethodCallHandler,
        MainPageExtraListener, EventChannel.StreamHandler, ActivityAware {
    // 8.0未知应用
    public static final int INSTALL_PACKAGES_REQUESTCODE = 1112;
    public static final int GET_UNKNOWN_APP_SOURCES = 1113;
    private static MethodChannel channel;
    private static EventChannel eventChannel;
    private static UpdateManager updateUtils;
    private static ReceiveManage receiveManage;
    private Activity activity;
    //    API等级19：Android 4.4 KitKat
//    API等级20：Android 4.4W
//    API等级21：Android 5.0 Lollipop
//    API等级22：Android 5.1 Lollipop
//    API等级23：Android 6.0 Marshmallow
//    API等级24：Android 7.0 Nougat
//    API等级25：Android 7.1 Nougat
//    API等级26：Android 8.0 Oreo
//    API等级27：Android 8.1 Oreo
//    API等级28：Android 9.0 Pie
    ///////////////////////////////////////////
    // 实现 StreamHandler 需要重写 onListen 和 onCancel 方法
    // onListen 不会每次数据改变就会调用，只在 Flutter 层，eventChannel 订阅广播
    // 的时候调用，当取消订阅的时候则会调用 onCancel，
    // 所以当开始订阅数据的时候，注册接收数据变化的关闭，
    // 在取消订阅的时候，将注册的广播注销，防止内存泄漏
    private BroadcastReceiver downloadReceiver;

    public static void showToast(String msg) {
        System.out.println(msg);
        channel.invokeMethod("showToast", msg);
    }

    /**
     * 关联引擎
     *
     * @param binding
     */
    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getFlutterEngine().getDartExecutor(), "update_plugin");
        channel.setMethodCallHandler(new UpdatePlugin());
        eventChannel = new EventChannel(binding.getFlutterEngine().getDartExecutor(), "update_plugin/s");
        eventChannel.setStreamHandler(new UpdatePlugin());
    }

//http://appdl.hicloud.com/dl/appdl/application/apk/66/66b10ac29c9549cb892a5c430b1c090e/com.sqparking.park.1901261600.apk?mkey=5c516e787ae099f7&f=9e4a&sign=portal@portal1548830859005&source=portalsite

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
        eventChannel.setStreamHandler(null);
        eventChannel = null;
    }

    //BasicMessageChannel 用于传递字符串和半结构化的信息。
    //MethodChannel 用于传递方法调用（method invocation）
    //EventChannel 用于数据流（event streams）的通信
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.endsWith("downloadApk")) {
            checkUpdate(call.arguments.toString());
//            DownLoadUtils.downLoadApk(registrar.activity(), call.arguments.toString(),permisson,result);
//            UpdateRemindDialog updateRemindDialog = UpdateRemindDialog.newInstance(null);
//            updateRemindDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), "UpdateManager");
        } else if (call.method.equals("cancelUpdate")) {
            //取消更新
            updateUtils.cancelUpdate();
        } else if (call.method.equals("installApk")) {
            receiveManage.installApkById(updateUtils, (int) call.argument("id"));
        } else {
            result.notImplemented();
        }
    }

    /**
     * 检查更新 https://github.com/NewHuLe/AppUpdate
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void checkUpdate(String downLoadUrl) {
        AppUpdate appUpdate = new AppUpdate.Builder()
                //更新地址
                .newVersionUrl(downLoadUrl)
                // 版本号
                .newVersionCode("v1.2")
                // 通过传入资源id来自定义更新对话框，注意取消更新的id要定义为btnUpdateLater，立即更新的id要定义为btnUpdateNow
//                .updateResourceId(R.layout.dialog_update)
                // 文件大小
                .fileSize("5.8M")
                //是否采取静默下载模式（只显示更新提示，后台下载完自动弹出安装界面），否则，显示下载进度，显示下载失败弹框
                //风格：true代表默认静默下载模式，只弹出下载更新框,下载完毕自动安装， false 代表配合使用进度框与下载失败弹框
                .isSilentMode(false)
                //默认不采取强制更新，否则，不更新无法使用
                .forceUpdate(0)
                .build();
        updateUtils.startUpdate(appUpdate, this);
    }

    @Override
    public void forceExit() {
        // 如果使用到了强制退出，需要自己控制
//    finish();
//    System.exit(0);
    }

    /**
     * 检测到无权限安装未知来源应用，回调接口中需要重新请求安装未知应用来源的权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void applyAndroidOInstall() {
        //请求安装未知应用来源的权限
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DOWNLOAD_STATUS);
        downloadReceiver = receiveManage.createBroadcastReceiver(eventSink);
        receiveManage.registerReceiver(downloadReceiver, filter);
    }

    @Override
    public void onCancel(Object o) {
        if (downloadReceiver != null) {
            receiveManage.unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }

    /**
     * 关联到Activity
     *
     * @param binding
     */
    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        receiveManage = new ReceiveManage(activity.getApplicationContext(), this);
        updateUtils = new UpdateManager(activity);
        PermissionsResult permissionsResult = new PermissionsResult(activity, updateUtils);
        binding.addActivityResultListener(permissionsResult);
        binding.addRequestPermissionsResultListener(permissionsResult);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {
        if (downloadReceiver != null) {
            receiveManage.unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }
}
