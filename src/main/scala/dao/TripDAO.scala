package dao

import models.{Trip, TripStatus}
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet
import java.time.LocalDate

class TripDAO {
  
  def create(trip: Trip): Try[Trip] = {
    val sql = """
      INSERT INTO trips (driver_id, vehicle_id, departure_city, arrival_city, departure_date, 
                        departure_time, available_seats, price_per_seat, description, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        trip.driverId,
        trip.vehicleId,
        trip.departureCity,
        trip.arrivalCity,
        trip.departureDate,
        trip.departureTime,
        trip.availableSeats,
        trip.pricePerSeat,
        trip.description.orNull,
        trip.status.toString
      )
      createdTrip <- findById(id)
    } yield createdTrip.get
  }
  
  def findById(id: Int): Try[Option[Trip]] = {
    val sql = "SELECT * FROM trips WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToTrip(rs))
      else None
    }
  }
  
  def findAll(): Try[List[Trip]] = {
    val sql = "SELECT * FROM trips ORDER BY departure_date, departure_time"
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  def findByDriver(driverId: Int): Try[List[Trip]] = {
    val sql = "SELECT * FROM trips WHERE driver_id = ? ORDER BY departure_date DESC"
    
    DatabaseConnection.executeQuery(sql, driverId) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  def searchTrips(departureCity: String, arrivalCity: String, departureDate: LocalDate): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE LOWER(departure_city) = LOWER(?) 
        AND LOWER(arrival_city) = LOWER(?) 
        AND departure_date = ? 
        AND status = 'ACTIVE' 
        AND available_seats > 0
      ORDER BY departure_time
    """
    
    DatabaseConnection.executeQuery(sql, departureCity, arrivalCity, departureDate) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  def findAvailableTrips(): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE status = 'ACTIVE' 
        AND available_seats > 0 
        AND departure_date >= CURRENT_DATE
      ORDER BY departure_date, departure_time
    """
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  def updateAvailableSeats(tripId: Int, newAvailableSeats: Int): Try[Boolean] = {
    val sql = "UPDATE trips SET available_seats = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, newAvailableSeats, tripId).map(_ > 0)
  }
  
  def updateStatus(tripId: Int, status: TripStatus.TripStatus): Try[Boolean] = {
    val sql = "UPDATE trips SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, status.toString, tripId).map(_ > 0)
  }
  
  def update(trip: Trip): Try[Trip] = {
    val sql = """
      UPDATE trips 
      SET departure_city = ?, arrival_city = ?, departure_date = ?, departure_time = ?, 
          available_seats = ?, price_per_seat = ?, description = ?, status = ?, 
          updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    """
    
    for {
      _ <- DatabaseConnection.executeUpdate(
        sql,
        trip.departureCity,
        trip.arrivalCity,
        trip.departureDate,
        trip.departureTime,
        trip.availableSeats,
        trip.pricePerSeat,
        trip.description.orNull,
        trip.status.toString,
        trip.id.get
      )
      updatedTrip <- findById(trip.id.get)
    } yield updatedTrip.get
  }
  
  def delete(id: Int): Try[Boolean] = {
    val sql = "DELETE FROM trips WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, id).map(_ > 0)
  }
  
  def canDeleteTrip(tripId: Int): Try[Boolean] = {
    val sql = "SELECT COUNT(*) FROM reservations WHERE trip_id = ? AND status = 'ACTIVE'"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      rs.next()
      rs.getInt(1) == 0
    }
  }
  
  def findUpcomingTrips(driverId: Int): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE driver_id = ? 
        AND departure_date >= CURRENT_DATE 
        AND status = 'ACTIVE'
      ORDER BY departure_date, departure_time
    """
    
    DatabaseConnection.executeQuery(sql, driverId) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  def findPastTrips(driverId: Int): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE driver_id = ? 
        AND departure_date < CURRENT_DATE
      ORDER BY departure_date DESC, departure_time DESC
    """
    
    DatabaseConnection.executeQuery(sql, driverId) { rs =>
      val trips = scala.collection.mutable.ListBuffer[Trip]()
      while (rs.next()) {
        trips += mapResultSetToTrip(rs)
      }
      trips.toList
    }
  }
  
  private def mapResultSetToTrip(rs: ResultSet): Trip = {
    Trip(
      id = Some(rs.getInt("id")),
      driverId = rs.getInt("driver_id"),
      vehicleId = rs.getInt("vehicle_id"),
      departureCity = rs.getString("departure_city"),
      arrivalCity = rs.getString("arrival_city"),
      departureDate = rs.getDate("departure_date").toLocalDate,
      departureTime = rs.getTime("departure_time").toLocalTime,
      availableSeats = rs.getInt("available_seats"),
      pricePerSeat = BigDecimal(rs.getBigDecimal("price_per_seat")),
      description = Option(rs.getString("description")),
      status = TripStatus.withName(rs.getString("status")),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime),
      updatedAt = Option(rs.getTimestamp("updated_at")).map(_.toLocalDateTime)
    )
  }
}