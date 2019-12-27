package com.update.update_plugin.manage;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.update.update_plugin.entity.ResultMap;
import com.update.update_plugin.listener.MainPageExtraListener;
import com.update.update_plugin.utils.UpdateManager;
import com.update.update_plugin.utils.Md5Util;

import java.io.File;
import java.lang.ref.WeakReference;

import io.flutter.plugin.common.EventChannel;

import static com.update.update_plugin.utils.DownloadHandler.DOWNLOAD_STATUS;

//https://segmentfault.com/a/1190000017452056
public class ReceiveManage extends ContextWrapper {
    private final String TAG = getClass().getSimpleName();
    private final WeakReference<Context> wrfContext;
    private final WeakReference<MainPageExtraListener> wrfMainPageExtraListener;

    public ReceiveManage(Context base, MainPageExtraListener mainPageExtraListener) {
        super(base);
        wrfContext = new WeakReference<>(base);
        wrfMainPageExtraListener = new WeakReference<>(mainPageExtraListener);
    }

    public BroadcastReceiver createBroadcastReceiver(final EventChannel.EventSink eventSink) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null && intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
//                    timer.cancel();
//                    timer = null;
//                    long id = intent.getLongExtra("extra_download_id", 0);
//                    queryTask(id);
                } else if (intent != null && intent.getAction() != null && intent.getAction().equals(DOWNLOAD_STATUS)) {
                    int progress = intent.getIntExtra("progress", 0);
                    int status = intent.getIntExtra("status", 0);
                    long id = intent.getLongExtra("id", 0);
                    double percent = intent.getDoubleExtra("percent", 0);
                    int total = intent.getIntExtra("total", 0);
                    double planTime = intent.getDoubleExtra("planTime", 0);
                    double speed = intent.getDoubleExtra("speed", 0);
                    String address = intent.getStringExtra("address");
                    Log.d("receiver", "----------->" + percent);
                    eventSink.success(ResultMap.getInstance()
                            .pubClear("progress", progress)
                            .put("status", status)
                            .put("id", id)
                            .put("percent", percent)
                            .put("total", total)
                            .put("planTime", planTime)
                            .put("speed", speed)
                            .put("address", address)
                            .getMap());
                }
            }
        };
    }

    public void installApkById(UpdateManager updateUtils, int id) {
//        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//        return installApk(manager.getUriForDownloadedFile(id));
        downloadSuccess(updateUtils, id);
    }

    //安装apk
    public boolean installApk(Uri uri) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        if (uri != null) {
            Log.d("安装Uri", uri.toString());
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
            return true;
        } else {
            return false;
        }
    }


    /**
     * 下载成功
     */
    private void downloadSuccess(UpdateManager updateUtils, int lastDownloadId) {
        // 关闭进度框
//        UpdateProgressDialog progressDialog = wrfUpdateProgressDialog.get();
//        if (progressDialog != null && progressDialog.isShowing && wrfContext.get() != null && !((Activity) wrfContext.get()).isFinishing()) {
//            progressDialog.dismiss();
//        }
        // 获取下载的文件并安装
        DownloadManager.Query query = new DownloadManager.Query();
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager == null) return;
        Cursor cursor = manager.query(query.setFilterById(lastDownloadId));
        if (cursor != null && cursor.moveToFirst()) {
            String fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            cursor.close();
            String path = Uri.parse(fileUri).getPath();
            if (!TextUtils.isEmpty(path)) {
                File apkFile = new File(path);
                if (!TextUtils.isEmpty(updateUtils.getAppUpdate().getMd5())) {
                    boolean md5IsRight = Md5Util.checkFileMd5(updateUtils.getAppUpdate().getMd5(), apkFile);
                    if (!md5IsRight) {
                        if (wrfContext.get() != null) {
                            Toast.makeText(wrfContext.get(), "为了安全性和更好的体验，为你推荐浏览器下载更新！", Toast.LENGTH_SHORT).show();
                        }
                        downloadFromBrowse(updateUtils);
                        return;
                    }
                }
                installApp(apkFile);
            }
        }
    }

    /**
     * 从浏览器打开下载，暂时没有选择应用市场，因为应用市场太多，而且协议不同，无法兼顾所有
     */
    private void downloadFromBrowse(UpdateManager updateUtils) {
        try {
            Intent intent = new Intent();
            Uri uri = Uri.parse(updateUtils.getAppUpdate().getDownBrowserUrl());
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            wrfContext.get().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "无法通过浏览器下载！");
        }
    }

    /**
     * 安装app
     *
     * @param apkFile 下载的文件
     */
    private void installApp(File apkFile) {
        Context context = wrfContext.get();
        if (context != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        boolean allowInstall = context.getPackageManager().canRequestPackageInstalls();
                        if (!allowInstall) {
                            //不允许安装未知来源应用，请求安装未知应用来源的权限
                            if (wrfMainPageExtraListener.get() != null) {
                                wrfMainPageExtraListener.get().applyAndroidOInstall();
                            }
                            return;
                        }
                    }
                    //Android7.0之后获取uri要用contentProvider
                    Uri apkUri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".fileProvider", apkFile);
                    //// 该语句会授予临时的访问权限 否则会秒退
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "请点击通知栏完成应用的安装！", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
