package models

import java.time.LocalDateTime

object PaymentStatus extends Enumeration {
  type PaymentStatus = Value
  val PENDING, COMPLETED, FAILED, REFUNDED = Value
}

case class Payment(
    id: Option[Int] = None,
    reservationId: Int,
    payerId: Int,
    receiverId: Int,
    amount: BigDecimal,
    paymentMethod: String = "CASH",
    status: PaymentStatus.PaymentStatus = PaymentStatus.PENDING,
    transactionDate: Option[LocalDateTime] = None,
    createdAt: Option[LocalDateTime] = None
) {
  require(reservationId > 0, "Reservation ID must be positive")
  require(payerId > 0, "Payer ID must be positive")
  require(receiverId > 0, "Receiver ID must be positive")
  require(payerId != receiverId, "Payer and receiver must be different")
  require(amount > 0, "Amount must be positive")
  require(paymentMethod.nonEmpty, "Payment method cannot be empty")

  def isCompleted: Boolean = status == PaymentStatus.COMPLETED
  
  def isPending: Boolean = status == PaymentStatus.PENDING
  
  def isFailed: Boolean = status == PaymentStatus.FAILED
}

object Payment {
  def create(
      reservationId: Int,
      payerId: Int,
      receiverId: Int,
      amount: BigDecimal,
      paymentMethod: String = "CASH"
  ): Payment = {
    Payment(
      reservationId = reservationId,
      payerId = payerId,
      receiverId = receiverId,
      amount = amount,
      paymentMethod = paymentMethod,
      transactionDate = Some(LocalDateTime.now()),
      createdAt = Some(LocalDateTime.now())
    )
  }
}