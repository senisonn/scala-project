import dao.*
import models.{PaymentStatus, ReservationStatus, TripStatus, User}
import services.AuthService
import utils.DatabaseConnection

import scala.io.StdIn
import scala.util.{Failure, Success, Try}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RidesharingApp {
  private val userDAO = new UserDAO()
  private val tripDAO = new TripDAO()
  private val reservationDAO = new ReservationDAO()
  private val vehicleDAO = new VehicleDAO()
  private val paymentDAO = new PaymentDAO()
  private val ratingDAO = new RatingDAO()
  private val messageDAO = new MessageDAO()
  
  private val authService = new AuthService(userDAO)
  
  private var currentUser: Option[User] = None
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  
  def start(): Unit = {
    println("🚗 Welcome to Ridesharing Platform!")
    
    if (!DatabaseConnection.testConnection()) {
      println("❌ Unable to connect to database. Please check your PostgreSQL configuration.")
      return
    }
    
    println("✅ Database connection successful!")
    
    var running = true
    while (running) {
      try {
        currentUser match {
          case None => running = showMainMenu()
          case Some(user) => running = showUserMenu(user)
        }
      } catch {
        case e: Exception =>
          println(s"❌ An error occurred: ${e.getMessage}")
          println("Please try again.")
      }
    }
    
    println("👋 Thank you for using Ridesharing Platform!")
  }
  
  private def showMainMenu(): Boolean = {
    println("\n=== MAIN MENU ===")
    println("1. Register")
    println("2. Login")
    println("3. Exit")
    print("Choose an option (1-3): ")
    
    StdIn.readLine().trim match {
      case "1" => handleRegistration()
      case "2" => handleLogin()
      case "3" => false
      case _ => 
        println("❌ Invalid option. Please choose 1, 2, or 3.")
        true
    }
  }
  
  private def showUserMenu(user: User): Boolean = {
    println(s"\n=== WELCOME ${user.firstName.toUpperCase()} ===")
    println("1. My Profile")
    println("2. Search Trips")
    println("3. My Reservations")
    if (user.isDriver) {
      println("4. My Trips (Driver)")
      println("5. Create Trip")
      println("6. My Vehicles")
    } else {
      println("4. Become a Driver")
    }
    println("7. Messages")
    println("8. Payment History")
    println("9. Ratings")
    println("10. Logout")
    print("Choose an option: ")
    
    StdIn.readLine().trim match {
      case "1" => showProfile(user); true
      case "2" => searchTrips(); true
      case "3" => showMyReservations(user); true
      case "4" => 
        if (user.isDriver) showMyTrips(user)
        else becomeDriver(user)
        true
      case "5" if user.isDriver => createTrip(user); true
      case "6" if user.isDriver => showMyVehicles(user); true
      case "4" if !user.isDriver => becomeDriver(user); true
      case "7" => showMessages(user); true
      case "8" => showPaymentHistory(user); true
      case "9" => showRatings(user); true
      case "10" => 
        currentUser = None
        println("👋 Logged out successfully!")
        true
      case _ => 
        println("❌ Invalid option. Please try again.")
        true
    }
  }
  
  private def handleRegistration(): Boolean = {
    println("\n=== USER REGISTRATION ===")
    
    try {
      print("Email: ")
      val email = StdIn.readLine().trim
      
      print("Password: ")
      val password = StdIn.readLine().trim
      
      print("First Name: ")
      val firstName = StdIn.readLine().trim
      
      print("Last Name: ")
      val lastName = StdIn.readLine().trim
      
      print("Phone (optional): ")
      val phoneInput = StdIn.readLine().trim
      val phone = if (phoneInput.isEmpty) None else Some(phoneInput)
      
      print("Are you a driver? (y/n): ")
      val isDriver = StdIn.readLine().trim.toLowerCase == "y"
      
      val driverLicense = if (isDriver) {
        print("Driver License: ")
        Some(StdIn.readLine().trim)
      } else None
      
      authService.registerUser(email, password, firstName, lastName, phone, None, isDriver, driverLicense) match {
        case Success(user) =>
          println("✅ Registration successful!")
          currentUser = Some(user)
        case Failure(e) =>
          println(s"❌ Registration failed: ${e.getMessage}")
      }
    } catch {
      case e: Exception =>
        println(s"❌ Registration error: ${e.getMessage}")
    }
    
    true
  }
  
  private def handleLogin(): Boolean = {
    println("\n=== USER LOGIN ===")
    
    try {
      print("Email: ")
      val email = StdIn.readLine().trim
      
      print("Password: ")
      val password = StdIn.readLine().trim
      
      authService.login(email, password) match {
        case Success(user) =>
          println("✅ Login successful!")
          currentUser = Some(user)
        case Failure(e) =>
          println(s"❌ Login failed: ${e.getMessage}")
      }
    } catch {
      case e: Exception =>
        println(s"❌ Login error: ${e.getMessage}")
    }
    
    true
  }
  
  private def showProfile(user: User): Unit = {
    println(s"\n=== PROFILE: ${user.fullName} ===")
    println(s"Email: ${user.email}")
    println(s"Phone: ${user.phone.getOrElse("Not provided")}")
    println(s"Account Type: ${if (user.isDriver) "Driver" else "Passenger"}")
    if (user.isDriver) {
      println(s"Driver License: ${user.driverLicense.getOrElse("Not provided")}")
    }
    
    ratingDAO.getAverageRating(user.id.get) match {
      case Success(Some(avgRating)) =>
        ratingDAO.getRatingCount(user.id.get) match {
          case Success(count) =>
            println(f"Rating: $avgRating%.1f/5.0 ($count reviews)")
          case _ => println("Rating: No ratings yet")
        }
      case _ => println("Rating: No ratings yet")
    }
  }
  
  private def searchTrips(): Unit = {
    println("\n=== SEARCH TRIPS ===")
    
    try {
      print("Departure City: ")
      val departureCity = StdIn.readLine().trim
      
      print("Arrival City: ")
      val arrivalCity = StdIn.readLine().trim
      
      print("Departure Date (yyyy-MM-dd): ")
      val dateStr = StdIn.readLine().trim
      val departureDate = LocalDate.parse(dateStr, dateFormatter)
      
      tripDAO.searchTrips(departureCity, arrivalCity, departureDate) match {
        case Success(trips) =>
          if (trips.isEmpty) {
            println("❌ No trips found for your search criteria.")
          } else {
            println(s"\n✅ Found ${trips.length} trip(s):")
            trips.zipWithIndex.foreach { case (trip, index) =>
              println(s"\n${index + 1}. ${trip.routeDescription}")
              println(s"   Date: ${trip.departureDate} at ${trip.departureTime}")
              println(s"   Available Seats: ${trip.availableSeats}")
              println(s"   Price per Seat: €${trip.pricePerSeat}")
              trip.description.foreach(desc => println(s"   Description: $desc"))
            }
            
            if (currentUser.isDefined) {
              print("\nBook a trip? Enter trip number (or 0 to cancel): ")
              val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
              if (choice > 0 && choice <= trips.length) {
                bookTrip(trips(choice - 1))
              }
            }
          }
        case Failure(e) =>
          println(s"❌ Search failed: ${e.getMessage}")
      }
    } catch {
      case e: Exception =>
        println(s"❌ Search error: ${e.getMessage}")
    }
  }
  
  private def bookTrip(trip: models.Trip): Unit = {
    currentUser match {
      case Some(user) =>
        println(s"\n=== BOOK TRIP: ${trip.routeDescription} ===")
        
        if (trip.driverId == user.id.get) {
          println("❌ You cannot book your own trip.")
          return
        }
        
        reservationDAO.hasActiveReservation(trip.id.get, user.id.get) match {
          case Success(true) =>
            println("❌ You already have an active reservation for this trip.")
            return
          case Success(false) =>
            print(s"How many seats? (max ${trip.availableSeats}): ")
            val seatsInput = Try(StdIn.readLine().trim.toInt).getOrElse(0)
            
            if (seatsInput <= 0 || seatsInput > trip.availableSeats) {
              println("❌ Invalid number of seats.")
              return
            }
            
            val totalPrice = trip.pricePerSeat * seatsInput
            println(f"Total price: €$totalPrice")
            print("Confirm booking? (y/n): ")
            
            if (StdIn.readLine().trim.toLowerCase == "y") {
              val reservation = models.Reservation.create(
                tripId = trip.id.get,
                passengerId = user.id.get,
                seatsReserved = seatsInput,
                totalPrice = totalPrice
              )
              
              reservationDAO.create(reservation) match {
                case Success(_) =>
                  tripDAO.updateAvailableSeats(trip.id.get, trip.availableSeats - seatsInput) match {
                    case Success(_) =>
                      println("✅ Trip booked successfully!")
                    case Failure(e) =>
                      println(s"❌ Booking error: ${e.getMessage}")
                  }
                case Failure(e) =>
                  println(s"❌ Booking failed: ${e.getMessage}")
              }
            }
          case Failure(e) =>
            println(s"❌ Error checking reservation: ${e.getMessage}")
        }
      case None =>
        println("❌ Please login to book trips.")
    }
  }
  
  private def showMyReservations(user: User): Unit = {
    println("\n=== MY RESERVATIONS ===")
    
    reservationDAO.findByPassenger(user.id.get) match {
      case Success(reservations) =>
        if (reservations.isEmpty) {
          println("❌ No reservations found.")
        } else {
          reservations.zipWithIndex.foreach { case (reservation, index) =>
            tripDAO.findById(reservation.tripId) match {
              case Success(Some(trip)) =>
                println(s"\n${index + 1}. 📅 ${trip.routeDescription}")
                println(s"   Date: ${trip.departureDate} at ${trip.departureTime}")
                println(s"   Seats: ${reservation.seatsReserved}")
                println(s"   Total: €${reservation.totalPrice}")
                println(s"   Status: ${reservation.status}")
              case _ =>
                println(s"\n${index + 1}. 📅 Reservation #${reservation.id.get} (Trip details unavailable)")
            }
          }
          
          print("\nCancel a reservation? Enter number (or 0 to go back): ")
          val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
          if (choice > 0 && choice <= reservations.length) {
            cancelReservation(reservations(choice - 1))
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading reservations: ${e.getMessage}")
    }
  }
  
  private def cancelReservation(reservation: models.Reservation): Unit = {
    if (!reservation.isActive) {
      println("❌ Only active reservations can be cancelled.")
      return
    }
    
    tripDAO.findById(reservation.tripId) match {
      case Success(Some(trip)) =>
        println(s"\n=== CANCEL RESERVATION ===")
        println(s"Trip: ${trip.routeDescription}")
        println(s"Date: ${trip.departureDate} at ${trip.departureTime}")
        println(s"Seats to cancel: ${reservation.seatsReserved}")
        println(s"Refund amount: €${reservation.totalPrice}")
        print("Confirm cancellation? (y/n): ")
        
        if (StdIn.readLine().trim.toLowerCase == "y") {
          val updatedReservation = reservation.copy(status = models.ReservationStatus.CANCELLED)
          reservationDAO.update(updatedReservation) match {
            case Success(_) =>
              tripDAO.updateAvailableSeats(trip.id.get, trip.availableSeats + reservation.seatsReserved) match {
                case Success(_) =>
                  println("✅ Reservation cancelled successfully! Seats returned to trip.")
                case Failure(e) =>
                  println(s"⚠️ Reservation cancelled but failed to update available seats: ${e.getMessage}")
              }
            case Failure(e) =>
              println(s"❌ Cancellation failed: ${e.getMessage}")
          }
        }
      case _ =>
        println("❌ Trip details not found.")
    }
  }
  
  private def showMyTrips(user: User): Unit = {
    println("\n=== MY TRIPS (DRIVER) ===")
    
    tripDAO.findByDriver(user.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("❌ No trips found.")
        } else {
          trips.foreach { trip =>
            println(s"\n🚗 ${trip.routeDescription}")
            println(s"   Date: ${trip.departureDate} at ${trip.departureTime}")
            println(s"   Available Seats: ${trip.availableSeats}")
            println(s"   Price per Seat: €${trip.pricePerSeat}")
            println(s"   Status: ${trip.status}")
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading trips: ${e.getMessage}")
    }
  }
  
  private def createTrip(user: User): Unit = {
    println("\n=== CREATE TRIP ===")
    
    vehicleDAO.findByOwner(user.id.get) match {
      case Success(vehicles) =>
        if (vehicles.isEmpty) {
          println("❌ You need to add a vehicle first.")
          return
        }
        
        println("Your vehicles:")
        vehicles.zipWithIndex.foreach { case (vehicle, index) =>
          println(s"${index + 1}. ${vehicle.description}")
        }
        
        print("Select vehicle (number): ")
        val vehicleChoice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
        
        if (vehicleChoice < 1 || vehicleChoice > vehicles.length) {
          println("❌ Invalid vehicle selection.")
          return
        }
        
        val selectedVehicle = vehicles(vehicleChoice - 1)
        
        try {
          print("Departure City: ")
          val departureCity = StdIn.readLine().trim
          
          print("Arrival City: ")
          val arrivalCity = StdIn.readLine().trim
          
          print("Departure Date (yyyy-MM-dd): ")
          val dateStr = StdIn.readLine().trim
          val departureDate = LocalDate.parse(dateStr, dateFormatter)
          
          print("Departure Time (HH:mm): ")
          val timeStr = StdIn.readLine().trim
          val departureTime = java.time.LocalTime.parse(timeStr)
          
          print(s"Available Seats (max ${selectedVehicle.availableSeatsForTrip}): ")
          val availableSeats = StdIn.readLine().trim.toInt
          
          print("Price per Seat (€): ")
          val pricePerSeat = BigDecimal(StdIn.readLine().trim.toFloat)
          
          print("Description (optional): ")
          val descriptionInput = StdIn.readLine().trim
          val description = if (descriptionInput.isEmpty) None else Some(descriptionInput)
          
          val trip = models.Trip.create(
            driverId = user.id.get,
            vehicleId = selectedVehicle.id.get,
            departureCity = departureCity,
            arrivalCity = arrivalCity,
            departureDate = departureDate,
            departureTime = departureTime,
            availableSeats = availableSeats,
            pricePerSeat = pricePerSeat,
            description = description
          )
          
          tripDAO.create(trip) match {
            case Success(_) =>
              println("✅ Trip created successfully!")
            case Failure(e) =>
              println(s"❌ Trip creation failed: ${e.getMessage}")
          }
        } catch {
          case e: Exception =>
            println(s"❌ Trip creation error: ${e.getMessage}")
        }
      case Failure(e) =>
        println(s"❌ Error loading vehicles: ${e.getMessage}")
    }
  }
  
  private def showMyVehicles(user: User): Unit = {
    println("\n=== MY VEHICLES ===")
    
    vehicleDAO.findByOwner(user.id.get) match {
      case Success(vehicles) =>
        if (vehicles.isEmpty) {
          println("❌ No vehicles found.")
          print("Add a vehicle? (y/n): ")
          if (StdIn.readLine().trim.toLowerCase == "y") {
            addVehicle(user)
          }
        } else {
          vehicles.foreach { vehicle =>
            println(s"\n🚙 ${vehicle.description}")
            println(s"   Color: ${vehicle.color.getOrElse("Not specified")}")
            println(s"   Seats: ${vehicle.seats}")
          }
          
          print("\nAdd another vehicle? (y/n): ")
          if (StdIn.readLine().trim.toLowerCase == "y") {
            addVehicle(user)
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading vehicles: ${e.getMessage}")
    }
  }
  
  private def addVehicle(user: User): Unit = {
    println("\n=== ADD VEHICLE ===")
    
    try {
      print("Make: ")
      val make = StdIn.readLine().trim
      
      print("Model: ")
      val model = StdIn.readLine().trim
      
      print("Year: ")
      val year = StdIn.readLine().trim.toInt
      
      print("Color (optional): ")
      val colorInput = StdIn.readLine().trim
      val color = if (colorInput.isEmpty) None else Some(colorInput)
      
      print("License Plate: ")
      val licensePlate = StdIn.readLine().trim
      
      print("Number of Seats: ")
      val seats = StdIn.readLine().trim.toInt
      
      val vehicle = models.Vehicle.create(
        ownerId = user.id.get,
        make = make,
        model = model,
        year = year,
        color = color,
        licensePlate = licensePlate,
        seats = seats
      )
      
      vehicleDAO.create(vehicle) match {
        case Success(_) =>
          println("✅ Vehicle added successfully!")
        case Failure(e) =>
          println(s"❌ Vehicle addition failed: ${e.getMessage}")
      }
    } catch {
      case e: Exception =>
        println(s"❌ Vehicle addition error: ${e.getMessage}")
    }
  }
  
  private def becomeDriver(user: User): Unit = {
    println("\n=== BECOME A DRIVER ===")
    
    print("Driver License Number: ")
    val driverLicense = StdIn.readLine().trim
    
    if (driverLicense.isEmpty) {
      println("❌ Driver license is required.")
      return
    }
    
    authService.promoteToDriver(user.id.get, driverLicense) match {
      case Success(updatedUser) =>
        currentUser = Some(updatedUser)
        println("✅ You are now a driver! You can create trips and add vehicles.")
      case Failure(e) =>
        println(s"❌ Driver promotion failed: ${e.getMessage}")
    }
  }
  
  private def showMessages(user: User): Unit = {
    println("\n=== MESSAGES ===")
    
    messageDAO.getUnreadCount(user.id.get) match {
      case Success(count) =>
        if (count > 0) {
          println(s"📬 You have $count unread message(s)")
        } else {
          println("📭 No unread messages")
        }
      case Failure(e) =>
        println(s"❌ Error loading messages: ${e.getMessage}")
    }
    
    println("\n1. View Received Messages")
    println("2. Send Message")
    println("3. View Conversations")
    println("4. Back to Main Menu")
    print("Choose an option (1-4): ")
    
    StdIn.readLine().trim match {
      case "1" => viewReceivedMessages(user)
      case "2" => sendMessage(user)
      case "3" => viewConversations(user)
      case "4" => // Back to main menu
      case _ => println("❌ Invalid option.")
    }
  }
  
  private def viewReceivedMessages(user: User): Unit = {
    println("\n=== RECEIVED MESSAGES ===")
    
    messageDAO.findByReceiver(user.id.get) match {
      case Success(messages) =>
        if (messages.isEmpty) {
          println("❌ No messages found.")
        } else {
          messages.take(10).foreach { message =>
            userDAO.findById(message.senderId) match {
              case Success(Some(sender)) =>
                val readStatus = if (message.isRead) "✓" else "●"
                println(s"\n$readStatus From: ${sender.firstName} ${sender.lastName}")
                message.subject.foreach(subject => println(s"   Subject: $subject"))
                println(s"   Content: ${message.content}")
                println(s"   Sent: ${message.sentAt}")
              case _ =>
                val readStatus = if (message.isRead) "✓" else "●"
                println(s"\n$readStatus From: Unknown User")
                println(s"   Content: ${message.content}")
            }
          }
          
          messageDAO.markAllAsRead(user.id.get) match {
            case Success(_) =>
              println("\n✅ All messages marked as read.")
            case _ =>
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading messages: ${e.getMessage}")
    }
  }
  
  private def sendMessage(user: User): Unit = {
    println("\n=== SEND MESSAGE ===")
    
    tripDAO.findByDriver(user.id.get) match {
      case Success(driverTrips) =>
        reservationDAO.findByPassenger(user.id.get) match {
          case Success(passengerReservations) =>
            val allUsers = scala.collection.mutable.Set[Int]()
            
            driverTrips.foreach { trip =>
              reservationDAO.findByTrip(trip.id.get) match {
                case Success(reservations) =>
                  reservations.foreach(r => allUsers.add(r.passengerId))
                case _ =>
              }
            }
            
            passengerReservations.foreach { reservation =>
              tripDAO.findById(reservation.tripId) match {
                case Success(Some(trip)) =>
                  allUsers.add(trip.driverId)
                case _ =>
              }
            }
            
            allUsers.remove(user.id.get)
            
            if (allUsers.isEmpty) {
              println("❌ No contacts available. You need to have trips or reservations to message other users.")
              return
            }
            
            println("Available contacts:")
            val userList = allUsers.toList.zipWithIndex
            userList.foreach { case (userId, index) =>
              userDAO.findById(userId) match {
                case Success(Some(contact)) =>
                  println(s"${index + 1}. ${contact.firstName} ${contact.lastName}")
                case _ =>
              }
            }
            
            print("Select contact (number): ")
            val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
            
            if (choice > 0 && choice <= userList.length) {
              val receiverId = userList(choice - 1)._1
              
              userDAO.findById(receiverId) match {
                case Success(Some(receiver)) =>
                  println(s"\n=== MESSAGE TO: ${receiver.firstName} ${receiver.lastName} ===")
                  
                  print("Subject (optional): ")
                  val subjectInput = StdIn.readLine().trim
                  val subject = if (subjectInput.isEmpty) None else Some(subjectInput)
                  
                  print("Message: ")
                  val content = StdIn.readLine().trim
                  
                  if (content.isEmpty) {
                    println("❌ Message content cannot be empty.")
                    return
                  }
                  
                  val message = models.Message.create(
                    senderId = user.id.get,
                    receiverId = receiverId,
                    tripId = None,
                    subject = subject,
                    content = content
                  )
                  
                  messageDAO.create(message) match {
                    case Success(_) =>
                      println("✅ Message sent successfully!")
                    case Failure(e) =>
                      println(s"❌ Failed to send message: ${e.getMessage}")
                  }
                case _ =>
                  println("❌ Contact not found.")
              }
            } else {
              println("❌ Invalid contact selection.")
            }
          case _ =>
        }
      case _ =>
    }
  }
  
  private def viewConversations(user: User): Unit = {
    println("\n=== CONVERSATIONS ===")
    
    messageDAO.getConversationPartners(user.id.get) match {
      case Success(partners) =>
        if (partners.isEmpty) {
          println("❌ No conversations found.")
        } else {
          println("Recent conversations:")
          partners.zipWithIndex.foreach { case (partnerId, index) =>
            userDAO.findById(partnerId) match {
              case Success(Some(partner)) =>
                println(s"${index + 1}. ${partner.firstName} ${partner.lastName}")
              case _ =>
            }
          }
          
          print("View conversation? Enter number (or 0 to go back): ")
          val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
          
          if (choice > 0 && choice <= partners.length) {
            val partnerId = partners(choice - 1)
            viewConversation(user, partnerId)
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading conversations: ${e.getMessage}")
    }
  }
  
  private def viewConversation(user: User, partnerId: Int): Unit = {
    userDAO.findById(partnerId) match {
      case Success(Some(partner)) =>
        println(s"\n=== CONVERSATION WITH: ${partner.firstName} ${partner.lastName} ===")
        
        messageDAO.findConversation(user.id.get, partnerId) match {
          case Success(messages) =>
            if (messages.isEmpty) {
              println("❌ No messages in this conversation.")
            } else {
              messages.foreach { message =>
                val direction = if (message.senderId == user.id.get) "You" else partner.firstName
                println(s"\n[$direction] ${message.sentAt}")
                message.subject.foreach(subject => println(s"Subject: $subject"))
                println(s"${message.content}")
              }
            }
          case Failure(e) =>
            println(s"❌ Error loading conversation: ${e.getMessage}")
        }
      case _ =>
        println("❌ Partner not found.")
    }
  }
  
  private def showPaymentHistory(user: User): Unit = {
    println("\n=== PAYMENT HISTORY ===")
    
    paymentDAO.findByUser(user.id.get) match {
      case Success(payments) =>
        if (payments.isEmpty) {
          println("❌ No payment history found.")
          
          reservationDAO.findByPassenger(user.id.get) match {
            case Success(reservations) =>
              val activeReservations = reservations.filter(_.status == ReservationStatus.ACTIVE)
              if (activeReservations.nonEmpty) {
                println("\n💳 Process pending payments:")
                activeReservations.zipWithIndex.foreach { case (reservation, index) =>
                  tripDAO.findById(reservation.tripId) match {
                    case Success(Some(trip)) =>
                      userDAO.findById(trip.driverId) match {
                        case Success(Some(driver)) =>
                          println(s"${index + 1}. Pay €${reservation.totalPrice} to ${driver.firstName} ${driver.lastName}")
                          println(s"   Trip: ${trip.routeDescription} on ${trip.departureDate}")
                        case _ =>
                      }
                    case _ =>
                  }
                }
                
                print("\nProcess payment? Enter number (or 0 to skip): ")
                val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
                if (choice > 0 && choice <= activeReservations.length) {
                  processPayment(activeReservations(choice - 1), user)
                }
              }
            case _ =>
          }
        } else {
          println(s"📊 Showing last ${Math.min(payments.length, 10)} payments:")
          payments.take(10).foreach { payment =>
            val direction = if (payment.payerId == user.id.get) "Paid to" else "Received from"
            val otherUserId = if (payment.payerId == user.id.get) payment.receiverId else payment.payerId
            
            userDAO.findById(otherUserId) match {
              case Success(Some(otherUser)) =>
                println(s"💳 $direction ${otherUser.firstName} ${otherUser.lastName}: €${payment.amount}")
                println(s"   Status: ${payment.status} | Date: ${payment.transactionDate}")
              case _ =>
                println(s"💳 $direction Unknown User: €${payment.amount} - ${payment.status}")
            }
          }
          
          if (user.isDriver) {
            paymentDAO.getTotalEarnings(user.id.get) match {
              case Success(earnings) =>
                println(f"\n💰 Total earnings: €$earnings")
              case _ =>
            }
          }
          
          paymentDAO.getTotalSpent(user.id.get) match {
            case Success(spent) =>
              println(f"💸 Total spent: €$spent")
            case _ =>
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading payment history: ${e.getMessage}")
    }
  }
  
  private def processPayment(reservation: models.Reservation, payer: User): Unit = {
    tripDAO.findById(reservation.tripId) match {
      case Success(Some(trip)) =>
        userDAO.findById(trip.driverId) match {
          case Success(Some(driver)) =>
            println(s"\n=== PROCESS PAYMENT ===")
            println(s"Amount: €${reservation.totalPrice}")
            println(s"To: ${driver.firstName} ${driver.lastName}")
            println(s"For: ${trip.routeDescription}")
            println("Payment methods: 1) Cash  2) Card  3) Bank Transfer")
            print("Select payment method (1-3): ")
            
            val methodChoice = StdIn.readLine().trim
            val paymentMethod = methodChoice match {
              case "1" => "CASH"
              case "2" => "CARD" 
              case "3" => "BANK_TRANSFER"
              case _ => "CASH"
            }
            
            print("Confirm payment? (y/n): ")
            if (StdIn.readLine().trim.toLowerCase == "y") {
              val payment = models.Payment.create(
                reservationId = reservation.id.get,
                payerId = payer.id.get,
                receiverId = driver.id.get,
                amount = reservation.totalPrice,
                paymentMethod = paymentMethod
              )
              
              paymentDAO.create(payment) match {
                case Success(_) =>
                  println("✅ Payment processed successfully!")
                  
                  paymentDAO.updateStatus(payment.id.get, PaymentStatus.COMPLETED) match {
                    case Success(_) =>
                      println("💳 Payment confirmed and completed.")
                    case _ =>
                  }
                case Failure(e) =>
                  println(s"❌ Payment processing failed: ${e.getMessage}")
              }
            }
          case _ =>
            println("❌ Driver not found.")
        }
      case _ =>
        println("❌ Trip not found.")
    }
  }
  
  private def showRatings(user: User): Unit = {
    println("\n=== RATINGS ===")
    
    ratingDAO.getAverageRating(user.id.get) match {
      case Success(Some(avgRating)) =>
        ratingDAO.getRatingCount(user.id.get) match {
          case Success(count) =>
            println(f"⭐ Your average rating: $avgRating%.1f/5.0 ($count reviews)")
          case _ =>
            println("⭐ No ratings yet")
        }
      case _ =>
        println("⭐ No ratings yet")
    }
    
    ratingDAO.getRecentRatings(user.id.get, 5) match {
      case Success(ratings) =>
        if (ratings.nonEmpty) {
          println("\nRecent ratings received:")
          ratings.foreach { rating =>
            userDAO.findById(rating.raterId) match {
              case Success(Some(rater)) =>
                val raterName = s"${rater.firstName} ${rater.lastName}"
                println(s"⭐ ${rating.rating}/5 from $raterName - ${rating.comment.getOrElse("No comment")}")
              case _ =>
                println(s"⭐ ${rating.rating}/5 - ${rating.comment.getOrElse("No comment")}")
            }
          }
        }
      case Failure(e) =>
        println(s"❌ Error loading ratings: ${e.getMessage}")
    }
    
    reservationDAO.findByPassenger(user.id.get) match {
      case Success(reservations) =>
        val completedReservations = reservations.filter(_.status == ReservationStatus.COMPLETED)
        val unratedTrips = completedReservations.filter { reservation =>
          ratingDAO.hasRated(reservation.tripId, user.id.get, reservation.tripId) match {
            case Success(hasRated) => !hasRated
            case _ => false
          }
        }
        
        if (unratedTrips.nonEmpty) {
          println(s"\n📝 You have ${unratedTrips.length} completed trip(s) to rate:")
          unratedTrips.take(5).zipWithIndex.foreach { case (reservation, index) =>
            tripDAO.findById(reservation.tripId) match {
              case Success(Some(trip)) =>
                userDAO.findById(trip.driverId) match {
                  case Success(Some(driver)) =>
                    println(s"${index + 1}. Rate ${driver.firstName} ${driver.lastName} for ${trip.routeDescription}")
                  case _ =>
                }
              case _ =>
            }
          }
          
          print("\nRate a driver? Enter number (or 0 to skip): ")
          val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
          if (choice > 0 && choice <= Math.min(unratedTrips.length, 5)) {
            rateDriver(unratedTrips(choice - 1), user)
          }
        }
      case _ =>
    }
    
    if (user.isDriver) {
      tripDAO.findByDriver(user.id.get) match {
        case Success(trips) =>
          val completedTrips = trips.filter(_.status == TripStatus.COMPLETED)
          completedTrips.foreach { trip =>
            reservationDAO.findByTrip(trip.id.get) match {
              case Success(reservations) =>
                val unratedPassengers = reservations.filter { reservation =>
                  ratingDAO.hasRated(trip.id.get, user.id.get, reservation.passengerId) match {
                    case Success(hasRated) => !hasRated && reservation.status == ReservationStatus.COMPLETED
                    case _ => false
                  }
                }
                
                if (unratedPassengers.nonEmpty) {
                  println(s"\n📝 Rate passengers from ${trip.routeDescription}:")
                  unratedPassengers.zipWithIndex.foreach { case (reservation, index) =>
                    userDAO.findById(reservation.passengerId) match {
                      case Success(Some(passenger)) =>
                        println(s"${index + 1}. Rate ${passenger.firstName} ${passenger.lastName}")
                      case _ =>
                    }
                  }
                  
                  print("\nRate a passenger? Enter number (or 0 to skip): ")
                  val choice = Try(StdIn.readLine().trim.toInt).getOrElse(0)
                  if (choice > 0 && choice <= unratedPassengers.length) {
                    ratePassenger(trip, unratedPassengers(choice - 1), user)
                  }
                }
              case _ =>
            }
          }
        case _ =>
      }
    }
  }
  
  private def rateDriver(reservation: models.Reservation, passenger: User): Unit = {
    tripDAO.findById(reservation.tripId) match {
      case Success(Some(trip)) =>
        userDAO.findById(trip.driverId) match {
          case Success(Some(driver)) =>
            println(s"\n=== RATE DRIVER: ${driver.firstName} ${driver.lastName} ===")
            println(s"Trip: ${trip.routeDescription}")
            print("Rating (1-5): ")
            val rating = Try(StdIn.readLine().trim.toInt).getOrElse(0)
            
            if (rating < 1 || rating > 5) {
              println("❌ Rating must be between 1 and 5.")
              return
            }
            
            print("Comment (optional): ")
            val commentInput = StdIn.readLine().trim
            val comment = if (commentInput.isEmpty) None else Some(commentInput)
            
            val ratingRecord = models.Rating.create(
              tripId = trip.id.get,
              raterId = passenger.id.get,
              ratedId = driver.id.get,
              rating = rating,
              comment = comment
            )
            
            ratingDAO.create(ratingRecord) match {
              case Success(_) =>
                println("✅ Rating submitted successfully!")
              case Failure(e) =>
                println(s"❌ Rating submission failed: ${e.getMessage}")
            }
          case _ =>
            println("❌ Driver not found.")
        }
      case _ =>
        println("❌ Trip not found.")
    }
  }
  
  private def ratePassenger(trip: models.Trip, reservation: models.Reservation, driver: User): Unit = {
    userDAO.findById(reservation.passengerId) match {
      case Success(Some(passenger)) =>
        println(s"\n=== RATE PASSENGER: ${passenger.firstName} ${passenger.lastName} ===")
        println(s"Trip: ${trip.routeDescription}")
        print("Rating (1-5): ")
        val rating = Try(StdIn.readLine().trim.toInt).getOrElse(0)
        
        if (rating < 1 || rating > 5) {
          println("❌ Rating must be between 1 and 5.")
          return
        }
        
        print("Comment (optional): ")
        val commentInput = StdIn.readLine().trim
        val comment = if (commentInput.isEmpty) None else Some(commentInput)
        
        val ratingRecord = models.Rating.create(
          tripId = trip.id.get,
          raterId = driver.id.get,
          ratedId = passenger.id.get,
          rating = rating,
          comment = comment
        )
        
        ratingDAO.create(ratingRecord) match {
          case Success(_) =>
            println("✅ Rating submitted successfully!")
          case Failure(e) =>
            println(s"❌ Rating submission failed: ${e.getMessage}")
        }
      case _ =>
        println("❌ Passenger not found.")
    }
  }
}

@main
def main(): Unit = {
  val app = new RidesharingApp()
  app.start()
}