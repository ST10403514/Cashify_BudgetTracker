# Cashify
**Save Smart, Spend Wise**

![Cashify Logo](assets/Cashify_Logo.gif)

## GitHub Link
[https://github.com/ST10403514/Cashify_BudgetTracker](https://github.com/ST10403514/Cashify_BudgetTracker)

## Demonstration Video - Part 2
[https://youtu.be/cs7XJiEM3zw](https://youtu.be/cs7XJiEM3zw)

## Demonstration Video - Part 3
[https://youtu.be/F7GplhIbe3M](https://youtu.be/F7GplhIbe3M)

## Team Information
**Members**:
- Ibrahim Ghogawala (ST10286968)
- Matthew Mason (ST10403514)
- Tiffany Mather (ST10249863)

**Course**: BCAD Year 3  
**Module**: Programming 3C (PROG7313)  
**Assessment**: Portfolio of Evidence (POE) Part 2 and Part 3

## Overview
Cashify is an intuitive budget tracking app designed to empower users to manage their finances with clarity and ease. It promotes mindful spending by enabling users to track expenses, set category-specific goals, and analyze spending patterns. Key features include:

- **Simple UI**: Clean, user-friendly interface for seamless navigation.
- **Secure Authentication**: User credentials stored securely for signup and login.
- **Photo Support**: Attach and view photos (e.g., receipts) for expenses.
- **Goal Tracking**: Set monthly minimum and maximum spending goals per category.
- **Insightful Analytics**: Visualize spending and goal progress via charts.
- **Cloud Storage**: Sync data online with Firebase Firestore, with offline support.

Cashify is built for reliability, security, and responsiveness, making it ideal for daily financial management.

## Technologies Used
- **Firebase Authentication**: Stores user credentials for secure signup and login.
- **Firebase Firestore**: Cloud database for online storage of expenses, goals, and categories.
- **RoomDB**: Local database for offline expense, goal, and category storage (Part 2).
- **MPAndroidChart**: Library for rendering spending and goal charts.
- **Android Studio**: Development environment (Kotlin, XML).
- **Kotlin**: Primary programming language for logic and UI, with Coroutines for asynchronous tasks.
- **XML**: Layout design for activities and fragments.
- **Glide**: Efficient image loading for expense and goal photos.
- **Material Design**: UI components for a modern look and feel.

## Setup Instructions

### Prerequisites
- **Android Studio**: Latest version (e.g., Koala | 2024.1.1 or later).
- **Emulator**: Pixel 8 API 35 or Medium Phone API 33, or a physical Android device (API 24+).
- **Git**: For cloning the repository.
- **Disk Space**: At least 5GB free.
- **Internet**: For Gradle sync and Firebase connectivity.

### Installation

#### Option 1: Clone the Repository
1. Navigate to the GitHub repository: [https://github.com/ST10403514/Cashify_BudgetTracker](https://github.com/ST10403514/Cashify_BudgetTracker).
2. Click the green **Code** button and copy the HTTPS URL.
3. Open **Android Studio**.
4. Select **File > New > Project from Version Control**.
5. Paste the URL and click **Clone**.
6. Wait for Gradle to sync (click **Sync Project with Gradle Files** if needed).
7. Configure Firebase:
   - Download `google-services.json` from your Firebase project.
   - Place it in the `app/` directory.
8. Run the app:
   - Select an emulator (Pixel 8 API 35 or Medium Phone API 33) or connect a physical device.
   - Click **Run > Run 'app'**.

#### Option 2: Install via APK
1. Clone the repository (as above) or download the source ZIP.
2. Open the project in **Android Studio**.
3. Build the APK:
   - Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Wait for the build to complete (check **Build Output**).
   - Locate the APK at `app/build/outputs/apk/debug/app-debug.apk`.
4. Install the APK:
   - Transfer the APK to your Android device (e.g., via USB or email).
   - Enable **Install from Unknown Sources** in device settings.
   - Tap the APK to install.
5. Launch the app on the device.

### Troubleshooting
- **Gradle Sync Fails**:
  - Ensure internet connectivity.
  - Update Android Studio and Gradle plugins.
  - Check `build.gradle` for correct dependencies (e.g., `com.github.PhilJay:MPAndroidChart:v3.1.0`, `com.google.firebase:firebase-firestore:25.1.0`).
- **Firebase Errors**:
  - Verify `google-services.json` is in `app/`.
  - Ensure Firebase project has Firestore enabled.
  - Check internet for Firebase Authentication and Firestore.
- **Authentication Errors**:
  - Ensure internet connectivity for credential validation.
  - Clear app data via **Settings > Apps > Cashify > Storage > Clear Data**.
- **Firestore Issues**:
  - Check Firestore rules (e.g., `allow read, write: if request.auth.uid == userId;`).
  - Ensure offline persistence is enabled (default in Firestore).
- **Emulator Crashes**:
  - Use a compatible API (33 or 35).
  - Increase emulator RAM in **Device Manager**.
- **APK Compatibility**:
  - Ensure the device runs Android 7.0+ (API 24).
  - Rebuild the APK if installation fails.

## Features

### Part 2: Core Features

#### User Registration and Authentication
- **Signup**: Create an account with username, email, and password.
- **Login**: Access the app with email and password.
- **Switching**: Toggle between signup and login pages.

#### Budget Entries
- **Add Expense**:
  - Specify description, category, amount, type (income/expense), date (dd/MM/yyyy), start/end times, and optionally attach a photo (e.g., receipt).
  - Photos are clickable to view full-size.
- **View Expenses**:
  - List all expenses with details (category, amount, date, time, photo).
  - Filter by user-selectable period (e.g., month, year).

#### Categories
- **Add Category**: Create custom categories for expenses (e.g., Food, Entertainment).
- **View Spending**:
  - Display total spending per category for a selected period.
  - Visualize spending trends.

#### Goals
- **Set Goals**:
  - Define monthly minimum and maximum spending goals for categories (e.g., Entertainment: min R1000, max R2500).
  - Specify goal month (MM/yyyy) and optionally add a photo.
- **Track Progress**:
  - View total spent, progress bar, and status (Below Goal, Within Budget, Over Budget).
  - Expenses in the goal’s month contribute to `totalSpent` (e.g., 550/2500).

#### Other
- **Local Storage**: All data (expenses, goals, categories) stored in RoomDB for offline access.
- **Photo Support**: Efficient image loading with Glide; placeholders for missing/invalid photos.
- **Responsive UI**: Material Design components and bottom navigation for seamless interaction.

### Part 3: Advanced Features

#### Currency Converter
- Convert expense and income amounts to a user-selected currency (e.g., ZAR, USD, EUR).
- Displays amounts with appropriate currency symbols (e.g., R, $, €) in all views (Home, Categories, Reports).
- Implemented as a utility class (`CurrencyConverter.kt`), applied to expense lists and charts.
- Makes use of an API to convert currencies.
- Supports consistent financial tracking across different currencies.
- This feature was newly introduced in Part 3 and was not present in Part 1.

#### Reminders Page
- New page accessible via bottom navigation or settings.
- Allows users to set reminders for:
  - Entering daily expenses or income.
  - Reviewing budget goal progress.
  - Upcoming due dates for categories.
- Uses Firebase Cloud Messaging (FCM) for push notifications, even when the app is closed.
- UI includes options to set reminder range (day, week, month) and time.
- Builds on the Calendar feature from Part 1 by adding proactive notifications and user-defined reminders.

#### Graph Page (Reports)
- Dedicated **Reports Page** (`ReportsActivity.kt`) for visualizing financial data.
- Features two charts powered by MPAndroidChart:
  - **Spending Chart**: Bar chart comparing expenses (blue) and income (green) by category, with min goal (magenta line) and max goal (red line) overlays.
  - **Goals Chart**: Bar chart showing min goals (green) and max goals (red) per category.
- Filterable by period: last week, last month, or custom date range (via date picker).
- Left-aligned legends for clarity (e.g., "Expenses," "Income," "Min Goal," "Max Goal").
- Displays currency-converted amounts with symbols.
- Shows a toast ("No transactions for this time period") when no data is available.

#### Firebase Cloud Storage
- Replaced RoomDB with Firebase Firestore for cloud-based storage (`ExpenseRepository.kt`, `GoalRepository.kt`).
- Stores expenses, goals, and categories in a user-specific collection (`users/{userId}`).
- Supports:
  - Real-time data syncing across devices.
  - Offline access with Firestore’s built-in caching.
  - Secure access via Firebase Authentication (user ID-based rules).
- Data structure:
  - Expenses: `category`, `amount`, `type` (income/expense), `date` (dd/MM/yyyy), `timestamp`, `photoPath`.
  - Goals: `category`, `month` (MM/yyyy), `minGoal`, `maxGoal`, `photoPath`.
- Ensures data persistence and backup, replacing local-only storage from Part 2.

## Usage Instructions

### Signup Page
1. Open the app; the **Signup Page** appears by default.
2. Enter a username, valid email, and password (minimum 6 characters).
3. Click **Signup** to create an account and go to the **Login Page**.
4. Already have an account? Click **Switch to Login** to go to the **Login Page**.
5. Enable notifications in **Settings > Apps > Cashify** for reminders.

### Login Page
1. Enter your email and password.
2. Click **Login** to access the **Home Page**.
3. Need to create an account? Click **Switch to Signup** to go to the **Signup Page**.

### Home Page
1. Displays a list of all expenses (`rvExpenses`) with details:
   - Category, amount (green for income, red for expense, with currency symbol), date, description, time range, photo (if added).
2. Click an expense’s photo (`ivPhoto`) to view it full-size in `PhotoViewActivity` (click **Back** to return).
3. Click **Add Expense** to open the **Add Expense Page**.
4. Use the bottom navigation bar to switch pages:
   - **Home** (current).
   - **Categories** (Categories Page).
   - **Goals** (Goals Page).
   - **Reports** (Graph Page).
   - **Reminders** (Reminders Page).

### Add Expense Page
1. Enter details:
   - **Description**: Optional note (e.g., "Lunch at Cafe").
   - **Category**: Select or enter a category (e.g., "Food").
   - **Amount**: Numeric value (e.g., 100.50, converted to selected currency).
   - **Type**: Choose "income" or "expense".
   - **Date**: Select a date (format: dd/MM/yyyy, e.g., 15/05/2025).
   - **Start/End Time**: Specify time range (e.g., 12:00–13:00).
   - **Photo**: Optionally attach an image (e.g., receipt) via gallery or camera.
2. Click **Save** to add the expense and return to the **Home Page**.
3. The expense syncs to Firestore and appears in the list, contributing to category/goal totals.

### Categories Page
1. View a list of categories and total spending (in selected currency) for a selected period (e.g., May 2025).
2. Click a category to view expenses/income (`CategoryExpensesActivity`).
3. Click **Add Category** to create a new category.
4. Select a period to filter spending (e.g., month, year).
5. Navigate to other pages via the bottom nav bar.

### Goals Page
1. Displays a list of goals (`rvGoals`) with:
   - Category, month (MM/yyyy), created date, description, type, min/max goals (in selected currency), photo, spent amount, progress bar, status.
2. Click **Add Goal** to open the **Add Goal Page**.
3. Verify expenses contribute to goals:
   - Example: Goal for "Entertainment" (month: 05/2025, min: $100, max: $250).
   - Expense: $55 on 15/05/2025 → Shows "Spent: $55.00", 22% progress, "Below Goal".
4. Navigate via the bottom nav bar.

### Add Goal Page
1. Enter details:
   - **Category**: Select or enter a category (e.g., "Entertainment").
   - **Type**: Choose "income" or "expense".
   - **Month**: Select a month (MM/yyyy, e.g., 05/2025).
   - **Min Goal**: Minimum target (e.g., 100, in selected currency).
   - **Max Goal**: Maximum budget (e.g., 250).
   - **Description**: Optional note.
   - **Photo**: Optionally attach an image.
2. Click **Save** to add the goal to Firestore and return to the **Goals Page**.
3. The goal tracks expenses in the specified month.

### Reports Page (Graph Page)
1. Access via bottom navigation (**Reports**).
2. Select a period (last week, last month, custom range) via a spinner.
3. View:
   - **Spending Chart**: Expenses (blue bars), income (green bars), min goals (magenta lines), max goals (red lines) by category.
   - **Goals Chart**: Min goals (green bars), max goals (red bars) by category.
4. Amounts display in the selected currency (e.g., R, $).
5. If no transactions exist, a toast shows: "No transactions for this time period".
6. Navigate to other pages via the bottom nav bar.

### Reminders Page
1. Access via bottom navigation or settings.
2. Set reminders for:
   - Daily expense/income entry.
   - Budget goal progress (e.g., nearing max goal).
   - Bill or subscription due dates.
3. Choose frequency (daily, weekly) and time.
4. Save to enable push notifications via Firebase Cloud Messaging.
5. Navigate to other pages via the bottom nav bar.

## Contributing
1. Fork the repository.
2. Create a branch (`git checkout -b feature/your-feature`).
3. Commit changes (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request with a clear description.
6. Team members (Ibrahim, Matthew, Tiffany) will review and merge.

**Guidelines**:
- Follow Kotlin coding standards.
- Update Firestore rules for data changes.
- Test on API 33/35 emulators.
- Document new features in this README.

## Design Considerations
Cashify was designed with a user-first approach, prioritizing accessibility, clarity, and responsiveness. Key design principles included:

- **Mobile-First UI**: Optimized for small screens using Material Design.
- **Offline-First Architecture**: Local RoomDB ensures the app remains usable without internet access.
- **Cloud Synchronization**: Firebase Firestore provides seamless real-time syncing across devices.
- **Modular Codebase**: Features are separated into repositories and services (e.g., `ExpenseRepository`, `GoalRepository`) for maintainability.
- **Asynchronous Operations**: Kotlin Coroutines were used to ensure smooth UI interactions during data loading.

## GitHub and CI/CD
Cashify's development leveraged GitHub for version control and collaboration. Key practices included:

- *Branching Strategy*: Feature branches were created for major modules (e.g., feature-auth, feature-goals), then merged via pull requests after code review.
- *Issues and Project Boards*: GitHub Issues were used to track bugs and feature requests. A GitHub Project Board tracked development progress.

### GitHub Actions
GitHub Actions was used to automate key workflows:
- *CI Pipeline*: Automatically builds the project on push to main and dev branches to ensure code integrity.
- *Linting and Static Analysis*: Runs ktlint and detekt to maintain code quality.
- *Unit Test Execution*: Executes all unit tests on pull request creation.
- *APK Build Artifact*: Uploads debug APKs as build artifacts for easy testing and distribution.

## License
[MIT License](LICENSE)  
Copyright © 2025 Ibrahim Ghogawala, Matthew Mason, Tiffany Mather
