package com.update.update_plugin.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.update.update_plugin.DownloadObserver;
import com.update.update_plugin.entity.AppUpdate;
import com.update.update_plugin.listener.MainPageExtraListener;
import com.update.update_plugin.listener.UpdateDialogListener;
import com.update.update_plugin.view.UpdateProgressDialog;
import com.update.update_plugin.view.UpdateRemindDialog;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * @author https://github.com/NewHuLe/AppUpdate
 * @date 2019/7/11 9:34
 * description: 下载更新工具类
 */
public class UpdateManager implements UpdateDialogListener {

    private static final String TAG = "UpdateManager";
    /**
     * 是否启动自动安装
     */
    public static boolean isAutoInstall;
    private final WeakReference<Context> wrfContext;
    private DownloadManager downloadManager;
    /**
     * 上次下载的id
     */
    private long lastDownloadId = -1;
    private AppUpdate appUpdate;
    /**
     * 下载与主页之间的通信
     */
    private MainPageExtraListener mainPageExtraListener;
    /**
     * 下载监听
     */
    private DownloadObserver downloadObserver;

    public UpdateManager(Context context) {
        wrfContext = new WeakReference<>(context);
    }

    /**
     * 开启下载更新
     *
     * @param appUpdate             更新数据
     * @param mainPageExtraListener 与当前页面交互的接口
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startUpdate(AppUpdate appUpdate, MainPageExtraListener mainPageExtraListener) {
        Context context = wrfContext.get();
        if (context == null) {
            throw new NullPointerException("UpdateManager======context不能为null，请先在构造方法中传入！");
        }
        if (appUpdate == null) {
            throw new NullPointerException("UpdateManager======appUpdate不能为null，请配置相关更新信息！");
        }
        this.appUpdate = appUpdate;
        isAutoInstall = appUpdate.getIsSlentMode();
        this.mainPageExtraListener = mainPageExtraListener;
        updateDownLoad();
    }

    /**
     * 获取实体性喜
     *
     * @return AppUpdate
     */
    public AppUpdate getAppUpdate() {
        return appUpdate;
    }

    /**
     * 下载apk
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void downLoadApk() {
        try {
            Context context = wrfContext.get();
            if (context != null) {
                if (!downLoadMangerIsEnable(context)) {
                    downFromBrowser();
                    return;
                }
                downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                clearCurrentTask();
                // 下载地址如果为null,抛出异常
                String downloadUrl = Objects.requireNonNull(appUpdate.getNewVersionUrl());
                Uri uri = Uri.parse(downloadUrl);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                // 下载中和下载完成显示通知栏
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                if (TextUtils.isEmpty(appUpdate.getSavePath())) {
                    //使用系统默认的下载路径 此处为应用内 /android/data/packages ,所以兼容7.0
                    request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, context.getPackageName() + ".apk");
                    deleteApkFile(Objects.requireNonNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + context.getPackageName() + ".apk")));
                } else {
                    // 自定义的下载目录,注意这是涉及到android Q的存储权限，建议不要用getExternalStorageDirectory（）
                    request.setDestinationInExternalFilesDir(context, appUpdate.getSavePath(), context.getPackageName() + ".apk");
                    // 清除本地缓存的文件
                    deleteApkFile(Objects.requireNonNull(context.getExternalFilesDir(appUpdate.getSavePath())));
                }
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                // 部分机型（暂时发现Nexus 6P）无法下载，猜测原因为默认下载通过计量网络连接造成的，通过动态判断一下
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    boolean activeNetworkMetered = connectivityManager.isActiveNetworkMetered();
                    request.setAllowedOverMetered(activeNetworkMetered);
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    request.allowScanningByMediaScanner();
                }
                // 设置通知栏的标题
                request.setTitle("应用更新");
                request.setDescription("应用正在下载中...");
                // 设置媒体类型为apk文件
                request.setMimeType("application/vnd.android.package-archive");
                lastDownloadId = downloadManager.enqueue(request);
                // 如需要进度及下载状态，增加下载监听,监听数据变化
                if (!appUpdate.getIsSlentMode()) {
                    DownloadHandler downloadHandler = new DownloadHandler(context, downloadObserver, mainPageExtraListener, downloadManager, lastDownloadId);
                    //下载消息给handle处理
                    downloadObserver = new DownloadObserver(downloadHandler, downloadManager, lastDownloadId);
                    context.getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, downloadObserver);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 防止有些厂商更改了系统的downloadManager
            downloadFromBrowse();
        }
    }

    /**
     * downloadManager 是否可用
     *
     * @param context 上下文
     * @return true 可用
     */
    private boolean downLoadMangerIsEnable(Context context) {
        int state = context.getApplicationContext().getPackageManager()
                .getApplicationEnabledSetting("com.android.providers.downloads");
        return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED);
    }

    /**
     * 下载前清空本地缓存的文件
     */
    private void deleteApkFile(File destFileDir) {
        if (!destFileDir.exists()) {
            return;
        }
        if (destFileDir.isDirectory()) {
            File[] files = destFileDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteApkFile(f);
                }
            }
        }
        destFileDir.delete();
    }

    /**
     * 从浏览器打开下载，暂时没有选择应用市场，因为应用市场太多，而且协议不同，无法兼顾所有
     */
    private void downloadFromBrowse() {
        try {
            String downloadUrl = TextUtils.isEmpty(appUpdate.getDownBrowserUrl()) ? appUpdate.getNewVersionUrl() : appUpdate.getDownBrowserUrl();
            Intent intent = new Intent();
            Uri uri = Uri.parse(downloadUrl);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            wrfContext.get().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "无法通过浏览器下载！");
        }
    }

    /**
     * 清除上一个任务，防止apk重复下载
     */
    private void clearCurrentTask() {
        try {
            if (lastDownloadId != -1) {
                downloadManager.remove(lastDownloadId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void forceExit() {
        // 回到退出整个应用
        if (mainPageExtraListener != null) {
            mainPageExtraListener.forceExit();
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void updateDownLoad() {
        downLoadApk();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void updateRetry() {
        // 开启下载
        downLoadApk();
    }

    @Override
    public void downFromBrowser() {
        // 从浏览器下载
        downloadFromBrowse();
    }

    @Override
    public void cancelUpdate() {
        // 取消更新
        clearCurrentTask();
    }

    /**
     * 重新安装app
     */
    public void installAppAgain() {
        Context context = wrfContext.get();
        if (context != null) {
            try {
                File downloadFile = getDownloadFile();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { //7.0
                    intent.setDataAndType(Uri.fromFile(downloadFile), "application/vnd.android.package-archive");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //8.0
                        boolean allowInstall = context.getPackageManager().canRequestPackageInstalls();
                        if (!allowInstall) {
                            //不允许安装未知来源应用，请求安装未知应用来源的权限
                            if (mainPageExtraListener != null) {
                                mainPageExtraListener.applyAndroidOInstall();
                            }
                            return;
                        }
                    }
                    //Android7.0之后获取uri要用contentProvider
                    Uri apkUri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".fileProvider", downloadFile);
                    //Granting Temporary Permissions to a URI
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


    /**
     * 获取下载的文件
     *
     * @return file
     */
    private File getDownloadFile() {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query.setFilterById(lastDownloadId));
        if (cursor != null && cursor.moveToFirst()) {
            String fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String apkPath = Uri.parse(fileUri).getPath();
            if (!TextUtils.isEmpty(apkPath)) {
                return new File(apkPath);
            }
            cursor.close();
        }
        return null;
    }

}
