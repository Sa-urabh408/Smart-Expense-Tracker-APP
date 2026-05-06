<h1 align="center">💰 Smart Expense Tracker</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Database-Firebase%20%7C%20Room-FFCA28?logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-blue.svg" />
</p>

> **Tired of doing complex math just to split a dinner bill?** Meet **Smart Expense Tracker**—the ultimate financial companion for friends, roommates, and travelers! From snapping receipt photos on the go to magically calculating exactly who owes whom, this app takes the stress out of shared costs. Dive into a seamless experience with smart debt settlements, personalized budgets, and premium analytics.

---

## 📥 Download App
You can directly download and install the latest APK on your Android device using the button below:

<a href="https://raw.githubusercontent.com/Sa-urabh408/Smart-Expense-Tracker-APP/main/SmartExpenseTracker.apk">
  <img src="https://img.shields.io/badge/Download-APK-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
</a>

*(Make sure to allow "Install from Unknown Sources" on your device if prompted).*

---

## 📸 Screenshots
*(Coming Soon)*
| Dashboard | Add Expense | Group Settlements | Reports |
|:---:|:---:|:---:|:---:|
| `<Image placeholder>` | `<Image placeholder>` | `<Image placeholder>` | `<Image placeholder>` |

---

## 🚀 Key Features

*   👥 **Group Expense Management:** Create custom groups for trips, apartments, or events. Add members easily and start logging shared expenses instantly.
*   🧾 **Smart Splitting:** Split bills equally, by exact amounts, or by percentages. The app handles all the complex math in the background.
*   📸 **Photo & Receipt Attachments:** Never lose track of a bill. Snap and attach photos of your receipts directly to the expense.
*   💸 **Simplified Settlements:** Our intelligent algorithm calculates the most efficient way to settle up, minimizing the number of transactions required to clear group debts ("who owes whom").
*   📊 **Detailed Analytics & Reports:** Weekly reports via Background Workers and a clear history of all your transactions.
*   ✨ **Premium Subscription:** Unlock advanced tracking, dark mode aesthetics, and exclusive perks through our in-app premium subscription.
*   🔐 **Secure Firebase Auth:** Your financial data is securely synchronized and authenticated via Firebase.

---

## 🛠️ Tech Stack & Architecture

This application strictly follows Modern Android Development practices:
- **Language**: Java
- **Architecture**: MVVM (Model-View-ViewModel) for robust, testable code.
- **Local Database**: Room Database for ultra-fast offline caching.
- **Remote Database**: Firebase Firestore for real-time cloud data synchronization.
- **Background Tasks**: WorkManager (Weekly reports and subscription verifications).
- **UI Components**: Material Design, Navigation Component, Bottom Sheets, and custom Chips.

---

## 💻 Installation & Setup (For Developers)

1. **Clone the repository**
   ```bash
   git clone https://github.com/Sa-urabh408/Smart-Expense-Tracker-APP.git
   ```
2. **Open in Android Studio**
   - Wait for Gradle to finish syncing the dependencies.
3. **Connect Firebase**
   - Add your own `google-services.json` file inside the `app/` directory. *(Note: The repository does not include this for security reasons).*
4. **Run the App**
   - Click the green "Run" button in Android Studio or run `./gradlew installDebug` in the terminal.

---

## 🤝 Contributing

Contributions are always welcome! Feel free to open a PR or submit an issue if you find a bug.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
<p align="center">Made with ❤️ by Saurabh</p>
