import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  ScrollView,
} from 'react-native';
import WifiManagerComponent from '../src/WifiManager';
import { useWifiManager } from '../src/useWifiManager';

const App = () => {
  const [showWifiModal, setShowWifiModal] = useState(false);
  const {
    networks,
    currentWifi,
    loading,
    error,
    scanNetworks,
    connectToWifi,
    disconnectFromWifi,
    getCurrentWifiInfo,
    clearError,
  } = useWifiManager();

  const handleWifiConnect = (ssid: string) => {
    Alert.alert('Success!', `Successfully connected to ${ssid}`);
  };

  const handleManualConnect = async () => {
    // This is just a demo - in a real app you'd have a form
    Alert.prompt(
      'Connect to WiFi',
      'Enter network SSID:',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Connect',
          onPress: async (ssid) => {
            if (ssid) {
              Alert.prompt(
                'Password',
                `Enter password for ${ssid}:`,
                [
                  { text: 'Cancel', style: 'cancel' },
                  {
                    text: 'Connect',
                    onPress: async (password) => {
                      if (password) {
                        try {
                          await connectToWifi(ssid, password);
                          Alert.alert('Success', 'Connected to WiFi!');
                        } catch (err) {
                          Alert.alert('Error', 'Failed to connect to WiFi');
                        }
                      }
                    },
                  },
                ],
                'secure-text'
              );
            }
          },
        },
      ],
      'plain-text'
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.title}>WiFi Manager Demo</Text>
          <Text style={styles.subtitle}>
            Management and Computer Consultants React Native WiFi Manager Plugin
          </Text>
        </View>

        {/* Current WiFi Status */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Current Connection</Text>
          {currentWifi ? (
            <View style={styles.currentWifi}>
              <Text style={styles.wifiName}>📶 {currentWifi.ssid}</Text>
              <Text style={styles.wifiDetails}>
                Signal: {currentWifi.signalStrength} dBm
              </Text>
              <TouchableOpacity
                style={styles.disconnectButton}
                onPress={disconnectFromWifi}
              >
                <Text style={styles.disconnectButtonText}>Disconnect</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <Text style={styles.noConnection}>Not connected to WiFi</Text>
          )}
        </View>

        {/* Quick Actions */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Quick Actions</Text>
          
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => setShowWifiModal(true)}
          >
            <Text style={styles.actionButtonText}>🔍 Open WiFi Manager</Text>
            <Text style={styles.actionButtonSubtext}>
              Beautiful popup interface
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionButton}
            onPress={scanNetworks}
            disabled={loading}
          >
            <Text style={styles.actionButtonText}>
              {loading ? '⏳ Scanning...' : '📡 Scan Networks'}
            </Text>
            <Text style={styles.actionButtonSubtext}>
              Programmatic scanning
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionButton}
            onPress={handleManualConnect}
          >
            <Text style={styles.actionButtonText}>🔗 Manual Connect</Text>
            <Text style={styles.actionButtonSubtext}>
              Connect with prompts
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionButton}
            onPress={getCurrentWifiInfo}
          >
            <Text style={styles.actionButtonText}>ℹ️ Get WiFi Info</Text>
            <Text style={styles.actionButtonSubtext}>
              Refresh connection info
            </Text>
          </TouchableOpacity>
        </View>

        {/* Scanned Networks */}
        {networks.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>
              Available Networks ({networks.length})
            </Text>
            <View style={styles.networksList}>
              {networks.slice(0, 5).map((network, index) => (
                <View key={network.bssid} style={styles.networkItem}>
                  <View style={styles.networkInfo}>
                    <Text style={styles.networkName}>{network.ssid}</Text>
                    <Text style={styles.networkDetails}>
                      Signal: {network.signalStrength} dBm • 
                      Channel: {network.channel}
                    </Text>
                  </View>
                  <View style={styles.networkSignal}>
                    <Text style={styles.signalIcon}>
                      {network.signalStrength >= -50 ? '📶' : 
                       network.signalStrength >= -60 ? '📶' : 
                       network.signalStrength >= -70 ? '📶' : '📶'}
                    </Text>
                  </View>
                </View>
              ))}
              {networks.length > 5 && (
                <Text style={styles.moreNetworks}>
                  ... and {networks.length - 5} more networks
                </Text>
              )}
            </View>
          </View>
        )}

        {/* Error Display */}
        {error && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Error</Text>
            <View style={styles.errorContainer}>
              <Text style={styles.errorText}>{error}</Text>
              <TouchableOpacity
                style={styles.clearErrorButton}
                onPress={clearError}
              >
                <Text style={styles.clearErrorButtonText}>Clear</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* Platform Info */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Platform Support</Text>
          <View style={styles.platformInfo}>
            <Text style={styles.platformText}>
              📱 Android: Full WiFi scanning and connection support
            </Text>
            <Text style={styles.platformText}>
              🍎 iOS: WiFi connection only (scanning not available)
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* WiFi Manager Modal */}
      <WifiManagerComponent
        visible={showWifiModal}
        onClose={() => setShowWifiModal(false)}
        onConnect={handleWifiConnect}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  scrollContent: {
    padding: 20,
  },
  header: {
    alignItems: 'center',
    marginBottom: 30,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#000',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#8E8E93',
    textAlign: 'center',
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
    marginBottom: 12,
  },
  currentWifi: {
    backgroundColor: '#E8F5E8',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#34C759',
  },
  wifiName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
    marginBottom: 4,
  },
  wifiDetails: {
    fontSize: 14,
    color: '#666',
    marginBottom: 12,
  },
  disconnectButton: {
    backgroundColor: '#FF3B30',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    alignSelf: 'flex-start',
  },
  disconnectButtonText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '500',
  },
  noConnection: {
    fontSize: 16,
    color: '#8E8E93',
    fontStyle: 'italic',
  },
  actionButton: {
    backgroundColor: 'white',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E5E5EA',
  },
  actionButtonText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#000',
    marginBottom: 4,
  },
  actionButtonSubtext: {
    fontSize: 14,
    color: '#8E8E93',
  },
  networksList: {
    backgroundColor: 'white',
    borderRadius: 12,
    overflow: 'hidden',
  },
  networkItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F2F2F7',
  },
  networkInfo: {
    flex: 1,
  },
  networkName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#000',
    marginBottom: 4,
  },
  networkDetails: {
    fontSize: 14,
    color: '#8E8E93',
  },
  networkSignal: {
    alignItems: 'center',
  },
  signalIcon: {
    fontSize: 20,
  },
  moreNetworks: {
    padding: 16,
    textAlign: 'center',
    color: '#8E8E93',
    fontSize: 14,
  },
  errorContainer: {
    backgroundColor: '#FFE5E5',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#FF3B30',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  errorText: {
    flex: 1,
    fontSize: 14,
    color: '#D70015',
  },
  clearErrorButton: {
    backgroundColor: '#FF3B30',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  clearErrorButtonText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '500',
  },
  platformInfo: {
    backgroundColor: 'white',
    padding: 16,
    borderRadius: 12,
  },
  platformText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
});

export default App; 