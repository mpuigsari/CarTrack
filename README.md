# 🚗 CarTrack - IoT-Based Vehicle Tracking App

CarTrack is a mobile application developed in the context of the course **IR2157 - Mobile Networks and Devices** from the **Bachelor’s Degree in Robotic Intelligence** at Universitat Jaume I. This project demonstrates the integration of mobile networks, IoT devices, and Android app development to enable real-time vehicle tracking.

> Developed as part of a team project, CarTrack uses Firebase and GPS/GSM-enabled IoT hardware (ESP32 TTGO T-Call) to track and manage vehicles via a real-time Android interface.

---

## 📱 App Features

- 🔐 User authentication via Firebase
- 📍 Real-time GPS tracking on an interactive Google Map
- 🚙 Management of multiple vehicles and associated IoT devices
- 📊 Location history visualization
- 🛠️ CRUD operations for vehicles/devices
- 🔄 Real-time synchronization using Firebase Realtime Database and Firestore

---

## 🌐 System Architecture

CarTrack follows a **modular IoT architecture**:

1. **IoT Device**: ESP32 TTGO T-Call with GPS/GSM sends location data to Firebase.
2. **Firebase Backend**:
   - **Realtime Database** for live updates
   - **Firestore** for persistent storage
   - **Storage** for vehicle images
3. **Android App**: Visualizes locations, manages devices, syncs with backend.

---

## 🧠 Learning Outcomes

- Developed a full-stack IoT system integrated with Android and Firebase.
- Worked with GPS and GSM hardware in real-world mobile contexts.
- Applied knowledge of mobile network protocols and cloud-based data management.
- Implemented real-time location tracking, map rendering, and user authentication.

---

## 🛠️ Technologies Used

- **Android Studio**
- **Firebase (Auth, Realtime DB, Firestore, Storage)**
- **Google Maps API**
- **ESP32 TTGO T-Call** (GPS + GSM)
- **Arduino IDE** (for device programming)
- **Java** (main app language)

---

## 🧪 Development Highlights

- Focused on modularity: Android app developed separately from IoT firmware.
- Implemented sync logic between Firebase Realtime Database and Firestore.
- Achieved location latency of ~3 seconds and accuracy within ~5 meters.
- Designed for scalability and usability across individual users and small fleets.

---

## 📘 Course Context

**Subject**: IR2157 - Mobile Networks and Devices  
**Instructor**: Raúl Marín Prades  
**Degree**: Bachelor's in Robotic Intelligence  
**Semester**: 4th year, 1st semester  
**Project Type**: Team final project (App code only)

---

## 👥 Authors

- Max Puig Sariñena  
- Albert Gabriel Matei  
- Lucas Gabaldón Selvi  

---

CarTrack shows how mobile networks and IoT devices can be combined into a user-friendly and scalable vehicle monitoring system. The Android app in this repository represents the front-end of a complete, cloud-connected tracking solution.

> 📦 *Note: This repository includes only the Android application code. IoT firmware and backend setup are documented in the project report.*
📄 [Download the Project Report from Releases](https://github.com/mpuigsari/CarTrack/releases)

