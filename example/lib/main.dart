import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'package:flutter_zebra_sdk/flutter_zebra_sdk.dart';

void main() {
  runApp(MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    initial();
  }

  void initial() async {
    // await Permission.
  }

  Future _ackAlert(BuildContext context, String title) async {
    return showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          // content: const Text('This item is no longer available'),
          actions: [
            TextButton(
              child: Text('Ok'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> onDiscovery() async {
    var a = await ZebraSdk.onDiscovery();
    print(a);
    var b = json.decode(a);

    var printers = b['content'];
    if (printers != null) {
      var printObj = json.decode(printers);
      print(printObj);
    }

    print(b);
  }

  Future<void> onDiscoveryUSB(dynamic context) async {
    var a = await ZebraSdk.onDiscoveryUSB();
    _ackAlert(context, 'USB $a');
    print(a);
    var b = json.decode(a);

    var printers = b['content'];
    if (printers != null) {
      var printObj = json.decode(printers);
      print(printObj);
    }
    print(b);
  }

  Future<void> onGetIPInfo() async {
    try {
      // Optionally, show a loading indicator here

      // Validate the IP address format (basic validation)
      String ipAddress = '192.168.0.101';
      int port = 9100;
      if (!isValidIPAddress(ipAddress)) {
        print("Invalid IP address format");
        return;
      }

      var printerInfo = await ZebraSdk.onGetPrinterInfo(ipAddress, port:port);

      if (printerInfo != null) {
        print("Printer Info: $printerInfo");
      } else {
        print("No information returned for the printer.");
      }
    } catch (e) {
      if (e is PlatformException && e.code == "DISCOVERY_ERROR") {
        print("Discovery error: ${e.message}");
      } else {
        print("An unexpected error occurred: $e");
      }
    } finally {
      // Hide the loading indicator here
    }
  }

// Basic IP address validation function
  bool isValidIPAddress(String ipAddress) {
    final RegExp ipRegExp = RegExp(
      r'^(?!0)(?!.*\.$)(?!.*\.\.)([0-9]{1,3}\.){3}[0-9]{1,3}$',
    );
    return ipRegExp.hasMatch(ipAddress);
  }


  Future<void> onTestConnect() async {
    try {
      var a = await ZebraSdk.isPrinterConnected('192.168.0.101', port:9100);
      if (a != null) {
        print("Connection result: $a"); // This is already a Map
        // You can access the data directly without decoding
        if (a['success'] == true) {
          print("Printer is connected: ${a['connected']}");
          print("Success ${a['message']}");
        }
        else {
          print("Failed ${a['message']}");
        }
      } else {
        print("No response from printer connection check.");
      }
    } catch (e) {
      print("Error during connection check: $e");
    }
  }

  Future<void> onTestTCP() async {
    String data;
    data = '''
    ''
    ^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR6,6~SD15^JUS^LRN^CI0^XZ
    ^XA
    ^MMT
    ^PW500
    ^LL0240
    ^LS0
    ^FT144,33^A0N,25,24^FB111,1,0,C^FH\^FDITEM TITLE^FS
    ^FT3,61^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDOption 1, Option 2, Option 3, Option 4, Opt^FS^CI0
    ^FT3,84^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDion 5, Option 6 ^FS^CI0
    ^FT34,138^A@N,25,24,TT0003M_^FB331,1,0,C^FH\^CI17^F8^FDOrder: https://eat.chat/phobac^FS^CI0
    ^FT29,173^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FDPromotional Promotional Promotional^FS^CI0
    ^FT29,193^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FD Promotional Promotional ^FS^CI0
    ^FT106,233^A0N,25,24^FB188,1,0,C^FH\^FDPHO BAC HOA VIET^FS
    ^PQ1,0,1,Y^XZ
        ''';

    final rep = ZebraSdk.printZPLOverTCPIP('192.168.0.101', data: data);
    print(rep);
  }

  Future<void> onTestBluetooth() async {
    String data;
    data = '''
    ''
    ^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR6,6~SD15^JUS^LRN^CI0^XZ
    ^XA
    ^MMC
    ^PW500
    ^LL0240
    ^LS0
    ^FT144,33^A0N,25,24^FB111,1,0,C^FH\^FDITEM TITLE^FS
    ^FT3,61^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDOption 1, Option 2, Option 3, Option 4, Opt^FS^CI0
    ^FT3,84^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDion 5, Option 6 ^FS^CI0
    ^FT34,138^A@N,25,24,TT0003M_^FB331,1,0,C^FH\^CI17^F8^FDOrder: https://eat.chat/phobac^FS^CI0
    ^FT29,173^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FDPromotional Promotional Promotional^FS^CI0
    ^FT29,193^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FD Promotional Promotional ^FS^CI0
    ^FT106,233^A0N,25,24^FB188,1,0,C^FH\^FDPHO BAC HOA VIET^FS
    ^PQ1,0,1,Y^XZ
        ''';

    String arr = '50:8C:B1:8D:10:C7';
    if (Platform.isIOS) {
      arr = '50J171201608';
    }
    final rep = ZebraSdk.printZPLOverBluetooth(arr, data: data);
    print(rep);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Center(
            child: Column(
              children: [
                TextButton(
                    onPressed: onGetIPInfo, child: Text('onGetPrinterInfo')),
                TextButton(
                    onPressed: onTestConnect, child: Text('onTestConnect')),
                TextButton(onPressed: onDiscovery, child: Text('Discovery')),
                TextButton(
                    onPressed: () => onDiscoveryUSB(context),
                    child: Text('Discovery USB')),
                TextButton(onPressed: onTestTCP, child: Text('Print TCP')),
                TextButton(
                    onPressed: onTestBluetooth, child: Text('Print Bluetooth')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
