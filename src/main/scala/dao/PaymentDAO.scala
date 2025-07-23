package dao

import models.{Payment, PaymentStatus}
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet

class PaymentDAO {
  
  def create(payment: Payment): Try[Payment] = {
    val sql = """
      INSERT INTO payments (reservation_id, payer_id, receiver_id, amount, payment_method, status, transaction_date)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        payment.reservationId,
        payment.payerId,
        payment.receiverId,
        payment.amount,
        payment.paymentMethod,
        payment.status.toString,
        payment.transactionDate.orNull
      )
      createdPayment <- findById(id)
    } yield createdPayment.get
  }
  
  def findById(id: Int): Try[Option[Payment]] = {
    val sql = "SELECT * FROM payments WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToPayment(rs))
      else None
    }
  }
  
  def findByReservation(reservationId: Int): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE reservation_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, reservationId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def findByPayer(payerId: Int): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE payer_id = ? ORDER BY transaction_date DESC"
    
    DatabaseConnection.executeQuery(sql, payerId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def findByReceiver(receiverId: Int): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE receiver_id = ? ORDER BY transaction_date DESC"
    
    DatabaseConnection.executeQuery(sql, receiverId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def findByUser(userId: Int): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE payer_id = ? OR receiver_id = ? ORDER BY transaction_date DESC"
    
    DatabaseConnection.executeQuery(sql, userId, userId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def updateStatus(paymentId: Int, status: PaymentStatus.PaymentStatus): Try[Boolean] = {
    val sql = "UPDATE payments SET status = ? WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, status.toString, paymentId).map(_ > 0)
  }
  
  def findPendingPayments(userId: Int): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE (payer_id = ? OR receiver_id = ?) AND status = 'PENDING' ORDER BY created_at"
    
    DatabaseConnection.executeQuery(sql, userId, userId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def getPaymentHistory(userId: Int): Try[List[Payment]] = {
    val sql = """
      SELECT * FROM payments 
      WHERE (payer_id = ? OR receiver_id = ?) 
        AND status IN ('COMPLETED', 'REFUNDED')
      ORDER BY transaction_date DESC
    """
    
    DatabaseConnection.executeQuery(sql, userId, userId) { rs =>
      val payments = scala.collection.mutable.ListBuffer[Payment]()
      while (rs.next()) {
        payments += mapResultSetToPayment(rs)
      }
      payments.toList
    }
  }
  
  def getTotalEarnings(userId: Int): Try[BigDecimal] = {
    val sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE receiver_id = ? AND status = 'COMPLETED'"
    
    DatabaseConnection.executeQuery(sql, userId) { rs =>
      rs.next()
      BigDecimal(rs.getBigDecimal(1))
    }
  }
  
  def getTotalSpent(userId: Int): Try[BigDecimal] = {
    val sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payer_id = ? AND status = 'COMPLETED'"
    
    DatabaseConnection.executeQuery(sql, userId) { rs =>
      rs.next()
      BigDecimal(rs.getBigDecimal(1))
    }
  }
  
  private def mapResultSetToPayment(rs: ResultSet): Payment = {
    Payment(
      id = Some(rs.getInt("id")),
      reservationId = rs.getInt("reservation_id"),
      payerId = rs.getInt("payer_id"),
      receiverId = rs.getInt("receiver_id"),
      amount = BigDecimal(rs.getBigDecimal("amount")),
      paymentMethod = rs.getString("payment_method"),
      status = PaymentStatus.withName(rs.getString("status")),
      transactionDate = Option(rs.getTimestamp("transaction_date")).map(_.toLocalDateTime),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime)
    )
  }
}