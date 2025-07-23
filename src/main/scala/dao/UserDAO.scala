package dao

import models.User
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet
import java.time.LocalDate

class UserDAO {
  
  def create(user: User): Try[User] = {
    val sql = """
      INSERT INTO users (email, password_hash, first_name, last_name, phone, date_of_birth, is_driver, driver_license)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        user.email,
        user.passwordHash,
        user.firstName,
        user.lastName,
        user.phone.orNull,
        user.dateOfBirth.orNull,
        user.isDriver,
        user.driverLicense.orNull
      )
      createdUser <- findById(id)
    } yield createdUser.get
  }
  
  def findById(id: Int): Try[Option[User]] = {
    val sql = "SELECT * FROM users WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToUser(rs))
      else None
    }
  }
  
  def findByEmail(email: String): Try[Option[User]] = {
    val sql = "SELECT * FROM users WHERE email = ?"
    
    DatabaseConnection.executeQuery(sql, email) { rs =>
      if (rs.next()) Some(mapResultSetToUser(rs))
      else None
    }
  }
  
  def findAll(): Try[List[User]] = {
    val sql = "SELECT * FROM users ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val users = scala.collection.mutable.ListBuffer[User]()
      while (rs.next()) {
        users += mapResultSetToUser(rs)
      }
      users.toList
    }
  }
  
  def update(user: User): Try[User] = {
    val sql = """
      UPDATE users 
      SET email = ?, first_name = ?, last_name = ?, phone = ?, date_of_birth = ?, 
          is_driver = ?, driver_license = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    """
    
    for {
      _ <- DatabaseConnection.executeUpdate(
        sql,
        user.email,
        user.firstName,
        user.lastName,
        user.phone.orNull,
        user.dateOfBirth.orNull,
        user.isDriver,
        user.driverLicense.orNull,
        user.id.get
      )
      updatedUser <- findById(user.id.get)
    } yield updatedUser.get
  }
  
  def delete(id: Int): Try[Boolean] = {
    val sql = "DELETE FROM users WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, id).map(_ > 0)
  }
  
  def findDrivers(): Try[List[User]] = {
    val sql = "SELECT * FROM users WHERE is_driver = true ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql) { rs =>
      val users = scala.collection.mutable.ListBuffer[User]()
      while (rs.next()) {
        users += mapResultSetToUser(rs)
      }
      users.toList
    }
  }
  
  def updatePassword(userId: Int, newPasswordHash: String): Try[Boolean] = {
    val sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, newPasswordHash, userId).map(_ > 0)
  }
  
  private def mapResultSetToUser(rs: ResultSet): User = {
    User(
      id = Some(rs.getInt("id")),
      email = rs.getString("email"),
      passwordHash = rs.getString("password_hash"),
      firstName = rs.getString("first_name"),
      lastName = rs.getString("last_name"),
      phone = Option(rs.getString("phone")),
      dateOfBirth = Option(rs.getDate("date_of_birth")).map(_.toLocalDate),
      isDriver = rs.getBoolean("is_driver"),
      driverLicense = Option(rs.getString("driver_license")),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime),
      updatedAt = Option(rs.getTimestamp("updated_at")).map(_.toLocalDateTime)
    )
  }
}