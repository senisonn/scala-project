package models

import java.time.LocalDateTime

case class Rating(
    id: Option[Int] = None,
    tripId: Int,
    raterId: Int,
    ratedId: Int,
    rating: Int,
    comment: Option[String] = None,
    createdAt: Option[LocalDateTime] = None
) {
  require(tripId > 0, "Trip ID must be positive")
  require(raterId > 0, "Rater ID must be positive")
  require(ratedId > 0, "Rated ID must be positive")
  require(raterId != ratedId, "Rater and rated user must be different")
  require(rating >= 1 && rating <= 5, "Rating must be between 1 and 5")

  def isPositive: Boolean = rating >= 4
  
  def isNegative: Boolean = rating <= 2
}

object Rating {
  def create(
      tripId: Int,
      raterId: Int,
      ratedId: Int,
      rating: Int,
      comment: Option[String] = None
  ): Rating = {
    Rating(
      tripId = tripId,
      raterId = raterId,
      ratedId = ratedId,
      rating = rating,
      comment = comment,
      createdAt = Some(LocalDateTime.now())
    )
  }
}