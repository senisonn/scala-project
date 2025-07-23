package utils

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}
import scala.util.{Try, Using}

object DatabaseConnection {
  private val url = "jdbc:postgresql://postgres_db:5432/scala_db"
  private val username = "scala_user"
  private val password = "scala_pass"
  
  def getConnection: Connection = {
    Class.forName("org.postgresql.Driver")
    DriverManager.getConnection(url, username, password)
  }
  
  def withConnection[T](operation: Connection => T): Try[T] = {
    Using(getConnection)(operation)
  }
  
  def withPreparedStatement[T](sql: String)(operation: PreparedStatement => T): Try[T] = {
    withConnection { conn =>
      Using(conn.prepareStatement(sql))(operation).get
    }
  }
  
  def executeQuery[T](sql: String, params: Any*)(resultProcessor: ResultSet => T): Try[T] = {
    withConnection { conn =>
      Using(conn.prepareStatement(sql)) { stmt =>
        params.zipWithIndex.foreach { case (param, index) =>
          setParameter(stmt, index + 1, param)
        }
        Using(stmt.executeQuery())(resultProcessor).get
      }.get
    }
  }
  
  def executeUpdate(sql: String, params: Any*): Try[Int] = {
    withConnection { conn =>
      Using(conn.prepareStatement(sql)) { stmt =>
        params.zipWithIndex.foreach { case (param, index) =>
          setParameter(stmt, index + 1, param)
        }
        stmt.executeUpdate()
      }.get
    }
  }
  
  def executeInsertAndGetId(sql: String, params: Any*): Try[Int] = {
    withConnection { conn =>
      Using(conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) { stmt =>
        params.zipWithIndex.foreach { case (param, index) =>
          setParameter(stmt, index + 1, param)
        }
        stmt.executeUpdate()
        Using(stmt.getGeneratedKeys) { rs =>
          if (rs.next()) rs.getInt(1)
          else throw new RuntimeException("Failed to get generated ID")
        }.get
      }.get
    }
  }
  
  private def setParameter(stmt: PreparedStatement, index: Int, param: Any): Unit = {
    param match {
      case null => stmt.setNull(index, java.sql.Types.NULL)
      case s: String => stmt.setString(index, s)
      case i: Int => stmt.setInt(index, i)
      case l: Long => stmt.setLong(index, l)
      case d: Double => stmt.setDouble(index, d)
      case f: Float => stmt.setFloat(index, f)
      case b: Boolean => stmt.setBoolean(index, b)
      case bd: scala.math.BigDecimal => stmt.setBigDecimal(index, bd.bigDecimal)
      case jbd: java.math.BigDecimal => stmt.setBigDecimal(index, jbd)
      case date: java.time.LocalDate => stmt.setDate(index, java.sql.Date.valueOf(date))
      case time: java.time.LocalTime => stmt.setTime(index, java.sql.Time.valueOf(time))
      case datetime: java.time.LocalDateTime => stmt.setTimestamp(index, java.sql.Timestamp.valueOf(datetime))
      case opt: Option[_] => opt match {
        case Some(value) => setParameter(stmt, index, value)
        case None => stmt.setNull(index, java.sql.Types.NULL)
      }
      case _ => stmt.setString(index, param.toString)
    }
  }
  
  def testConnection(): Boolean = {
    Try {
      Using(getConnection) { conn =>
        conn.isValid(5)
      }.get
    }.getOrElse(false)
  }
}