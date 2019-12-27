package com.update.update_plugin.utils;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import io.flutter.plugin.common.EventChannel;

import com.update.update_plugin.DownloadObserver;
import com.update.update_plugin.entity.AppUpdate;
import com.update.update_plugin.entity.DownloadBean;
import com.update.update_plugin.entity.ResultMap;
import com.update.update_plugin.listener.MainPageExtraListener;
import com.update.update_plugin.listener.UpdateDialogListener;
import com.update.update_plugin.view.UpdateFailureDialog;
import com.update.update_plugin.view.UpdateProgressDialog;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * @author hule
 * @date 2019/7/15 16:43
 * description:下载监听handler
 */
public class DownloadHandler extends Handler {

    //广播的action
    public static final String DOWNLOAD_STATUS = "com.update.update_plugin.DOWNLOAD_STATUS";
    private final String TAG = getClass().getSimpleName();
    private final WeakReference<Context> wrfContext;
    /**
     * 弱引用DownloadObserver
     */
    private final WeakReference<DownloadObserver> wrfDownloadObserver;
    /**
     * 弱引用与前台的通讯
     */
    private final WeakReference<MainPageExtraListener> wrfMainPageExtraListener;
    private final long lastDownloadId;
    private final DownloadManager downloadManager;

    public DownloadHandler(Context context, DownloadObserver downloadObserver,
                           MainPageExtraListener mainPageExtraListener, DownloadManager downloadManager, long lastDownloadId) {
        wrfContext = new WeakReference<>(context);
        wrfDownloadObserver = new WeakReference<>(downloadObserver);
        wrfMainPageExtraListener = new WeakReference<>(mainPageExtraListener);
        this.lastDownloadId = lastDownloadId;
        this.downloadManager = downloadManager;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        Intent intent = new Intent();
        switch (msg.what) {
            case DownloadManager.STATUS_PAUSED:
                // 暂停
                intent.setAction(DOWNLOAD_STATUS);
                intent.putExtra("status", DownloadStatus.STATUS_PAUSED.getValue());
                intent.putExtra("id", lastDownloadId);
                if (wrfContext.get() != null) {
                    wrfContext.get().sendBroadcast(intent);
                }
                break;
            case DownloadManager.STATUS_PENDING:
                // 开始
                intent.setAction(DOWNLOAD_STATUS);
                intent.putExtra("status", DownloadStatus.STATUS_PENDING.getValue());
                intent.putExtra("id", lastDownloadId);
                if (wrfContext.get() != null) {
                    wrfContext.get().sendBroadcast(intent);
                }
                break;
            case DownloadManager.STATUS_RUNNING:
                Bundle bundle = msg.getData();
                DownloadBean bean = bundle.getParcelable("DATA");
                //当前进度
//                double percent = msg.arg1 * 100 / msg.arg2;
                if (bean == null) return;
                intent.setAction(DOWNLOAD_STATUS);
                intent.putExtra("status", DownloadStatus.STATUS_RUNNING.getValue());
                intent.putExtra("progress", bean.getProgress());
                intent.putExtra("total", bean.getTotal());
                intent.putExtra("percent", bean.getPercent());
                intent.putExtra("id", lastDownloadId);
                intent.putExtra("planTime", bean.getPlanTime());
                intent.putExtra("speed", bean.getSpeed());
                intent.putExtra("address", bean.getAddress());
                if (wrfContext.get() != null) {
                    wrfContext.get().sendBroadcast(intent);
                }
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                // 取消监听的广播
                if (wrfContext.get() != null && wrfDownloadObserver.get() != null) {
                    wrfContext.get().getContentResolver().unregisterContentObserver(wrfDownloadObserver.get());
                }
                //下载成功,做200ms安装延迟
//                postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        downloadSuccess();
//                    }
//                }, 200);
                intent.setAction(DOWNLOAD_STATUS);
                intent.putExtra("status", DownloadStatus.STATUS_SUCCESSFUL.getValue());
                intent.putExtra("id", lastDownloadId);
                intent.putExtra("progress", 100);
                intent.putExtra("percent", 100.0);
                if (wrfContext.get() != null) {
                    wrfContext.get().sendBroadcast(intent);
                }
                break;
            case DownloadManager.STATUS_FAILED:
                try {
                    // 下载失败，清除本次的下载任务
                    if (lastDownloadId != -1) {
                        downloadManager.remove(lastDownloadId);
                    }
                    // 取消监听的广播
                    if (wrfContext.get() != null && wrfDownloadObserver.get() != null) {
                        wrfContext.get().getContentResolver().unregisterContentObserver(wrfDownloadObserver.get());
                    }
                    intent.setAction(DOWNLOAD_STATUS);
                    intent.putExtra("status", DownloadStatus.STATUS_FAILED.getValue());
                    intent.putExtra("id", lastDownloadId);
                    if (wrfContext.get() != null) {
                        wrfContext.get().sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
}
