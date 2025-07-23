package models

import java.time.{LocalDate, LocalDateTime, LocalTime}

object TripStatus extends Enumeration {
  type TripStatus = Value
  val ACTIVE, CANCELLED, COMPLETED = Value
}

case class Trip(
    id: Option[Int] = None,
    driverId: Int,
    vehicleId: Int,
    departureCity: String,
    arrivalCity: String,
    departureDate: LocalDate,
    departureTime: LocalTime,
    availableSeats: Int,
    pricePerSeat: BigDecimal,
    description: Option[String] = None,
    status: TripStatus.TripStatus = TripStatus.ACTIVE,
    createdAt: Option[LocalDateTime] = None,
    updatedAt: Option[LocalDateTime] = None
) {
  require(driverId > 0, "Driver ID must be positive")
  require(vehicleId > 0, "Vehicle ID must be positive")
  require(departureCity.nonEmpty, "Departure city cannot be empty")
  require(arrivalCity.nonEmpty, "Arrival city cannot be empty")
  require(departureCity != arrivalCity, "Departure and arrival cities must be different")
  require(availableSeats >= 0, "Available seats cannot be negative")
  require(pricePerSeat >= 0, "Price per seat cannot be negative")
  require(!departureDate.isBefore(LocalDate.now()), "Departure date cannot be in the past")

  def isActive: Boolean = status == TripStatus.ACTIVE
  
  def isBookable: Boolean = isActive && availableSeats > 0 && !departureDate.isBefore(LocalDate.now())
  
  def totalRevenue(reservedSeats: Int): BigDecimal = pricePerSeat * reservedSeats
  
  def routeDescription: String = s"$departureCity → $arrivalCity"
  
  def departureDateTime: LocalDateTime = LocalDateTime.of(departureDate, departureTime)
}

object Trip {
  def create(
      driverId: Int,
      vehicleId: Int,
      departureCity: String,
      arrivalCity: String,
      departureDate: LocalDate,
      departureTime: LocalTime,
      availableSeats: Int,
      pricePerSeat: BigDecimal,
      description: Option[String] = None
  ): Trip = {
    Trip(
      driverId = driverId,
      vehicleId = vehicleId,
      departureCity = departureCity,
      arrivalCity = arrivalCity,
      departureDate = departureDate,
      departureTime = departureTime,
      availableSeats = availableSeats,
      pricePerSeat = pricePerSeat,
      description = description,
      createdAt = Some(LocalDateTime.now()),
      updatedAt = Some(LocalDateTime.now())
    )
  }
}