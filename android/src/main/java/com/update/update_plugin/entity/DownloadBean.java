package com.update.update_plugin.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class DownloadBean implements Parcelable {
    //当前进度
    private int progress;
    private long id;
    private String percent;
    private double planTime;
    private int status;
    private double speed;
    private int total;
    private String address;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    public double getPlanTime() {
        return planTime;
    }

    public void setPlanTime(double planTime) {
        this.planTime = planTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.progress);
        dest.writeLong(this.id);
        dest.writeString(this.percent);
        dest.writeDouble(this.planTime);
        dest.writeInt(this.status);
        dest.writeDouble(this.speed);
        dest.writeInt(this.total);
        dest.writeString(this.address);
    }

    public DownloadBean() {
    }

    protected DownloadBean(Parcel in) {
        this.progress = in.readInt();
        this.id = in.readLong();
        this.percent = in.readString();
        this.planTime = in.readDouble();
        this.status = in.readInt();
        this.speed = in.readDouble();
        this.total = in.readInt();
        this.address = in.readString();
    }

    public static final Parcelable.Creator<DownloadBean> CREATOR = new Parcelable.Creator<DownloadBean>() {
        @Override
        public DownloadBean createFromParcel(Parcel source) {
            return new DownloadBean(source);
        }

        @Override
        public DownloadBean[] newArray(int size) {
            return new DownloadBean[size];
        }
    };
}
