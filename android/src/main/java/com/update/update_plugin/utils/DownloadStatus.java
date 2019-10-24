package com.update.update_plugin.utils;

public enum DownloadStatus {
    STATUS_PAUSED(0),
    STATUS_PENDING(1),
    STATUS_RUNNING(2),
    STATUS_SUCCESSFUL(3),
    STATUS_FAILED(4);
    private int value;

    DownloadStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
