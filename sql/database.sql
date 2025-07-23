-- Ridesharing Platform Database Schema
-- PostgreSQL 12+ compatible

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS trips CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Create Users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    is_driver BOOLEAN DEFAULT FALSE,
    driver_license VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Vehicles table
CREATE TABLE vehicles (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL,
    color VARCHAR(30),
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    seats INTEGER NOT NULL CHECK (seats > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Trips table
CREATE TABLE trips (
    id SERIAL PRIMARY KEY,
    driver_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    departure_city VARCHAR(100) NOT NULL,
    arrival_city VARCHAR(100) NOT NULL,
    departure_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
    price_per_seat DECIMAL(10,2) NOT NULL CHECK (price_per_seat >= 0),
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Reservations table
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    passenger_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seats_reserved INTEGER NOT NULL CHECK (seats_reserved > 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trip_id, passenger_id)
);

-- Create Payments table
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    payer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) DEFAULT 'CASH',
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Ratings table
CREATE TABLE ratings (
    id SERIAL PRIMARY KEY,
    trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    rater_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rated_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trip_id, rater_id, rated_id)
);

-- Create Messages table
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trip_id INTEGER REFERENCES trips(id) ON DELETE SET NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_vehicles_owner_id ON vehicles(owner_id);
CREATE INDEX idx_trips_driver_id ON trips(driver_id);
CREATE INDEX idx_trips_departure_city ON trips(departure_city);
CREATE INDEX idx_trips_arrival_city ON trips(arrival_city);
CREATE INDEX idx_trips_departure_date ON trips(departure_date);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_reservations_trip_id ON reservations(trip_id);
CREATE INDEX idx_reservations_passenger_id ON reservations(passenger_id);
CREATE INDEX idx_payments_reservation_id ON payments(reservation_id);
CREATE INDEX idx_ratings_trip_id ON ratings(trip_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);

-- Insert sample data for testing
INSERT INTO users (email, password_hash, first_name, last_name, phone, is_driver, driver_license) VALUES
('john.doe@email.com', '$2a$10$rK6XmkPqO0Y9BLsf/QW1Je9dXxZj1yAZQYqRqOg5VzCJKMj2NqU8K', 'John', 'Doe', '+33123456789', TRUE, 'AB123456'),
('jane.smith@email.com', '$2a$10$rK6XmkPqO0Y9BLsf/QW1Je9dXxZj1yAZQYqRqOg5VzCJKMj2NqU8K', 'Jane', 'Smith', '+33987654321', FALSE, NULL),
('bob.driver@email.com', '$2a$10$rK6XmkPqO0Y9BLsf/QW1Je9dXxZj1yAZQYqRqOg5VzCJKMj2NqU8K', 'Bob', 'Driver', '+33555123456', TRUE, 'CD789012');

INSERT INTO vehicles (owner_id, make, model, year, color, license_plate, seats) VALUES
(1, 'Toyota', 'Corolla', 2020, 'Blue', 'ABC-123', 4),
(3, 'Volkswagen', 'Golf', 2019, 'Red', 'XYZ-789', 5);

INSERT INTO trips (driver_id, vehicle_id, departure_city, arrival_city, departure_date, departure_time, available_seats, price_per_seat, description) VALUES
(1, 1, 'Paris', 'Lyon', '2024-02-01', '09:00', 3, 25.00, 'Comfortable trip to Lyon'),
(3, 2, 'Lyon', 'Marseille', '2024-02-02', '14:30', 4, 30.00, 'Direct route to Marseille');