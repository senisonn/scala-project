package dao

import models.Rating
import utils.DatabaseConnection
import scala.util.Try
import java.sql.ResultSet

class RatingDAO {
  
  def create(rating: Rating): Try[Rating] = {
    val sql = """
      INSERT INTO ratings (trip_id, rater_id, rated_id, rating, comment)
      VALUES (?, ?, ?, ?, ?)
    """
    
    for {
      id <- DatabaseConnection.executeInsertAndGetId(
        sql,
        rating.tripId,
        rating.raterId,
        rating.ratedId,
        rating.rating,
        rating.comment.orNull
      )
      createdRating <- findById(id)
    } yield createdRating.get
  }
  
  def findById(id: Int): Try[Option[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE id = ?"
    
    DatabaseConnection.executeQuery(sql, id) { rs =>
      if (rs.next()) Some(mapResultSetToRating(rs))
      else None
    }
  }
  
  def findByTrip(tripId: Int): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE trip_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, tripId) { rs =>
      val ratings = scala.collection.mutable.ListBuffer[Rating]()
      while (rs.next()) {
        ratings += mapResultSetToRating(rs)
      }
      ratings.toList
    }
  }
  
  def findByRatedUser(ratedId: Int): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE rated_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, ratedId) { rs =>
      val ratings = scala.collection.mutable.ListBuffer[Rating]()
      while (rs.next()) {
        ratings += mapResultSetToRating(rs)
      }
      ratings.toList
    }
  }
  
  def findByRaterUser(raterId: Int): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE rater_id = ? ORDER BY created_at DESC"
    
    DatabaseConnection.executeQuery(sql, raterId) { rs =>
      val ratings = scala.collection.mutable.ListBuffer[Rating]()
      while (rs.next()) {
        ratings += mapResultSetToRating(rs)
      }
      ratings.toList
    }
  }
  
  def getAverageRating(userId: Int): Try[Option[Double]] = {
    val sql = "SELECT AVG(rating::decimal) FROM ratings WHERE rated_id = ?"
    
    DatabaseConnection.executeQuery(sql, userId) { rs =>
      rs.next()
      val avg = rs.getDouble(1)
      if (rs.wasNull()) None else Some(avg)
    }
  }
  
  def getRatingCount(userId: Int): Try[Int] = {
    val sql = "SELECT COUNT(*) FROM ratings WHERE rated_id = ?"
    
    DatabaseConnection.executeQuery(sql, userId) { rs =>
      rs.next()
      rs.getInt(1)
    }
  }
  
  def hasRated(tripId: Int, raterId: Int, ratedId: Int): Try[Boolean] = {
    val sql = "SELECT COUNT(*) FROM ratings WHERE trip_id = ? AND rater_id = ? AND rated_id = ?"
    
    DatabaseConnection.executeQuery(sql, tripId, raterId, ratedId) { rs =>
      rs.next()
      rs.getInt(1) > 0
    }
  }
  
  def getRatingStats(userId: Int): Try[(Double, Int, Map[Int, Int])] = {
    val avgSql = "SELECT AVG(rating::decimal), COUNT(*) FROM ratings WHERE rated_id = ?"
    val distributionSql = "SELECT rating, COUNT(*) FROM ratings WHERE rated_id = ? GROUP BY rating ORDER BY rating"
    
    for {
      (avg, count) <- DatabaseConnection.executeQuery(avgSql, userId) { rs =>
        rs.next()
        val avgRating = if (rs.wasNull()) 0.0 else rs.getDouble(1)
        val totalCount = rs.getInt(2)
        (avgRating, totalCount)
      }
      distribution <- DatabaseConnection.executeQuery(distributionSql, userId) { rs =>
        val dist = scala.collection.mutable.Map[Int, Int]()
        while (rs.next()) {
          dist(rs.getInt(1)) = rs.getInt(2)
        }
        dist.toMap
      }
    } yield (avg, count, distribution)
  }
  
  def getRecentRatings(userId: Int, limit: Int = 10): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE rated_id = ? ORDER BY created_at DESC LIMIT ?"
    
    DatabaseConnection.executeQuery(sql, userId, limit) { rs =>
      val ratings = scala.collection.mutable.ListBuffer[Rating]()
      while (rs.next()) {
        ratings += mapResultSetToRating(rs)
      }
      ratings.toList
    }
  }
  
  def findMutualRatings(tripId: Int, user1Id: Int, user2Id: Int): Try[List[Rating]] = {
    val sql = """
      SELECT * FROM ratings 
      WHERE trip_id = ? 
        AND ((rater_id = ? AND rated_id = ?) OR (rater_id = ? AND rated_id = ?))
      ORDER BY created_at
    """
    
    DatabaseConnection.executeQuery(sql, tripId, user1Id, user2Id, user2Id, user1Id) { rs =>
      val ratings = scala.collection.mutable.ListBuffer[Rating]()
      while (rs.next()) {
        ratings += mapResultSetToRating(rs)
      }
      ratings.toList
    }
  }
  
  private def mapResultSetToRating(rs: ResultSet): Rating = {
    Rating(
      id = Some(rs.getInt("id")),
      tripId = rs.getInt("trip_id"),
      raterId = rs.getInt("rater_id"),
      ratedId = rs.getInt("rated_id"),
      rating = rs.getInt("rating"),
      comment = Option(rs.getString("comment")),
      createdAt = Option(rs.getTimestamp("created_at")).map(_.toLocalDateTime)
    )
  }
}