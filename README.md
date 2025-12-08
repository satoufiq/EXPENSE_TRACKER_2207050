# Expense Tracker Application

A comprehensive JavaFX desktop application for tracking personal, group, and child expenses with analytics and PDF reporting.

## Features

### 🔹 Three Modes of Operation

1. **Personal Mode**
   - Track individual expenses
   - View analytics and trends
   - Generate detailed PDF reports

2. **Group Mode**
   - Collaborative expense tracking
   - Share expenses with group members
   - View individual and group analytics
   - Group spending reports

3. **Parent Mode**
   - Monitor child's expenses
   - Receive alerts for unusual spending
   - View child analytics
   - Generate supervision reports

## Technology Stack

- **Frontend:** JavaFX 21
- **Database:** SQLite (JDBC)
- **PDF Generation:** iText 5
- **Build Tool:** Maven
- **Java Version:** 17

## Project Structure

```
Expense_Tracker_2207050/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/
│   │   │       ├── Main.java                 # Application entry point
│   │   │       ├── MainApp.java              # JavaFX Application class
│   │   │       ├── controller/               # All FXML controllers
│   │   │       ├── model/                    # Data models (User, Expense, Group)
│   │   │       ├── service/                  # Business logic services
│   │   │       └── util/                     # Utility classes
│   │   └── resources/
│   │       ├── css/
│   │       │   └── styles.css               # Application stylesheet
│   │       └── fxml/                         # All FXML view files
│   └── test/
│       └── java/                             # Unit tests
├── pom.xml                                   # Maven configuration
└── README.md                                 # This file
```

## Database Schema

### USERS
- user_id (TEXT, PRIMARY KEY)
- name (TEXT)
- email (TEXT, UNIQUE)
- password (TEXT)
- role (TEXT) - "normal" or "parent"

### GROUPS
- group_id (TEXT, PRIMARY KEY)
- group_name (TEXT)

### GROUP_MEMBERS
- id (INTEGER, PRIMARY KEY)
- group_id (TEXT, FOREIGN KEY)
- user_id (TEXT, FOREIGN KEY)

### EXPENSES
- expense_id (TEXT, PRIMARY KEY)
- user_id (TEXT, FOREIGN KEY)
- group_id (TEXT, nullable)
- category (TEXT)
- amount (REAL)
- date (TEXT)
- note (TEXT)

### PARENT_RELATION
- parent_id (TEXT, FOREIGN KEY)
- child_id (TEXT, FOREIGN KEY)

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation Steps

1. **Clone or download the project**
   ```bash
   cd C:\Users\shahr\Downloads\Expense_Tracker_2207050
   ```

2. **Install dependencies**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn javafx:run
   ```

   Or run using:
   ```bash
   java -jar target/Expense_Tracker_2207050-1.0-SNAPSHOT.jar
   ```

### Running from IDE (IntelliJ IDEA / Eclipse)

1. Import the project as a Maven project
2. Wait for Maven to download dependencies
3. Run the `Main.java` class

## Development Status

### ✅ Completed
- Project structure setup
- Database schema and initialization
- Model classes (User, Expense, Group)
- Utility classes (SessionManager, AlertUtil)
- Basic services (DatabaseHelper, UserService)
- Main application flow (MainApp.java)
- Home screen with navigation
- Comprehensive CSS styling

### 🔨 To Be Implemented (Step by Step)
1. Login & Register screens
2. Mode Selection screen
3. Personal Mode features
4. Group Mode features
5. Parent Mode features
6. Analytics services
7. PDF report generation
8. Alert engine

## Usage Flow

1. **Launch Application** → Home Screen
2. **Login/Register** → Authentication
3. **Select Mode** → Personal / Group / Parent
4. **Dashboard** → Mode-specific features
5. **Add/View/Analyze** → Expenses and reports

## Contributing

This is a student project (ID: 2207050). Development will be done step by step with approval at each stage.

## License

Educational project - All rights reserved.

## Contact

Project ID: 2207050
Year: 2024

---

**Note:** The application automatically creates `expense_tracker.db` in the project root on first run. This file stores all application data.

