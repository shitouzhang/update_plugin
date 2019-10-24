import 'dart:developer';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:update_plugin/update_plugin.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _downProgress = 'Unknown';

  @override
  void initState() {
    super.initState();
  }

  Future<void> initPlatformState() async {
    String downProgress;
    try {
//      platformVersion = await UpdatePlugin.platformVersion;
      UpdatePlugin.downloadApk(
          "http://appdl.hicloud.com/dl/appdl/application/apk/66/66b10ac29c9549cb892a5c430b1c090e/com.sqparking.park.1901261600.apk?mkey=5c516e787ae099f7&f=9e4a&sign=portal@portal1548830859005&source=portalsite");
      UpdatePlugin.stream.listen((info) {
        log('info----------------->$info');
        setState(() {
          _downProgress = info.progress.toString();
        });
      });
    } on PlatformException {
      downProgress = 'Failed to get platform version.';
    }

    if (!mounted) return;
    
//    {total: 14715618, progress: 89, percent: -1, id: 274, status: DownloadStatus(2), planTime: 0.4983142712080629, speed:3163.5329045027215, address:file:///storage/emulated/0/Android/data/com.update.update_plugin_example/files/download/com.update.update_plugin_example.apk}
//    {total: 0, progress: 100, percent: null, id: 274, status: DownloadStatus(3), planTime: 0.0, speed:0.0, address:null}
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              SizedBox(
                height: 100,
              ),
              Text('Running *****on: $_downProgress\n'),
              RaisedButton(onPressed: () {
                initPlatformState();
              }),
            ],
          ),
        ),
      ),
    );
  }
}
