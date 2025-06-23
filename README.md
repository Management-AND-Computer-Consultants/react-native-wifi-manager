# Management and Computer Consultants React Native WiFi Manager

A comprehensive React Native plugin for WiFi network scanning and connection management. This plugin provides a beautiful popup interface for users to scan available WiFi networks and connect to them with password authentication.

## Features

- 🔍 **WiFi Network Scanning**: Scan and display available WiFi networks
- 🔐 **Secure Connection**: Connect to WiFi networks with password authentication
- 📱 **Cross-Platform**: Works on both Android and iOS (with platform-specific limitations)
- 🎨 **Beautiful UI**: Modern, native-looking popup interface
- ⚡ **Real-time Updates**: Live signal strength and network information
- 🔒 **Permission Handling**: Automatic permission requests for location access
- 🎯 **TypeScript Support**: Full TypeScript definitions included

## Platform Support

| Feature | Android | iOS |
|---------|---------|-----|
| WiFi Scanning | ✅ Full Support | ❌ Not Available (iOS limitation) |
| WiFi Connection | ✅ Full Support | ✅ iOS 11.0+ |
| WiFi Disconnection | ✅ Full Support | ✅ iOS 11.0+ |
| Current WiFi Info | ✅ Full Support | ✅ Limited |
| WiFi Enable/Disable | ✅ Full Support | ❌ Not Available (iOS limitation) |

## Installation

### 1. Install the package

```bash
npm install management-and-computer-consultants-react-native-wifi-manager
# or
yarn add management-and-computer-consultants-react-native-wifi-manager
```

### 2. Link the native modules

#### For React Native 0.60+

The plugin should auto-link. If not, run:

```bash
npx react-native link management-and-computer-consultants-react-native-wifi-manager
```

#### Manual linking (if needed)

**Android:**

Add to `android/settings.gradle`:
```gradle
include ':management-and-computer-consultants-react-native-wifi-manager'
project(':management-and-computer-consultants-react-native-wifi-manager').projectDir = new File(rootProject.projectDir, '../node_modules/management-and-computer-consultants-react-native-wifi-manager/android')
```

Add to `android/app/build.gradle`:
```gradle
dependencies {
    implementation project(':management-and-computer-consultants-react-native-wifi-manager')
}
```

Add to `android/app/src/main/java/com/yourapp/MainApplication.java`:
```java
import com.wifimanager.WifiManagerPackage;

// In getPackages() method:
packages.add(new WifiManagerPackage());
```

**iOS:**

Add to `ios/Podfile`:
```ruby
pod 'management-and-computer-consultants-react-native-wifi-manager', :path => '../node_modules/management-and-computer-consultants-react-native-wifi-manager'
```

Then run:
```bash
cd ios && pod install
```

### 3. Configure permissions

**Android:**

Add these permissions to `android/app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

**iOS:**

Add these capabilities to your Xcode project:
- `Access WiFi Information` capability
- `Network Extensions` capability (for iOS 11+)

## Usage

### Basic Usage with Popup Component

```tsx
import React, { useState } from 'react';
import { View, Button, Alert } from 'react-native';
import WifiManagerComponent from 'management-and-computer-consultants-react-native-wifi-manager/src/WifiManager';

const App = () => {
  const [showWifiModal, setShowWifiModal] = useState(false);

  const handleWifiConnect = (ssid: string) => {
    Alert.alert('Connected!', `Successfully connected to ${ssid}`);
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Button 
        title="Open WiFi Manager" 
        onPress={() => setShowWifiModal(true)} 
      />
      
      <WifiManagerComponent
        visible={showWifiModal}
        onClose={() => setShowWifiModal(false)}
        onConnect={handleWifiConnect}
      />
    </View>
  );
};
```

### Advanced Usage with Custom Hook

```tsx
import React, { useState, useEffect } from 'react';
import { View, Text, Button, FlatList, Alert } from 'react-native';
import { useWifiManager } from 'management-and-computer-consultants-react-native-wifi-manager/src/useWifiManager';

const WifiScreen = () => {
  const {
    networks,
    currentWifi,
    loading,
    error,
    scanNetworks,
    connectToWifi,
    disconnectFromWifi,
    getCurrentWifiInfo,
  } = useWifiManager();

  const [selectedNetwork, setSelectedNetwork] = useState(null);
  const [password, setPassword] = useState('');

  useEffect(() => {
    // Get current WiFi info on component mount
    getCurrentWifiInfo();
  }, []);

  const handleConnect = async () => {
    if (!selectedNetwork || !password) {
      Alert.alert('Error', 'Please select a network and enter password');
      return;
    }

    try {
      await connectToWifi(selectedNetwork.ssid, password);
      Alert.alert('Success', 'Connected to WiFi!');
      setPassword('');
      setSelectedNetwork(null);
    } catch (err) {
      Alert.alert('Error', 'Failed to connect to WiFi');
    }
  };

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>
        WiFi Networks
      </Text>

      {currentWifi && (
        <View style={{ marginBottom: 20, padding: 10, backgroundColor: '#e8f5e8' }}>
          <Text>Connected to: {currentWifi.ssid}</Text>
          <Text>Signal: {currentWifi.signalStrength} dBm</Text>
        </View>
      )}

      <Button 
        title={loading ? "Scanning..." : "Scan Networks"} 
        onPress={scanNetworks}
        disabled={loading}
      />

      {error && (
        <Text style={{ color: 'red', marginTop: 10 }}>{error}</Text>
      )}

      <FlatList
        data={networks}
        keyExtractor={(item) => item.bssid}
        renderItem={({ item }) => (
          <View style={{ padding: 15, borderBottomWidth: 1, borderBottomColor: '#eee' }}>
            <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{item.ssid}</Text>
            <Text>Signal: {item.signalStrength} dBm</Text>
            <Text>Security: {item.capabilities}</Text>
            <Button 
              title="Connect" 
              onPress={() => setSelectedNetwork(item)}
            />
          </View>
        )}
      />
    </View>
  );
};
```

### Direct API Usage

```tsx
import WifiManager from 'management-and-computer-consultants-react-native-wifi-manager';

// Scan for WiFi networks
const scanNetworks = async () => {
  try {
    const networks = await WifiManager.scanWifiNetworks();
    console.log('Available networks:', networks);
  } catch (error) {
    console.error('Scan failed:', error);
  }
};

// Connect to a WiFi network
const connectToNetwork = async (ssid: string, password: string) => {
  try {
    await WifiManager.connectToWifi(ssid, password);
    console.log('Connected successfully!');
  } catch (error) {
    console.error('Connection failed:', error);
  }
};

// Get current WiFi information
const getCurrentWifi = async () => {
  try {
    const info = await WifiManager.getCurrentWifiInfo();
    console.log('Current WiFi:', info);
  } catch (error) {
    console.error('Failed to get WiFi info:', error);
  }
};
```

## API Reference

### WifiManager (Native Module)

#### Methods

- `scanWifiNetworks(): Promise<WifiNetwork[]>`
  - Scans for available WiFi networks
  - Returns array of network objects
  - Requires location permission on Android

- `connectToWifi(ssid: string, password: string): Promise<boolean>`
  - Connects to a WiFi network
  - Returns true on success

- `disconnectFromWifi(): Promise<boolean>`
  - Disconnects from current WiFi network
  - Returns true on success

- `getCurrentWifiInfo(): Promise<WifiInfo | null>`
  - Gets information about currently connected WiFi
  - Returns null if not connected

- `isWifiEnabled(): Promise<boolean>`
  - Checks if WiFi is enabled
  - Returns true/false

- `setWifiEnabled(enabled: boolean): Promise<boolean>`
  - Enables or disables WiFi
  - Android only (iOS doesn't support this)

### WifiManagerComponent

#### Props

- `visible: boolean` - Controls modal visibility
- `onClose: () => void` - Called when modal is closed
- `onConnect?: (ssid: string) => void` - Called when successfully connected

### useWifiManager Hook

#### Returns

- `networks: WifiNetwork[]` - Array of scanned networks
- `currentWifi: WifiInfo | null` - Current WiFi connection info
- `loading: boolean` - Loading state
- `error: string | null` - Error message
- `scanNetworks()` - Function to scan networks
- `connectToWifi(ssid, password)` - Function to connect
- `disconnectFromWifi()` - Function to disconnect
- `getCurrentWifiInfo()` - Function to get current info
- `isWifiEnabled()` - Function to check WiFi status
- `setWifiEnabled(enabled)` - Function to enable/disable WiFi
- `clearError()` - Function to clear error state

## Types

```typescript
interface WifiNetwork {
  ssid: string;
  bssid: string;
  signalStrength: number;
  capabilities: string;
  frequency: number;
  channel: number;
}

interface WifiInfo {
  ssid: string;
  signalStrength: number;
  networkId: number;
}
```

## Troubleshooting

### Common Issues

1. **Permission Denied on Android**
   - Ensure location permission is granted
   - Check that all required permissions are in AndroidManifest.xml

2. **WiFi Scanning Not Working on iOS**
   - This is a platform limitation - iOS doesn't allow WiFi scanning
   - Only WiFi connection is supported on iOS

3. **Build Errors**
   - Clean and rebuild your project
   - Ensure all native dependencies are properly linked

4. **Connection Fails**
   - Verify the password is correct
   - Check that the network is in range
   - Ensure WiFi is enabled

### Debug Mode

Enable debug logging by setting the following in your app:

```tsx
// Add this to see detailed logs
if (__DEV__) {
  console.log('WiFi Manager Debug Mode Enabled');
}
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

If you encounter any issues or have questions, please:

1. Check the [troubleshooting section](#troubleshooting)
2. Search existing [issues](https://github.com/yourusername/management-and-computer-consultants-react-native-wifi-manager/issues)
3. Create a new issue with detailed information about your problem

## Changelog

### 1.0.0
- Initial release
- WiFi scanning and connection support
- Beautiful popup interface
- Cross-platform compatibility
- TypeScript support 