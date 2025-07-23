package models

import java.time.LocalDateTime

case class Message(
    id: Option[Int] = None,
    senderId: Int,
    receiverId: Int,
    tripId: Option[Int] = None,
    subject: Option[String] = None,
    content: String,
    isRead: Boolean = false,
    sentAt: Option[LocalDateTime] = None
) {
  require(senderId > 0, "Sender ID must be positive")
  require(receiverId > 0, "Receiver ID must be positive")
  require(senderId != receiverId, "Sender and receiver must be different")
  require(content.nonEmpty, "Message content cannot be empty")
  require(content.length <= 1000, "Message content cannot exceed 1000 characters")

  def isUnread: Boolean = !isRead
  
  def hasSubject: Boolean = subject.isDefined && subject.get.nonEmpty
  
  def isTripRelated: Boolean = tripId.isDefined
}

object Message {
  def create(
      senderId: Int,
      receiverId: Int,
      content: String,
      subject: Option[String] = None,
      tripId: Option[Int] = None
  ): Message = {
    Message(
      senderId = senderId,
      receiverId = receiverId,
      tripId = tripId,
      subject = subject,
      content = content,
      sentAt = Some(LocalDateTime.now())
    )
  }
}