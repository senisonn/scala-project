package dao

import models.Vehicle
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet

class VehicleDAO {
  
  def create(vehicle: Vehicle): Try[Vehicle] = {
    val sql = """
      INSERT INTO vehicles (owner_id, make, model, year, color, license_plate, seats)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        vehicle.ownerId,
        vehicle.make,
        vehicle.model,
        vehicle.year,
        vehicle.color.orNull,
        vehicle.licensePlate,
        vehicle.seats
      )
      createdVehicle <- findById(id)
    } yield createdVehicle.get
  }
  
  def findById(id: Int): Try[Option[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToVehicle(rs))
      else None
    }
  }
  
  def findByOwner(ownerId: Int): Try[List[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE owner_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, ownerId) { rs =>
      val vehicles = scala.collection.mutable.ListBuffer[Vehicle]()
      while (rs.next()) {
        vehicles += mapResultSetToVehicle(rs)
      }
      vehicles.toList
    }
  }
  
  def findAll(): Try[List[Vehicle]] = {
    val sql = "SELECT * FROM vehicles ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val vehicles = scala.collection.mutable.ListBuffer[Vehicle]()
      while (rs.next()) {
        vehicles += mapResultSetToVehicle(rs)
      }
      vehicles.toList
    }
  }
  
  def update(vehicle: Vehicle): Try[Vehicle] = {
    val sql = """
      UPDATE vehicles 
      SET make = ?, model = ?, year = ?, color = ?, license_plate = ?, 
          seats = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    """
    
    for {
      _ <- DatabaseConnection.executeUpdate(
        sql,
        vehicle.make,
        vehicle.model,
        vehicle.year,
        vehicle.color.orNull,
        vehicle.licensePlate,
        vehicle.seats,
        vehicle.id.get
      )
      updatedVehicle <- findById(vehicle.id.get)
    } yield updatedVehicle.get
  }
  
  def delete(id: Int): Try[Boolean] = {
    val sql = "DELETE FROM vehicles WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, id).map(_ > 0)
  }
  
  def findByLicensePlate(licensePlate: String): Try[Option[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE license_plate = ?"
    
    DatabaseConnection.executeQuery(sql, licensePlate) { rs =>
      if (rs.next()) Some(mapResultSetToVehicle(rs))
      else None
    }
  }
  
  private def mapResultSetToVehicle(rs: ResultSet): Vehicle = {
    Vehicle(
      id = Some(rs.getInt("id")),
      ownerId = rs.getInt("owner_id"),
      make = rs.getString("make"),
      model = rs.getString("model"),
      year = rs.getInt("year"),
      color = Option(rs.getString("color")),
      licensePlate = rs.getString("license_plate"),
      seats = rs.getInt("seats"),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime),
      updatedAt = Option(rs.getTimestamp("updated_at")).map(_.toLocalDateTime)
    )
  }
}