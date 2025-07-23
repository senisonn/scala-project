# Ridesharing Platform - Scala Console Application

A comprehensive peer-to-peer ridesharing platform built with Scala 3, PostgreSQL, and JDBC. This console application provides all essential ridesharing features including user management, trip creation, reservations, payments, ratings, and messaging.

## 🚀 Features

### Core Functionality
- **User Management**: Registration, authentication, profile management
- **Trip Management**: Create, search, and manage trips
- **Reservations**: Book seats, cancel reservations, view booking history
- **Payment System**: Simulated payment processing between users
- **Rating System**: Rate drivers and passengers after completed trips
- **Messaging System**: Communication between users for trip coordination

### User Roles
- **Passengers**: Search and book trips, rate drivers, manage reservations
- **Drivers**: Create trips, manage vehicles, view passenger reservations

## 🏗️ Architecture

### Project Structure
```
ridesharing-platform/
├── src/main/scala/
│   ├── models/           # Case classes (User, Trip, Reservation, etc.)
│   ├── dao/              # Data Access Objects with JDBC
│   ├── services/         # Business logic layer
│   ├── utils/            # Database connection utilities
│   └── RidesharingApp.scala  # Main console application
├── sql/
│   └── database.sql      # PostgreSQL schema and sample data
├── docs/                 # Documentation and diagrams
└── README.md
```

### Technical Stack
- **Language**: Scala 3.3.6
- **Database**: PostgreSQL 16+
- **Connectivity**: JDBC with prepared statements
- **Security**: BCrypt password hashing
- **Build Tool**: SBT

## 🚀 Getting Started

### Option 1: Docker (Recommended) 🐳

The easiest way to run the application with all dependencies:

⚠️ Important – Using docker attach

When you start the application with:
```bash
docker-compose up --build
```
The initial terminal interaction (e.g., a menu or prompt with choices) is only displayed in the terminal where docker-compose was launched.

If you then attach to the container using:
```bash
docker attach scala_project
```

You won’t see the initial options, because they were already displayed in the first terminal.
However, once the first input is entered in the docker-compose terminal, the interaction will resume normally in the attached terminal.

✅ Tip: Make the initial selection from the docker-compose terminal, then use docker attach if needed to continue interacting.

For detailed Docker instructions, see [README-DOCKER.md](README-DOCKER.md).

### Option 2: Manual Setup

#### Prerequisites

- **Java**: JDK 11 or higher
- **Scala**: 3.3.6+ (managed by SBT)
- **PostgreSQL**: 12+ with running instance
- **SBT**: Scala Build Tool

## ⚙️ Manual Setup Instructions

### 1. Database Setup

1. **Install PostgreSQL** (if not already installed):
   ```bash
   # macOS with Homebrew
   brew install postgresql
   brew services start postgresql
   
   # Ubuntu/Debian
   sudo apt update
   sudo apt install postgresql postgresql-contrib
   sudo systemctl start postgresql
   ```

2. **Create Database and User**:
   ```bash
   # Connect to PostgreSQL
   psql -U postgres
   
   # Create database
   CREATE DATABASE ridesharing;
   
   # Create user (optional, or use existing postgres user)
   CREATE USER ridesharing_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE ridesharing TO ridesharing_user;
   
   # Exit psql
   \q
   ```

3. **Initialize Database Schema**:
   ```bash
   # From project root directory
   psql -U postgres -d ridesharing -f sql/database.sql
   ```

### 2. Application Configuration

1. **Clone/Download the Project**:
   ```bash
   cd /path/to/your/project
   ```

2. **Update Database Connection** (if needed):
   Edit `src/main/scala/utils/DatabaseConnection.scala`:
   ```scala
   private val url = "jdbc:postgresql://localhost:5432/ridesharing"
   private val username = "postgres"  // or your username
   private val password = "password"  // your password
   ```

### 3. Build and Run

1. **Install Dependencies**:
   ```bash
   sbt update
   ```

2. **Compile the Project**:
   ```bash
   sbt compile
   ```

3. **Run the Application**:
   ```bash
   sbt run
   ```

## 📱 Usage Guide

### First Time Setup
1. Run the application with `sbt run`
2. Choose "Register" from the main menu
3. Create your account (passenger or driver)
4. Login with your credentials

### As a Passenger
1. **Search Trips**: Find available rides by city and date
2. **Book Seats**: Reserve seats on available trips
3. **Manage Reservations**: View and manage your bookings
4. **Rate Drivers**: Provide feedback after completed trips

### As a Driver
1. **Add Vehicles**: Register your vehicles for trips
2. **Create Trips**: Publish new ride offers
3. **Manage Trips**: View and update your trips
4. **View Reservations**: See passenger bookings

### Sample User Flow
```
1. Register as driver → 2. Add vehicle → 3. Create trip
4. Register as passenger → 5. Search trips → 6. Book trip
7. Complete trip → 8. Exchange ratings
```

## 🗄️ Database Schema

### Main Tables
- **users**: User accounts and profiles
- **vehicles**: Driver vehicles
- **trips**: Trip offers with routes and pricing
- **reservations**: Passenger bookings
- **payments**: Transaction records
- **ratings**: User ratings and reviews
- **messages**: Inter-user communication

### Key Relationships
- Users can be drivers (1:N with vehicles and trips)
- Trips belong to drivers and vehicles (N:1)
- Reservations link passengers to trips (N:N)
- Payments track money flow between users
- Ratings provide feedback system
- Messages enable user communication

## 🧪 Testing

### Sample Data
The `database.sql` file includes sample users and trips for testing:
- **Email**: `john.doe@email.com`, **Password**: `password123`
- **Email**: `jane.smith@email.com`, **Password**: `password123`
- **Email**: `bob.driver@email.com`, **Password**: `password123`

### Test Scenarios
1. **User Registration**: Create new passenger and driver accounts
2. **Trip Creation**: Driver creates trip from Paris to Lyon
3. **Trip Search**: Passenger searches and books available trips
4. **Reservation Management**: View and cancel bookings
5. **Rating Exchange**: Rate other users after trips

## 🔧 Development

### Code Quality
- **Validation**: Input validation on all user inputs
- **Error Handling**: Comprehensive Try/Success/Failure pattern usage
- **Security**: BCrypt password hashing, SQL injection prevention
- **Architecture**: Clear separation of concerns (Model-DAO-Service layers)

### Database Best Practices
- **Prepared Statements**: All queries use prepared statements
- **Transactions**: Proper transaction management
- **Indexes**: Performance optimization with strategic indexes
- **Constraints**: Data integrity with foreign keys and checks

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Error**:
   - Verify PostgreSQL is running: `brew services list | grep postgresql`
   - Check connection parameters in `DatabaseConnection.scala`
   - Ensure database and tables exist

2. **Compilation Errors**:
   - Clean and rebuild: `sbt clean compile`
   - Check Scala version compatibility

3. **Runtime Exceptions**:
   - Verify sample data is loaded: Check `sql/database.sql`
   - Check logs for specific error messages

### Performance Tips
- Use connection pooling for production deployment
- Monitor database query performance
- Consider adding more indexes for frequent queries

## 📚 Academic Project Context

This project fulfills the requirements for a Scala ridesharing platform assignment including:

### Phase 1: Analysis & Design ✅
- Business entity identification and relationships
- Complete database schema design
- UML class diagrams (see `docs/` folder)

### Phase 2: Implementation ✅
- Scala 3 case classes for all entities
- JDBC-based DAO pattern with prepared statements
- Interactive console application with role-based menus
- Comprehensive error handling and validation

### Phase 3: Testing & Documentation ✅
- Complete setup and usage documentation
- Code quality with Scala best practices
- End-to-end testing scenarios

## 🤝 Contributing

This is an academic project, but suggestions and improvements are welcome:
1. Fork the repository
2. Create a feature branch
3. Follow Scala coding standards
4. Add appropriate tests
5. Submit a pull request

## 📄 License

Educational project - free to use and modify for learning purposes.

---

**Project Status**: ✅ Complete and functional
**Last Updated**: December 2024
**Scala Version**: 3.3.6
**Database**: PostgreSQL 12+