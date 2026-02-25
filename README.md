# SafeSphere

SafeSphere is an Android-based real-time group location tracking and safety application designed to help families and trusted groups stay connected. It enables secure live location sharing, emergency alerts, group communication, and battery monitoring within private groups.

This project was developed as a Bachelor of Science (Software Engineering) Final Year Project at the University of Sindh (Session 2026).

---

## Project Overview

In modern society, families and friends often travel separately, leading to communication gaps and safety concerns. Existing applications like Google Maps Location Sharing, Life360, and Find My Friends provide partial solutions but lack structured group management, simplified privacy control, and academic-focused system design.

SafeSphere addresses these challenges by providing:

- Secure group-based real-time tracking
- Controlled access to location data
- Emergency alert system
- Real-time chat functionality
- Battery level monitoring
- Continuous background tracking

## Key Features

### Group Location Sharing
- Real-time GPS-based location updates
- Interactive map visualization
- Private group-only access

### Group Management
- Create and manage family/friend groups
- Add members via registered email
- Define family relationships

### Real-Time Group Chat
- Individual (1-to-1) chat
- Group chat
- Read receipts (similar to WhatsApp)

### Battery Percentage Monitoring
- Displays battery percentage of group members
- Low battery indicator (color change alert)

### Emergency Alert System
- Instant emergency notification
- Live location sharing during alert
- Notification delivery to all group members

### Persistent Background Tracking
- Location sharing continues even if app is minimized or closed

## Technologies Used

### Android Development
- Java
- XML (UI Layout Design)
- Android SDK
- Background Services

### Location & Maps
- Google Maps API
- GPS Services

### Backend & Cloud Services
- Firebase Authentication
- Firebase Firestore
- Firebase Realtime Database
- Firebase Cloud Messaging (Notifications)

## System Architecture

SafeSphere follows a modular architecture:

- **User Module** – Authentication, profile management
- **Group Module** – Group creation & member management
- **Location Module** – GPS tracking & real-time updates
- **Chat Module** – Individual and group messaging
- **Emergency Module** – Alert system & notifications
- **Cloud Backend** – Firebase-based real-time synchronization

---

## Testing & Validation

The system was tested using:

- Functional Testing
- Non-Functional Testing
- Real-Time Tracking Testing
- Emergency Alert Validation
- Battery Consumption Monitoring
- Notification Delivery Testing

All major test cases passed successfully, validating:

- Accurate real-time tracking
- Reliable emergency alerts
- Secure authentication
- Stable performance

## Functional Modules Tested

- User Registration & Login
- Group Member Addition
- Live Location Sharing
- Real-Time Map Updates
- Emergency Alert Trigger
- Push Notification Delivery
- Battery Status Updates
- Logout & Session Management

## Academic Context

This project was submitted as a Final Year Thesis for:

**Degree:** Bachelor of Science in Software Engineering  
**University:** University of Sindh  
**Session:** 2026  

## Limitations

- Requires active internet connection
- High battery consumption during continuous tracking
- Android platform only
- GPS accuracy may vary by device
- Emergency alert requires manual trigger

## Future Enhancements

- Geo-fencing support
- AI-based movement anomaly detection
- Automatic emergency SMS/call trigger
- Cross-platform support (iOS & Web)
- Enhanced privacy controls
