package models

import java.time.{LocalDate, LocalDateTime}

case class User(
    id: Option[Int] = None,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
    phone: Option[String] = None,
    dateOfBirth: Option[LocalDate] = None,
    isDriver: Boolean = false,
    driverLicense: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    updatedAt: Option[LocalDateTime] = None
) {
  require(email.nonEmpty, "Email cannot be empty")
  require(firstName.nonEmpty, "First name cannot be empty")
  require(lastName.nonEmpty, "Last name cannot be empty")
  require(passwordHash.nonEmpty, "Password hash cannot be empty")
  require(!isDriver || driverLicense.isDefined, "Driver must have a license")

  def fullName: String = s"$firstName $lastName"
  
  def isValidDriver: Boolean = isDriver && driverLicense.isDefined
}

object User {
  def create(
      email: String,
      passwordHash: String,
      firstName: String,
      lastName: String,
      phone: Option[String] = None,
      dateOfBirth: Option[LocalDate] = None,
      isDriver: Boolean = false,
      driverLicense: Option[String] = None
  ): User = {
    User(
      email = email,
      passwordHash = passwordHash,
      firstName = firstName,
      lastName = lastName,
      phone = phone,
      dateOfBirth = dateOfBirth,
      isDriver = isDriver,
      driverLicense = driverLicense,
      createdAt = Some(LocalDateTime.now()),
      updatedAt = Some(LocalDateTime.now())
    )
  }
}