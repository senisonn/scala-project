package models

import java.time.LocalDateTime

object ReservationStatus extends Enumeration {
  type ReservationStatus = Value
  val ACTIVE, CANCELLED, COMPLETED = Value
}

case class Reservation(
    id: Option[Int] = None,
    tripId: Int,
    passengerId: Int,
    seatsReserved: Int,
    totalPrice: BigDecimal,
    status: ReservationStatus.ReservationStatus = ReservationStatus.ACTIVE,
    createdAt: Option[LocalDateTime] = None,
    updatedAt: Option[LocalDateTime] = None
) {
  require(tripId > 0, "Trip ID must be positive")
  require(passengerId > 0, "Passenger ID must be positive")
  require(seatsReserved > 0, "Seats reserved must be positive")
  require(totalPrice >= 0, "Total price cannot be negative")

  def isActive: Boolean = status == ReservationStatus.ACTIVE
  
  def isCompleted: Boolean = status == ReservationStatus.COMPLETED
  
  def isCancelled: Boolean = status == ReservationStatus.CANCELLED
  
  def pricePerSeat: BigDecimal = totalPrice / seatsReserved
}

object Reservation {
  def create(
      tripId: Int,
      passengerId: Int,
      seatsReserved: Int,
      totalPrice: BigDecimal
  ): Reservation = {
    Reservation(
      tripId = tripId,
      passengerId = passengerId,
      seatsReserved = seatsReserved,
      totalPrice = totalPrice,
      createdAt = Some(LocalDateTime.now()),
      updatedAt = Some(LocalDateTime.now())
    )
  }
}