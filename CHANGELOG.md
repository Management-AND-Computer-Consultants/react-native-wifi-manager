# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-XX

### Added
- Initial release of React Native WiFi Manager
- WiFi network scanning functionality (Android)
- WiFi connection management with password authentication
- Beautiful popup interface for network selection
- Cross-platform support (Android & iOS)
- TypeScript definitions
- Custom React Hook (`useWifiManager`)
- React Component (`WifiManagerComponent`)
- Permission handling for location access
- Real-time signal strength monitoring
- Network capabilities detection
- Current WiFi information retrieval
- WiFi enable/disable functionality (Android only)

### Features
- **Android Support**: Full WiFi scanning, connection, and management
- **iOS Support**: WiFi connection and basic management (scanning not available due to iOS limitations)
- **TypeScript**: Complete type definitions for all APIs
- **Modern UI**: Native-looking popup interface
- **Permission Management**: Automatic handling of required permissions
- **Error Handling**: Comprehensive error handling and user feedback

### Technical Details
- React Native 0.60+ auto-linking support
- Peer dependencies for React and React Native
- Proper npm package structure with exports
- Installation script for easy setup
- Comprehensive documentation and examples

### Platform Limitations
- **iOS**: WiFi scanning is not available due to iOS security restrictions
- **iOS**: WiFi enable/disable functionality is not available
- **Android**: Requires location permissions for WiFi scanning 