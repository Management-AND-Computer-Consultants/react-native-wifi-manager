# WiFi Manager Troubleshooting Guide

## "Failed to Scan Error" - Common Solutions

### 1. **Check WiFi is Enabled**
Make sure WiFi is turned on in your device settings before scanning.

### 2. **Permission Issues**
The most common cause of "Failed to Scan" errors is missing permissions.

#### Required Permissions:
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE` 
- `ACCESS_FINE_LOCATION` (Required for WiFi scanning on Android)
- `ACCESS_COARSE_LOCATION`
- `ACCESS_NETWORK_STATE`
- `CHANGE_NETWORK_STATE`

#### Android 13+ (API 33+) Additional Permission:
- `NEARBY_WIFI_DEVICES` (Alternative to location permission)

### 3. **Check Permissions Programmatically**
Use the new `checkPermissions()` method to diagnose permission issues:

```javascript
import WifiManager from '@management-and-computer-consultants/react-native-wifi-manager';

// Check all permissions
const checkPermissions = async () => {
  try {
    const permissions = await WifiManager.checkPermissions();
    console.log('Permission Status:', permissions);
    
    if (!permissions.canScan) {
      console.log('Cannot scan - missing permissions or WiFi disabled');
      console.log('WiFi Enabled:', permissions.isWifiEnabled);
      console.log('Fine Location:', permissions.hasFineLocation);
      console.log('Nearby WiFi Devices:', permissions.hasNearbyWifiDevices);
    }
  } catch (error) {
    console.error('Permission check failed:', error);
  }
};
```

### 4. **Request Permissions**
Make sure to request permissions before scanning:

```javascript
import { PermissionsAndroid, Platform } from 'react-native';

const requestPermissions = async () => {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
      ]);
      
      if (granted['android.permission.ACCESS_FINE_LOCATION'] === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('Location permissions granted');
      } else {
        console.log('Location permissions denied');
      }
    } catch (err) {
      console.warn(err);
    }
  }
};
```

### 5. **AndroidManifest.xml Permissions**
Ensure these permissions are in your app's `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

<!-- For Android 13+ -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" 
                 android:maxSdkVersion="32" />
```

### 6. **Device-Specific Issues**

#### Samsung Devices:
- Some Samsung devices require additional permissions
- Check if "Location" is enabled in device settings
- Ensure "WiFi scanning" is enabled in Location settings

#### Huawei/Xiaomi Devices:
- Check battery optimization settings
- Disable battery optimization for your app
- Check if "Auto-start" is enabled for your app

### 7. **Debug Logging**
Enable debug logging to see what's happening:

```javascript
// Check the console/logs for detailed error messages
// Look for logs starting with "WifiManagerModule"
```

### 8. **Common Error Messages and Solutions**

| Error Message | Solution |
|---------------|----------|
| `WIFI_DISABLED` | Enable WiFi in device settings |
| `PERMISSION_DENIED` | Grant location permissions |
| `SCAN_FAILED` | Check WiFi state and permissions |
| `SCAN_TIMEOUT` | Try again, may be temporary |
| `NO_NETWORKS_FOUND` | Check if WiFi networks are available |

### 9. **Testing Steps**

1. **Enable WiFi** in device settings
2. **Grant location permissions** when prompted
3. **Check permissions** using `checkPermissions()` method
4. **Try scanning** with error handling:

```javascript
const scanNetworks = async () => {
  try {
    const networks = await WifiManager.scanWifiNetworks();
    console.log('Found networks:', networks);
  } catch (error) {
    console.error('Scan failed:', error.message);
    
    // Check permissions if scan fails
    if (error.message.includes('PERMISSION_DENIED')) {
      await checkPermissions();
    }
  }
};
```

### 10. **Still Having Issues?**

If you're still experiencing problems:

1. **Check device logs** for detailed error messages
2. **Verify Android version** compatibility
3. **Test on different devices** to isolate device-specific issues
4. **Check if other WiFi apps work** on the same device
5. **Restart the device** and try again

### 11. **Version Compatibility**

- **minSdkVersion**: 21 (Android 5.0+)
- **targetSdkVersion**: 35 (Android 15)
- **React Native**: 0.60.0+

For older Android versions, some features may be limited due to platform restrictions. 