package models

import java.time.LocalDateTime

case class Vehicle(
    id: Option[Int] = None,
    ownerId: Int,
    make: String,
    model: String,
    year: Int,
    color: Option[String] = None,
    licensePlate: String,
    seats: Int,
    createdAt: Option[LocalDateTime] = None,
    updatedAt: Option[LocalDateTime] = None
) {
  require(make.nonEmpty, "Make cannot be empty")
  require(model.nonEmpty, "Model cannot be empty")
  require(year > 1900 && year <= LocalDateTime.now().getYear + 1, "Year must be valid")
  require(licensePlate.nonEmpty, "License plate cannot be empty")
  require(seats > 0 && seats <= 10, "Seats must be between 1 and 10")
  require(ownerId > 0, "Owner ID must be positive")

  def description: String = s"$year $make $model ($licensePlate)"
  
  def availableSeatsForTrip: Int = seats - 1 // Driver takes one seat
}

object Vehicle {
  def create(
      ownerId: Int,
      make: String,
      model: String,
      year: Int,
      color: Option[String],
      licensePlate: String,
      seats: Int
  ): Vehicle = {
    Vehicle(
      ownerId = ownerId,
      make = make,
      model = model,
      year = year,
      color = color,
      licensePlate = licensePlate,
      seats = seats,
      createdAt = Some(LocalDateTime.now()),
      updatedAt = Some(LocalDateTime.now())
    )
  }
}