package services

import dao.UserDAO
import models.User
import org.mindrot.jbcrypt.BCrypt
import scala.util.{Try, Success, Failure}
import java.time.LocalDate

class AuthService(userDAO: UserDAO) {
  
  def registerUser(
      email: String,
      password: String,
      firstName: String,
      lastName: String,
      phone: Option[String] = None,
      dateOfBirth: Option[LocalDate] = None,
      isDriver: Boolean = false,
      driverLicense: Option[String] = None
  ): Try[User] = {
    for {
      _ <- validateEmail(email)
      _ <- validatePassword(password)
      _ <- validateUserInput(firstName, lastName, isDriver, driverLicense)
      existingUser <- userDAO.findByEmail(email)
      _ <- existingUser match {
        case Some(_) => Failure(new IllegalArgumentException("Email already exists"))
        case None => Success(())
      }
      hashedPassword = hashPassword(password)
      user = User.create(
        email = email,
        passwordHash = hashedPassword,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        dateOfBirth = dateOfBirth,
        isDriver = isDriver,
        driverLicense = driverLicense
      )
      createdUser <- userDAO.create(user)
    } yield createdUser
  }
  
  def login(email: String, password: String): Try[User] = {
    for {
      userOpt <- userDAO.findByEmail(email)
      user <- userOpt match {
        case Some(u) => Success(u)
        case None => Failure(new IllegalArgumentException("Invalid email or password"))
      }
      _ <- if (checkPassword(password, user.passwordHash)) Success(())
           else Failure(new IllegalArgumentException("Invalid email or password"))
    } yield user
  }
  
  def changePassword(userId: Int, currentPassword: String, newPassword: String): Try[Boolean] = {
    for {
      userOpt <- userDAO.findById(userId)
      user <- userOpt match {
        case Some(u) => Success(u)
        case None => Failure(new IllegalArgumentException("User not found"))
      }
      _ <- if (checkPassword(currentPassword, user.passwordHash)) Success(())
           else Failure(new IllegalArgumentException("Current password is incorrect"))
      _ <- validatePassword(newPassword)
      hashedNewPassword = hashPassword(newPassword)
      result <- userDAO.updatePassword(userId, hashedNewPassword)
    } yield result
  }
  
  def updateProfile(
      userId: Int,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      phone: Option[String] = None,
      dateOfBirth: Option[LocalDate] = None
  ): Try[User] = {
    for {
      userOpt <- userDAO.findById(userId)
      user <- userOpt match {
        case Some(u) => Success(u)
        case None => Failure(new IllegalArgumentException("User not found"))
      }
      updatedUser = user.copy(
        firstName = firstName.getOrElse(user.firstName),
        lastName = lastName.getOrElse(user.lastName),
        phone = phone.orElse(user.phone),
        dateOfBirth = dateOfBirth.orElse(user.dateOfBirth)
      )
      result <- userDAO.update(updatedUser)
    } yield result
  }
  
  def promoteToDriver(userId: Int, driverLicense: String): Try[User] = {
    for {
      userOpt <- userDAO.findById(userId)
      user <- userOpt match {
        case Some(u) => Success(u)
        case None => Failure(new IllegalArgumentException("User not found"))
      }
      _ <- if (driverLicense.trim.nonEmpty) Success(())
           else Failure(new IllegalArgumentException("Driver license is required"))
      updatedUser = user.copy(isDriver = true, driverLicense = Some(driverLicense))
      result <- userDAO.update(updatedUser)
    } yield result
  }
  
  def deleteUser(userId: Int, password: String): Try[Boolean] = {
    for {
      userOpt <- userDAO.findById(userId)
      user <- userOpt match {
        case Some(u) => Success(u)
        case None => Failure(new IllegalArgumentException("User not found"))
      }
      _ <- if (checkPassword(password, user.passwordHash)) Success(())
           else Failure(new IllegalArgumentException("Password is incorrect"))
      result <- userDAO.delete(userId)
    } yield result
  }
  
  def getUserProfile(userId: Int): Try[User] = {
    userDAO.findById(userId).flatMap {
      case Some(user) => Success(user)
      case None => Failure(new IllegalArgumentException("User not found"))
    }
  }
  
  private def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }
  
  private def checkPassword(password: String, hash: String): Boolean = {
    BCrypt.checkpw(password, hash)
  }
  
  private def validateEmail(email: String): Try[Unit] = {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".r
    if (email.trim.isEmpty) {
      Failure(new IllegalArgumentException("Email cannot be empty"))
    } else if (!emailRegex.matches(email)) {
      Failure(new IllegalArgumentException("Invalid email format"))
    } else {
      Success(())
    }
  }
  
  private def validatePassword(password: String): Try[Unit] = {
    if (password.length < 6) {
      Failure(new IllegalArgumentException("Password must be at least 6 characters long"))
    } else if (!password.exists(_.isDigit)) {
      Failure(new IllegalArgumentException("Password must contain at least one digit"))
    } else if (!password.exists(_.isLetter)) {
      Failure(new IllegalArgumentException("Password must contain at least one letter"))
    } else {
      Success(())
    }
  }
  
  private def validateUserInput(
      firstName: String,
      lastName: String,
      isDriver: Boolean,
      driverLicense: Option[String]
  ): Try[Unit] = {
    if (firstName.trim.isEmpty) {
      Failure(new IllegalArgumentException("First name cannot be empty"))
    } else if (lastName.trim.isEmpty) {
      Failure(new IllegalArgumentException("Last name cannot be empty"))
    } else if (isDriver && driverLicense.isEmpty) {
      Failure(new IllegalArgumentException("Driver license is required for drivers"))
    } else {
      Success(())
    }
  }
}