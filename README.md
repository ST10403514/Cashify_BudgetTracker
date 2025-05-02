# Cashify
**Save Smart, Spend Wise**

![Cashify Logo](assets/Cashify_Logo.gif)

## GitHub Link
[https://github.com/ST10403514/Cashify_BudgetTracker](https://github.com/ST10403514/Cashify_BudgetTracker)

## Demonstration Video
(https://youtu.be/cs7XJiEM3zw) 

## Team Information
**Members**:
- Ibrahim Ghogawala (ST10286968)
- Matthew Mason (ST10403514)
- Tiffany Mather (ST10249863)

**Course**: BCAD Year 3  
**Module**: Programming 3C (PROG7313)  
**Assessment**: Portfolio of Evidence (POE) Part 2

## Overview
Cashify is an intuitive budget tracking app designed to empower users to manage their finances with clarity and ease. It promotes mindful spending by enabling users to track expenses, set category-specific goals, and analyze spending patterns. Key features include:

- **Simple UI**: Clean, user-friendly interface for seamless navigation.
- **Offline Access**: Local storage via RoomDB ensures functionality without internet.
- **Secure Authentication**: User credentials stored securely for signup and login.
- **Photo Support**: Attach and view photos (e.g., receipts) for expenses.
- **Goal Tracking**: Set monthly minimum and maximum spending goals per category.
- **Insightful Analytics**: View spending by category and track progress toward goals.

Cashify is built for reliability, security, and responsiveness, making it ideal for daily financial management.

## Technologies Used
- **Firebase Authentication**: Stores user credentials for secure signup and login.
- **RoomDB**: Local database for offline expense, goal, and category storage.
- **Android Studio**: Development environment (Kotlin, XML).
- **Kotlin**: Primary programming language for logic and UI.
- **XML**: Layout design for activities and fragments.
- **Glide**: Efficient image loading for expense and goal photos.
- **Material Design**: UI components for a modern look and feel.

## Setup Instructions

### Prerequisites
- **Android Studio**: Latest version (e.g., Koala | 2024.1.1 or later).
- **Emulator**: Pixel 8 API 35 or Medium Phone API 33, or a physical Android device (API 24+).
- **Git**: For cloning the repository.
- **Disk Space**: At least 5GB free.
- **Internet**: For Gradle sync.

### Installation

#### Option 1: Clone the Repository
1. Navigate to the GitHub repository: [https://github.com/ST10403514/Cashify_BudgetTracker](https://github.com/ST10403514/Cashify_BudgetTracker).
2. Click the green **Code** button and copy the HTTPS URL.
3. Open **Android Studio**.
4. Select **File > New > Project from Version Control**.
5. Paste the URL and click **Clone**.
6. Wait for Gradle to sync (click **Sync Project with Gradle Files** if needed).
7. Run the app:
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
  - Check `build.gradle` for correct dependencies (e.g., `com.github.bumptech.glide:glide:4.16.0`).
- **Authentication Errors**:
  - Ensure internet connectivity for credential validation.
  - Clear app data via **Settings > Apps > Cashify > Storage > Clear Data**.
- **RoomDB Issues**:
  - Check database migrations (e.g., for `createdAt` in `goals` table).
  - Clear app data to reset the database.
- **Emulator Crashes**:
  - Use a compatible API (33 or 35).
  - Increase emulator RAM in **Device Manager**.
- **APK Compatibility**:
  - Ensure the device runs Android 7.0+ (API 24).
  - Rebuild the APK if installation fails.

## Features

### User Registration and Authentication
- **Signup**: Create an account with username, email, and password.
- **Login**: Access the app with email and password.
- **Switching**: Toggle between signup and login pages.

### Budget Entries
- **Add Expense**:
  - Specify description, category, amount, type (income/expense), date (dd/MM/yyyy), start/end times, and optionally attach a photo (e.g., receipt).
  - Photos are clickable to view full-size.
- **View Expenses**:
  - List all expenses with details (category, amount, date, time, photo).
  - Filter by user-selectable period (e.g., month, year).

### Categories
- **Add Category**: Create custom categories for expenses (e.g., Food, Entertainment).
- **View Spending**:
  - Display total spending per category for a selected period.
  - Visualize spending trends.

### Goals
- **Set Goals**:
  - Define monthly minimum and maximum spending goals for categories (e.g., Entertainment: min R1000, max R2500).
  - Specify goal month (MM/yyyy) and optionally add a photo.
- **Track Progress**:
  - View total spent, progress bar, and status (Below Goal, Within Budget, Over Budget).
  - Expenses in the goal’s month contribute to `totalSpent` (e.g., 550/2500).

### Other
- **Local Storage**: All data (expenses, goals, categories) stored in RoomDB for offline access.
- **Photo Support**: Efficient image loading with Glide; placeholders for missing/invalid photos.
- **Responsive UI**: Material Design components and bottom navigation for seamless interaction.

## Usage Instructions

### Signup Page
1. Open the app; the **Signup Page** appears by default.
2. Enter a username, valid email, and password (minimum 6 characters).
3. Click **Signup** to create an account and go to the **Login Page**.
4. Already have an account? Click **Switch to Login** to go to the **Login Page**.

### Login Page
1. Enter your email and password.
2. Click **Login** to access the **Home Page**.
3. Need to create an account? Click **Switch to Signup** to go to the **Signup Page**.

### Home Page
1. Displays a list of all expenses (`rvExpenses`) with details:
   - Category, amount (green for income, red for expense), date, description, time range, photo (if added).
2. Click an expense’s photo (`ivPhoto`) to view it full-size in `PhotoViewActivity` (click **Back** to return).
3. Click **Add Expense** to open the **Add Expense Page**.
4. Use the bottom navigation bar to switch pages:
   - **Home** (current).
   - **Categories** (Categories Page).
   - **Goals** (Goals Page).

### Add Expense Page
1. Enter details:
   - **Description**: Optional note (e.g., "Lunch at Cafe").
   - **Category**: Select or enter a category (e.g., "Food").
   - **Amount**: Numeric value (e.g., 100.50).
   - **Type**: Choose "income" or "expense".
   - **Date**: Select a date (format: dd/MM/yyyy, e.g., 15/05/2025).
   - **Start/End Time**: Specify time range (e.g., 12:00–13:00).
   - **Photo**: Optionally attach an image (e.g., receipt) via gallery or camera.
2. Click **Save** to add the expense and return to the **Home Page**.
3. The expense appears in the list and contributes to category/goal totals (if in the goal’s month).

### Categories Page
1. View a list of categories and total spending for a selected period (e.g., May 2025).
2. Click **Add Category** to create a new category.
3. Select a period to filter spending (e.g., month, year).
4. Navigate to other pages via the bottom nav bar.

### Goals Page
1. Displays a list of goals (`rvGoals`) with:
   - Category, month (MM/yyyy), created date, description, type, min/max goals, photo, spent amount, progress bar, status.
2. Click **Add Goal** to open the **Add Goal Page**.
3. Verify expenses contribute to goals:
   - Example: Goal for "Entertainment" (month: 05/2025, min: R1000, max: R2500).
   - Expense: R550 on 15/05/2025 → Shows "Spent: R550.00", 22% progress, "Below Goal".
4. Navigate via the bottom nav bar.

### Add Goal Page
1. Enter details:
   - **Category**: Select or enter a category (e.g., "Entertainment").
   - **Type**: Choose "income" or "expense".
   - **Month**: Select a month (MM/yyyy, e.g., 05/2025).
   - **Min Goal**: Minimum target (e.g., 1000).
   - **Max Goal**: Maximum budget (e.g., 2500).
   - **Description**: Optional note.
   - **Photo**: Optionally attach an image.
2. Click **Save** to add the goal and return to the **Goals Page**.
3. The goal tracks expenses in the specified month.


## Future Requirements
- **Spending Graphs**:
  - Display daily and category-based spending charts for a selected period.
  - Use libraries like MPAndroidChart for visualization.
- **Progress Dashboard**:
  - Show a monthly overview of budget goal progress.
  - Highlight overspending categories in red.
- **Gamification**:
  - Award badges for meeting goals (e.g., "Budget Master" for 3 months within budget).
  - Add a points system for consistent tracking.
- **Notifications**:
  - Alert users when approaching or exceeding goal limits.
  - Schedule daily/weekly spending summaries.

## Contributing
1. Fork the repository.
2. Create a branch (`git checkout -b feature/your-feature`).
3. Commit changes (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request with a clear description.
6. Team members (Ibrahim, Matthew, Tiffany) will review and merge.

**Guidelines**:
- Follow Kotlin coding standards.
- Update RoomDB migrations for schema changes.
- Test on API 33/35 emulators.
- Document new features in this README.

## License
[MIT License](LICENSE)  
Copyright © 2025 Ibrahim Ghogawala, Matthew Mason, Tiffany Mather
