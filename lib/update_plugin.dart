import 'dart:async';
import 'dart:io';
import 'package:flutter/services.dart';

class UpdatePlugin {
  static const MethodChannel _channel = const MethodChannel('update_plugin');

  static const EventChannel _StreamChannel =
      const EventChannel('update_plugin/s');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  //startDownload
  static Future<void> downloadApk(String url) async {
    _channel.invokeMethod("downloadApk", url);
  }

  //cancelUpdate
  static Future<bool> cancelUpdateApk() async {
    _channel.invokeMethod("cancelUpdate");
    return true;
  }

  static Stream<DownloadInfo> get stream {
    assert(Platform.isAndroid, 'This method only support android application');
    return _StreamChannel.receiveBroadcastStream()
        .map((map) => DownloadInfo.formMap(map));
  }
}

class DownloadInfo {
  final int progress;
  final int id;
  final String percent;
  final int total;
  final double planTime;
  final double speed;
  final String address;
  final DownloadStatus status;

  DownloadInfo(
      {this.progress,
      this.id,
      this.percent,
      this.total,
      this.planTime,
      this.speed,
      this.address,
      this.status});

  factory DownloadInfo.formMap(dynamic map) => DownloadInfo(
        progress: map['progress'],
        id: map['id'],
        percent: map['percent'],
        total: map['total'],
        planTime: map['planTime'],
        speed: map['speed'],
        address: map['address'],
        status: DownloadStatus._internal(map['status']),
      );

  @override
  String toString() {
    return 'DownloadInfo{total: $total, progress: $progress, percent: $percent, id: $id, status: $status, '
        'planTime: $planTime, speed:$speed, address:$address}';
  }
}

class DownloadStatus {
  final int _value;

  int get value => _value;

  const DownloadStatus._internal(this._value);

  static DownloadStatus from(int value) => DownloadStatus._internal(value);

  static const STATUS_PAUSED = const DownloadStatus._internal(0);
  static const STATUS_PENDING = const DownloadStatus._internal(1);
  static const STATUS_RUNNING = const DownloadStatus._internal(2);
  static const STATUS_SUCCESSFUL = const DownloadStatus._internal(3);
  static const STATUS_FAILED = const DownloadStatus._internal(4);

  get hashCode => _value;

  operator ==(status) => status._value == this._value;

  toString() => 'DownloadStatus($_value)';
}
