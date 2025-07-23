package dao

import models.{Reservation, ReservationStatus}
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet

class ReservationDAO {
  
  def create(reservation: Reservation): Try[Reservation] = {
    val sql = """
      INSERT INTO reservations (trip_id, passenger_id, seats_reserved, total_price, status)
      VALUES (?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        reservation.tripId,
        reservation.passengerId,
        reservation.seatsReserved,
        reservation.totalPrice,
        reservation.status.toString
      )
      createdReservation <- findById(id)
    } yield createdReservation.get
  }
  
  def findById(id: Int): Try[Option[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToReservation(rs))
      else None
    }
  }
  
  def findAll(): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def findByPassenger(passengerId: Int): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE passenger_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, passengerId) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def findByTrip(tripId: Int): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE trip_id = ? ORDER BY created_at"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def findActiveByTrip(tripId: Int): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE trip_id = ? AND status = 'ACTIVE' ORDER BY created_at"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def findActiveByPassenger(passengerId: Int): Try[List[Reservation]] = {
    val sql = """
      SELECT r.* FROM reservations r
      JOIN trips t ON r.trip_id = t.id
      WHERE r.passenger_id = ? 
        AND r.status = 'ACTIVE' 
        AND t.departure_date >= CURRENT_DATE
      ORDER BY t.departure_date, t.departure_time
    """
    
    DatabaseConnection.executeQuery(sql, passengerId) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def findPastByPassenger(passengerId: Int): Try[List[Reservation]] = {
    val sql = """
      SELECT r.* FROM reservations r
      JOIN trips t ON r.trip_id = t.id
      WHERE r.passenger_id = ? 
        AND t.departure_date < CURRENT_DATE
      ORDER BY t.departure_date DESC, t.departure_time DESC
    """
    
    DatabaseConnection.executeQuery(sql, passengerId) { rs =>
      val reservations = scala.collection.mutable.ListBuffer[Reservation]()
      while (rs.next()) {
        reservations += mapResultSetToReservation(rs)
      }
      reservations.toList
    }
  }
  
  def updateStatus(reservationId: Int, status: ReservationStatus.ReservationStatus): Try[Boolean] = {
    val sql = "UPDATE reservations SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, status.toString, reservationId).map(_ > 0)
  }
  
  def update(reservation: Reservation): Try[Reservation] = {
    val sql = """
      UPDATE reservations 
      SET seats_reserved = ?, total_price = ?, status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    """
    
    for {
      _ <- DatabaseConnection.executeUpdate(
        sql,
        reservation.seatsReserved,
        reservation.totalPrice,
        reservation.status.toString,
        reservation.id.get
      )
      updatedReservation <- findById(reservation.id.get)
    } yield updatedReservation.get
  }
  
  def delete(id: Int): Try[Boolean] = {
    val sql = "DELETE FROM reservations WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, id).map(_ > 0)
  }
  
  def getTotalReservedSeats(tripId: Int): Try[Int] = {
    val sql = "SELECT COALESCE(SUM(seats_reserved), 0) FROM reservations WHERE trip_id = ? AND status = 'ACTIVE'"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      rs.next()
      rs.getInt(1)
    }
  }
  
  def hasActiveReservation(tripId: Int, passengerId: Int): Try[Boolean] = {
    val sql = "SELECT COUNT(*) FROM reservations WHERE trip_id = ? AND passenger_id = ? AND status = 'ACTIVE'"
    
    DatabaseConnection.executeQuery(sql, tripId, passengerId) { rs =>
      rs.next()
      rs.getInt(1) > 0
    }
  }
  
  def findReservationForTrip(tripId: Int, passengerId: Int): Try[Option[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE trip_id = ? AND passenger_id = ? ORDER BY created_at DESC LIMIT 1"
    
    DatabaseConnection.executeQuery(sql, tripId, passengerId) { rs =>
      if (rs.next()) Some(mapResultSetToReservation(rs))
      else None
    }
  }
  
  def getReservationStats(passengerId: Int): Try[(Int, Int, Int)] = {
    val sql = """
      SELECT 
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
        COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled
      FROM reservations 
      WHERE passenger_id = ?
    """
    
    DatabaseConnection.executeQuery(sql, passengerId) { rs =>
      rs.next()
      (rs.getInt("active"), rs.getInt("completed"), rs.getInt("cancelled"))
    }
  }
  
  private def mapResultSetToReservation(rs: ResultSet): Reservation = {
    Reservation(
      id = Some(rs.getInt("id")),
      tripId = rs.getInt("trip_id"),
      passengerId = rs.getInt("passenger_id"),
      seatsReserved = rs.getInt("seats_reserved"),
      totalPrice = BigDecimal(rs.getBigDecimal("total_price")),
      status = ReservationStatus.withName(rs.getString("status")),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime),
      updatedAt = Option(rs.getTimestamp("updated_at")).map(_.toLocalDateTime)
    )
  }
}