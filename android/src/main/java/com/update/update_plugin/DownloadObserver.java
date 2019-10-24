package com.update.update_plugin;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.update.update_plugin.entity.DownloadBean;

/**
 * @author hule
 * @date 2019/7/11 10:38
 * description:通过ContentObserver监听下载的进度
 */
public class DownloadObserver extends ContentObserver {

    private final String TAG = getClass().getCanonicalName();

    private final Handler handler;
    private final DownloadManager downloadManager;
    private final DownloadManager.Query query;
    //速度
    private double lastProgress = 0;
    //最后更新的时间
    private long lastTime = 0;
    /**
     * 记录成功或者失败的状态，主要用来只发送一次成功或者失败
     */
    private boolean isEnd = false;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DownloadObserver(Handler handler, DownloadManager downloadManager, long downloadId) {
        super(handler);
        this.handler = handler;
        this.downloadManager = downloadManager;
        query = new DownloadManager.Query().setFilterById(downloadId);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        queryDownloadStatus();
    }

    /**
     * 检查下载的状态
     */
    private void queryDownloadStatus() {
        Cursor cursor = downloadManager.query(query);
        DownloadBean downloadBean = new DownloadBean();
        if (cursor != null && cursor.moveToNext()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            //总需下载的字节数
            int totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            //已经下载的字节数
            int currentSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            //下载的文件到本地的目录
            String address = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            //下载文件的URL链接
            String url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
            // 当前进度
            int mProgress;
            if (totalSize != 0) {
                mProgress = (currentSize * 100) / totalSize;
            } else {
                mProgress = 0;
            }
            Log.d(TAG, String.valueOf(mProgress));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    // 下载暂停
                    handler.sendEmptyMessage(DownloadManager.STATUS_PAUSED);
                    Log.d(TAG, "STATUS_PAUSED");
                    break;
                case DownloadManager.STATUS_PENDING:
                    // 开始下载
                    handler.sendEmptyMessage(DownloadManager.STATUS_PENDING);
                    Log.d(TAG, "STATUS_PENDING");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    // 正在下载，不做任何事情
                    if (lastProgress == 0) {
                        lastProgress = currentSize;
                        lastTime = System.currentTimeMillis();
                    }
                    //下载速度
                    double speed = ((currentSize - lastProgress) * 1000 / (System.currentTimeMillis() - lastTime)) / 1024;
                    //计划完成时间
                    double planTime = (totalSize - currentSize) / (speed * 1024);
                    Message message = new Message();
                    message.what = DownloadManager.STATUS_RUNNING;
                    downloadBean.setStatus(DownloadManager.STATUS_RUNNING);
                    downloadBean.setProgress(mProgress);
                    downloadBean.setTotal(totalSize);
                    downloadBean.setPercent("-1");
//                    downloadBean.setId();
                    downloadBean.setPlanTime(planTime);
                    downloadBean.setSpeed(speed);
                    downloadBean.setAddress(address);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("DATA", downloadBean);
                    message.setData(bundle);
                    handler.sendMessage(message);
                    Log.d(TAG, "STATUS_RUNNING");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    if (!isEnd) {
                        // 完成
                        handler.sendEmptyMessage(DownloadManager.STATUS_SUCCESSFUL);
                        Log.d(TAG, "STATUS_SUCCESSFUL");
                    }
                    isEnd = true;
                    break;
                case DownloadManager.STATUS_FAILED:
                    if (!isEnd) {
                        handler.sendEmptyMessage(DownloadManager.STATUS_FAILED);
                        Log.d(TAG, "STATUS_FAILED");
                    }
                    isEnd = true;
                    break;
                default:
                    Log.d(TAG, "default");
                    break;
            }
            cursor.close();
        } else {
            Log.d(TAG, "cursor======null");
        }
    }

}
