package com.update.update_plugin.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import io.flutter.plugin.common.PluginRegistry;

import static com.update.update_plugin.UpdatePlugin.GET_UNKNOWN_APP_SOURCES;
import static com.update.update_plugin.UpdatePlugin.INSTALL_PACKAGES_REQUESTCODE;

public class PermissionsResult implements PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {

    private final Activity activity;
    private UpdateManager updateUtils;

    public PermissionsResult(Activity activity, UpdateManager updateUtils) {
        this.activity=activity;
        this.updateUtils = updateUtils;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //8.0应用设置界面未知安装开源返回时候
        if (requestCode == GET_UNKNOWN_APP_SOURCES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean allowInstall = activity.getPackageManager().canRequestPackageInstalls();
                if (allowInstall) {
                    installApkAgain();
                } else {
                    Toast.makeText(activity, "您拒绝了安装未知来源应用，应用暂时无法更新！", Toast.LENGTH_SHORT).show();
                    if (0 != updateUtils.getAppUpdate().getForceUpdate()) {
                        updateUtils.forceExit();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 8.0的权限请求结果回调
        if (requestCode == INSTALL_PACKAGES_REQUESTCODE) {
            // 授权成功
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                installApkAgain();
            } else {
                // 授权失败，引导用户去未知应用安装的界面
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    //注意这个是8.0新API
                    Uri packageUri = Uri.parse("package:" + activity.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
                    activity.startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                }
            }
        }
        return false;
    }

    private void installApkAgain() {
        if (updateUtils != null) {
            updateUtils.installAppAgain();
        }
    }

}
