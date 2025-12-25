---

# 📍 M-APP-GPS — Android GPS Tracking & Trip Management App

M-APP-GPS is a **feature-rich Android application** built using **Kotlin** that provides real-time GPS tracking, trip management, user authentication, and payment handling.
The app is designed with a **service-based architecture**, integrates **Firebase**, and follows clean Android development practices.

This project demonstrates practical implementation of **location services, background tracking, REST APIs, Firebase messaging, and modern UI design**.

---

## 🚀 Key Features

### 🔐 User Authentication

* User **Login & Registration**
* Secure activity flow control
* Separate screens for login and registration

### 🛰️ Real-Time GPS Tracking

* Background **LocationService**
* Continuous GPS tracking
* Tracker controller to manage tracking lifecycle
* Optimized for accuracy and battery efficiency

### 🧭 Trip Management

* Automatic trip creation
* Trip history screen
* RecyclerView adapter for trip listing
* API-based trip retrieval

### 💳 Payment Integration

* Dedicated payment activity
* Payment model & service layer
* Backend communication using Retrofit
* Paid badge UI indicators

### 🔔 Firebase Integration

* Firebase Cloud Messaging
* Background message handling
* Command listener for real-time updates

### 🎨 Modern UI & UX

* Custom XML drawables
* Reusable UI components
* Clean and consistent theming
* Card-based layouts for trips and payments

---

## 🧠 Tech Stack

| Layer                 | Technology                     |
| --------------------- | ------------------------------ |
| Language              | **Kotlin**                     |
| Architecture          | Activities + Services          |
| Networking            | **Retrofit**                   |
| Background Tasks      | Android Services               |
| GPS                   | Android Location APIs          |
| Backend Communication | REST APIs                      |
| Cloud                 | **Firebase**                   |
| UI                    | XML Layouts & Custom Drawables |
| Build System          | Gradle (Kotlin DSL)            |

---

## 📁 Project Structure

```
app/
├── manifests/
│   └── AndroidManifest.xml
│
├── kotlin+java/com.example.gpsapp/
│   ├── LoginActivity.kt
│   ├── RegisterActivity.kt
│   ├── MainActivity.kt
│   ├── TripHistoryActivity.kt
│   ├── PaymentActivity.kt
│   ├── LocationService.kt
│   ├── TrackerController.kt
│   ├── TripAdapter.kt
│   ├── Trip.kt
│   ├── TripService.kt
│   ├── TripHistoryResponse.kt
│   ├── PaymentModel.kt
│   ├── PaymentService.kt
│   ├── RetroFitClient.kt
│   ├── FirebaseCommandListener.kt
│   ├── MyFirebaseService.kt
│   └── VehicleRegistrationHelper.kt
│
├── res/
│   ├── layout/
│   │   ├── activity_login.xml
│   │   ├── activity_register.xml
│   │   ├── activity_main.xml
│   │   ├── activity_payment.xml
│   │   ├── activity_trip_history.xml
│   │   └── trip_item.xml
│   │
│   ├── drawable/
│   │   ├── btn_primary.xml
│   │   ├── btn_secondary.xml
│   │   ├── card_beauty.xml
│   │   ├── search_bg.xml
│   │   └── paid_badge_bg.xml
│   │
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   └── themes.xml
│
└── Gradle Scripts/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── proguard-rules.pro
```

---

## 🛠️ Setup & Installation

### 📥 Clone the Repository

```bash
git clone https://github.com/shritej1808/M-APP-GPS.git
```

### 🧪 Open in Android Studio

1. Open **Android Studio**
2. Click **Open Project**
3. Select the cloned folder
4. Let Gradle sync complete

### 📱 Run the App

* Connect a physical device or emulator
* Enable **Location Services**
* Grant **runtime permissions**
* Click **Run ▶**

---

## ⚠️ Required Permissions

The app uses:

* 📍 **Fine Location**
* 📍 **Background Location**
* 🌐 **Internet Access**
* 🔔 **Firebase Messaging**

Ensure all permissions are granted for proper functionality.

---

## 📸 Screenshots

> 📷 Add screenshots here (Login, Tracking, Trip History, Payment)

---

## 🔮 Future Enhancements

* Google Maps / OpenStreetMap integration
* Live trip sharing
* Route visualization
* Admin dashboard
* Analytics for trip distance & cost
* Google Pay / UPI integration

---

## 🎯 Why This Project Matters

This project demonstrates **real-world Android development skills**, including:

* Background services
* GPS tracking
* API integration
* Firebase usage
* Clean UI design
* Scalable app structure

Perfect as a **portfolio project** for Android / Full-Stack / Mobile Developer roles.

---
Just say the word 😉
