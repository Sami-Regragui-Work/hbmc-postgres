package org.hbmc;

import org.hbmc.config.DatabaseInitializer;
import org.hbmc.db.DatabaseConnection;
import org.hbmc.exception.AuthenticationException;
import org.hbmc.exception.InvalidReservationException;
import org.hbmc.exception.ReservationAlreadyCancelledException;
import org.hbmc.exception.ReservationNotFoundException;
import org.hbmc.exception.RoomCapacityExceededException;
import org.hbmc.exception.RoomNotAvailableException;
import org.hbmc.exception.UnauthorizedReservationAccessException;
import org.hbmc.model.Client;
import org.hbmc.model.Reservation;
import org.hbmc.model.Room;
import org.hbmc.model.User;
import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.RoomStatus;
import org.hbmc.model.enums.RoomType;
import org.hbmc.policy.PricingStrategy;
import org.hbmc.policy.RefundPolicy;
import org.hbmc.policy.StandardPricingStrategy;
import org.hbmc.policy.StandardRefundPolicy;
import org.hbmc.repository.InvoiceRepository;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.ReservationRepository;
import org.hbmc.repository.RoomRepository;
import org.hbmc.repository.UserRepository;
import org.hbmc.repository.jdbc.InvoiceRepositoryJDBC;
import org.hbmc.repository.jdbc.PaymentRepositoryJDBC;
import org.hbmc.repository.jdbc.ReservationRepositoryJDBC;
import org.hbmc.repository.jdbc.RoomRepositoryJDBC;
import org.hbmc.repository.jdbc.UserRepositoryJDBC;
import org.hbmc.service.AuthService;
import org.hbmc.service.InvoiceService;
import org.hbmc.service.PaymentService;
import org.hbmc.service.PricingService;
import org.hbmc.service.ReportService;
import org.hbmc.service.ReservationService;
import org.hbmc.service.RoomService;
import org.hbmc.util.InputUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static AuthService authService;
    private static RoomService roomService;
    private static ReservationService reservationService;
    private static PaymentService paymentService;
    private static InvoiceService invoiceService;
    private static PricingService pricingService;
    private static ReportService reportService;
    private static InputUtils input;

    private static User currentUser;

    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        Connection connection = DatabaseConnection.getInstance().getConnection();

        UserRepository userRepository = new UserRepositoryJDBC();
        RoomRepository roomRepository = new RoomRepositoryJDBC();
        ReservationRepository reservationRepository = new ReservationRepositoryJDBC();
        PaymentRepository paymentRepository = new PaymentRepositoryJDBC();
        InvoiceRepository invoiceRepository = new InvoiceRepositoryJDBC();

        PricingStrategy pricingStrategy = new StandardPricingStrategy();
        RefundPolicy refundPolicy = new StandardRefundPolicy();

        Main.authService = new AuthService(userRepository);
        Main.roomService = new RoomService(roomRepository);
        Main.pricingService = new PricingService(pricingStrategy);
        Main.paymentService = new PaymentService(paymentRepository);
        Main.invoiceService = new InvoiceService(invoiceRepository);
        Main.reportService = new ReportService(reservationRepository, paymentRepository, roomRepository);
        Main.reservationService = new ReservationService(
                reservationRepository,
                paymentRepository,
                invoiceRepository,
                roomRepository,
                connection,
                pricingStrategy,
                refundPolicy
        );

        Main.input = new InputUtils(new Scanner(System.in));

        System.out.println("=== Hotel Booking Management System ===");
        Main.runWelcomeLoop();
    }

    private static void runWelcomeLoop() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            int choice = Main.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> Main.handleLogin();
                case 2 -> Main.handleRegister();
                case 0 -> running = false;
                default -> System.out.println("Invalid option.");
            }

            if (Main.currentUser != null) {
                Main.runRoleMenu();
                Main.currentUser = null;
            }
        }
        System.out.println("Goodbye.");
    }

    private static void handleLogin() {
        try {
            String email = Main.input.readNonEmptyLine("Email: ");
            String password = Main.input.readNonEmptyLine("Password: ");
            Main.currentUser = Main.authService.login(email, password);
            System.out.println("Welcome, " + Main.currentUser.getFullName() + " (" + Main.currentUser.getRole() + ")");
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void handleRegister() {
        try {
            String fullName = Main.input.readNonEmptyLine("Full name: ");
            String email = Main.input.readNonEmptyLine("Email: ");
            String password = Main.input.readNonEmptyLine("Password: ");
            String phone = Main.input.readNonEmptyLine("Phone: ");

            Client newClient = new Client(fullName, email, null, null, phone);
            Main.currentUser = Main.authService.register(newClient, password);
            System.out.println("Registered successfully. Welcome, " + Main.currentUser.getFullName() + "!");
        } catch (IllegalArgumentException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private static void runRoleMenu() {
        if ("ADMIN".equalsIgnoreCase(Main.currentUser.getRole())) {
            Main.runAdminMenu();
        } else {
            Main.runClientMenu((Client) Main.currentUser);
        }
    }

    // #############Client Menu

    private static void runClientMenu(Client client) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- Client Menu (" + client.getFullName() + ") ---");
            System.out.println("1. Search available rooms");
            System.out.println("2. Book a room");
            System.out.println("3. My reservations");
            System.out.println("4. Update a reservation");
            System.out.println("5. Cancel a reservation");
            System.out.println("6. View invoice for a reservation");
            System.out.println("0. Logout");
            int choice = Main.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> Main.searchAvailableRooms();
                case 2 -> Main.bookRoom(client);
                case 3 -> Main.listMyReservations(client);
                case 4 -> Main.updateReservationFlow(client);
                case 5 -> Main.cancelReservationFlow(client);
                case 6 -> Main.viewInvoiceFlow();
                case 0 -> loggedIn = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void searchAvailableRooms() {
        LocalDate checkIn = Main.input.readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = Main.input.readDate("Check-out date (YYYY-MM-DD): ");

        List<Room> rooms = Main.roomService.findAvailableRooms(checkIn, checkOut);
        if (rooms.isEmpty()) {
            System.out.println("No rooms available for those dates.");
            return;
        }
        for (Room room : rooms) {
            BigDecimal quote = Main.pricingService.quotePrice(room, checkIn, checkOut);
            System.out.printf("Room %s | %s | capacity %d | estimated total: %s%n",
                    room.getRoomNumber(), room.getType(), room.getCapacity(), quote);
        }
    }

    private static void bookRoom(Client client) {
        LocalDate checkIn = Main.input.readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = Main.input.readDate("Check-out date (YYYY-MM-DD): ");
        String roomNumber = Main.input.readNonEmptyLine("Room number: ");
        int guests = Main.input.readInt("Number of guests: ");

        Room room = Main.roomService.findAllRooms().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .findFirst()
                .orElse(null);

        if (room == null) {
            System.out.println("Room not found.");
            return;
        }

        System.out.println("Payment method: 1. CREDIT_CARD  2. CASH");
        int methodChoice = Main.input.readInt("Choose: ");
        PaymentMethod method = methodChoice == 1 ? PaymentMethod.CREDIT_CARD : PaymentMethod.CASH;

        try {
            Reservation reservation = Main.reservationService.bookReservation(client, room, checkIn, checkOut, guests, method);
            System.out.println("Booked! Reservation code: " + reservation.getReservationCode());
        } catch (RoomNotAvailableException | RoomCapacityExceededException | InvalidReservationException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    private static void listMyReservations(Client client) {
        List<Reservation> reservations = Main.reservationService.getReservationsForClient(client);
        if (reservations.isEmpty()) {
            System.out.println("You have no reservations.");
            return;
        }
        for (Reservation r : reservations) {
            System.out.printf("[%s] Room %s | %s to %s | status: %s%n",
                    r.getReservationCode(), r.getRoom().getRoomNumber(), r.getCheckIn(), r.getCheckOut(), r.getStatus());
        }
    }

    private static void updateReservationFlow(Client client) {
        String code = Main.input.readNonEmptyLine("Reservation code: ");
        LocalDate newCheckIn = Main.input.readDate("New check-in date (YYYY-MM-DD): ");
        LocalDate newCheckOut = Main.input.readDate("New check-out date (YYYY-MM-DD): ");
        int newGuests = Main.input.readInt("New number of guests: ");

        try {
            Main.reservationService.updateReservation(client, code, newCheckIn, newCheckOut, newGuests);
            System.out.println("Reservation updated.");
        } catch (ReservationNotFoundException | UnauthorizedReservationAccessException
                 | ReservationAlreadyCancelledException | RoomNotAvailableException
                 | RoomCapacityExceededException | InvalidReservationException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private static void cancelReservationFlow(Client client) {
        String code = Main.input.readNonEmptyLine("Reservation code: ");
        try {
            var canceled = Main.reservationService.cancelReservation(client, code);
            System.out.println("Cancelled. Refund amount: " + canceled.getRefundAmount());
        } catch (ReservationNotFoundException | UnauthorizedReservationAccessException
                 | ReservationAlreadyCancelledException | InvalidReservationException e) {
            System.out.println("Cancellation failed: " + e.getMessage());
        }
    }

    private static void viewInvoiceFlow() {
        int reservationId = Main.input.readInt("Reservation id: ");
        var payment = Main.paymentService.findByReservationId(reservationId).orElse(null);
        if (payment == null) {
            System.out.println("No payment found for that reservation.");
            return;
        }
        var invoice = Main.invoiceService.findByPaymentId(payment.getId()).orElse(null);
        if (invoice == null) {
            System.out.println("No invoice found for that payment.");
            return;
        }
        System.out.println("HT: " + invoice.getOffTax());
        System.out.println("TVA: " + invoice.getTax());
        System.out.println("TTC: " + invoice.getOffTax().add(invoice.getTax()));
    }

    // ####################Admin Menu

    private static void runAdminMenu() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- Admin Menu ---");
            System.out.println("1. Create room");
            System.out.println("2. Update room status");
            System.out.println("3. Update room price");
            System.out.println("4. List all rooms");
            System.out.println("5. View KPI report");
            System.out.println("0. Logout");
            int choice = Main.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> Main.createRoomFlow();
                case 2 -> Main.updateRoomStatusFlow();
                case 3 -> Main.updateRoomPriceFlow();
                case 4 -> Main.listAllRooms();
                case 5 -> Main.showReport();
                case 0 -> loggedIn = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void createRoomFlow() {
        String roomNumber = Main.input.readNonEmptyLine("Room number: ");
        System.out.println("Type: 1. SINGLE  2. DOUBLE  3. SUITE");
        int typeChoice = Main.input.readInt("Choose: ");
        RoomType type = switch (typeChoice) {
            case 2 -> RoomType.DOUBLE;
            case 3 -> RoomType.SUITE;
            default -> RoomType.SINGLE;
        };
        int capacity = Main.input.readInt("Capacity: ");
        BigDecimal price = Main.input.readBigDecimal("Price per night: ");

        try {
            Main.roomService.createRoom(roomNumber, type, capacity, price);
            System.out.println("Room created.");
        } catch (IllegalArgumentException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void updateRoomStatusFlow() {
        int roomId = Main.input.readInt("Room id: ");
        System.out.println("Status: 1. AVAILABLE  2. MAINTENANCE");
        int statusChoice = Main.input.readInt("Choose: ");
        RoomStatus status = statusChoice == 2 ? RoomStatus.MAINTENANCE : RoomStatus.AVAILABLE;

        try {
            Main.roomService.updateRoomStatus(roomId, status);
            System.out.println("Room status updated.");
        } catch (IllegalArgumentException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void updateRoomPriceFlow() {
        int roomId = Main.input.readInt("Room id: ");
        BigDecimal newPrice = Main.input.readBigDecimal("New price per night: ");

        try {
            Main.roomService.updateRoomPrice(roomId, newPrice);
            System.out.println("Room price updated.");
        } catch (IllegalArgumentException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void listAllRooms() {
        List<Room> rooms = Main.roomService.findAllRooms();
        for (Room room : rooms) {
            System.out.printf("[%d] %s | %s | capacity %d | %s/night | %s%n",
                    room.getId(), room.getRoomNumber(), room.getType(), room.getCapacity(),
                    room.getPricePerNight(), room.getStatus());
        }
    }

    private static void showReport() {
        System.out.println("Total revenue: " + Main.reportService.totalRevenue());
        System.out.println("Average booking value: " + Main.reportService.averageBookingValue());
        System.out.println("Occupancy rate: " + Main.reportService.occupancyRate() + "%");
        System.out.println("Total reservations: " + Main.reportService.totalReservationsCount());
        System.out.println("Cancelled reservations: " + Main.reportService.cancelledReservationsCount());
    }
}