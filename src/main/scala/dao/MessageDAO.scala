package dao

import models.Message
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet

class MessageDAO {
  
  def create(message: Message): Try[Message] = {
    val sql = """
      INSERT INTO messages (sender_id, receiver_id, trip_id, subject, content, is_read, sent_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        message.senderId,
        message.receiverId,
        message.tripId.orNull,
        message.subject.orNull,
        message.content,
        message.isRead,
        message.sentAt.orNull
      )
      createdMessage <- findById(id)
    } yield createdMessage.get
  }
  
  def findById(id: Int): Try[Option[Message]] = {
    val sql = "SELECT * FROM messages WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToMessage(rs))
      else None
    }
  }
  
  def findByReceiver(receiverId: Int): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE receiver_id = ? ORDER BY sent_at DESC"
    
    DatabaseConnection.executeQuery(sql, receiverId) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def findBySender(senderId: Int): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE sender_id = ? ORDER BY sent_at DESC"
    
    DatabaseConnection.executeQuery(sql, senderId) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def findConversation(user1Id: Int, user2Id: Int): Try[List[Message]] = {
    val sql = """
      SELECT * FROM messages 
      WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
      ORDER BY sent_at ASC
    """
    
    DatabaseConnection.executeQuery(sql, user1Id, user2Id, user2Id, user1Id) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def findUnreadMessages(receiverId: Int): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE receiver_id = ? AND is_read = false ORDER BY sent_at DESC"
    
    DatabaseConnection.executeQuery(sql, receiverId) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def markAsRead(messageId: Int): Try[Boolean] = {
    val sql = "UPDATE messages SET is_read = true WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, messageId).map(_ > 0)
  }
  
  def markAllAsRead(receiverId: Int): Try[Boolean] = {
    val sql = "UPDATE messages SET is_read = true WHERE receiver_id = ? AND is_read = false"
    
    DatabaseConnection.executeUpdate(sql, receiverId).map(_ > 0)
  }
  
  def getUnreadCount(receiverId: Int): Try[Int] = {
    val sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = false"
    
    DatabaseConnection.executeQuery(sql, receiverId) { rs =>
      rs.next()
      rs.getInt(1)
    }
  }
  
  def findTripMessages(tripId: Int): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE trip_id = ? ORDER BY sent_at ASC"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def findRecentMessages(userId: Int, limit: Int = 20): Try[List[Message]] = {
    val sql = """
      SELECT * FROM messages 
      WHERE sender_id = ? OR receiver_id = ? 
      ORDER BY sent_at DESC 
      LIMIT ?
    """
    
    DatabaseConnection.executeQuery(sql, userId, userId, limit) { rs =>
      val messages = scala.collection.mutable.ListBuffer[Message]()
      while (rs.next()) {
        messages += mapResultSetToMessage(rs)
      }
      messages.toList
    }
  }
  
  def delete(messageId: Int): Try[Boolean] = {
    val sql = "DELETE FROM messages WHERE id = ?"
    
    DatabaseConnection.executeUpdate(sql, messageId).map(_ > 0)
  }
  
  def getConversationPartners(userId: Int): Try[List[Int]] = {
    val sql = """
      SELECT DISTINCT 
        CASE 
          WHEN sender_id = ? THEN receiver_id 
          ELSE sender_id 
        END as partner_id
      FROM messages 
      WHERE sender_id = ? OR receiver_id = ?
      ORDER BY partner_id
    """
    
    DatabaseConnection.executeQuery(sql, userId, userId, userId) { rs =>
      val partners = scala.collection.mutable.ListBuffer[Int]()
      while (rs.next()) {
        partners += rs.getInt("partner_id")
      }
      partners.toList
    }
  }
  
  private def mapResultSetToMessage(rs: ResultSet): Message = {
    Message(
      id = Some(rs.getInt("id")),
      senderId = rs.getInt("sender_id"),
      receiverId = rs.getInt("receiver_id"),
      tripId = Option(rs.getObject("trip_id")).map(_.toString.toInt),
      subject = Option(rs.getString("subject")),
      content = rs.getString("content"),
      isRead = rs.getBoolean("is_read"),
      sentAt = Option(rs.getTimestamp("sent_at")).map(_.toLocalDateTime)
    )
  }
}