package org.hbmc;

import org.hbmc.config.DatabaseInitializer;
import org.hbmc.db.DatabaseConnection;
import org.hbmc.dto.AvailableRoomDTO;
import org.hbmc.dto.ReservationSummaryDTO;
import org.hbmc.dto.RoomSearchCriteria;
import org.hbmc.exception.AuthenticationException;
import org.hbmc.exception.InvalidInputException;
import org.hbmc.exception.InvalidReservationException;
import org.hbmc.exception.ReservationAlreadyCancelledException;
import org.hbmc.exception.ReservationNotFoundException;
import org.hbmc.exception.ResourceAlreadyExistsException;
import org.hbmc.exception.RoomCapacityExceededException;
import org.hbmc.exception.RoomNotAvailableException;
import org.hbmc.exception.RoomNotFoundException;
import org.hbmc.exception.UnauthorizedReservationAccessException;
import org.hbmc.model.CanceledReservation;
import org.hbmc.model.Client;
import org.hbmc.model.Invoice;
import org.hbmc.model.Payment;
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
import org.hbmc.util.ValidationUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;

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
            String fullName = Main.promptValid("Full name: ", ValidationUtils::isValidFullName, "Please enter a full name.");
            String email = Main.promptValid("Email: ", ValidationUtils::isValidEmail, "Please enter a valid email.");
            String password = Main.promptValid("Password (min 6 chars): ", ValidationUtils::isValidPassword, "Password must be at least 6 characters.");
            String phone = Main.promptValid("Phone: ", ValidationUtils::isValidPhone, "Please enter a valid phone number.");

            Client newClient = new Client(fullName, email, null, null, phone);
            Main.currentUser = Main.authService.register(newClient, password);
            System.out.println("Registered successfully. Welcome, " + Main.currentUser.getFullName() + "!");
        } catch (ResourceAlreadyExistsException e) {
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
                case 6 -> Main.viewInvoiceFlow(client);
                case 0 -> loggedIn = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void searchAvailableRooms() {
        LocalDate checkIn = Main.input.readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = Main.input.readDate("Check-out date (YYYY-MM-DD): ");

        List<Room> rooms;
        try {
            rooms = Main.roomService.findAvailableRooms(checkIn, checkOut);
        } catch (InvalidInputException e) {
            System.out.println("Search failed: " + e.getMessage());
            return;
        }
        if (rooms.isEmpty()) {
            System.out.println("No rooms available for those dates.");
            return;
        }

        RoomSearchCriteria criteria = Main.roomSearchCriteria(checkIn, checkOut);

        List<AvailableRoomDTO> matches = rooms.stream()
                .filter(room -> Main.matchesCriteria(room, criteria))
                .map(room -> Main.pricingService.quoteForRoom(room, criteria.getCheckIn(), criteria.getCheckOut()))
                .toList();

        if (matches.isEmpty()) {
            System.out.println("No rooms match the given filters.");
            return;
        }
        System.out.println("Available rooms (estimated total, HT):");
        for (AvailableRoomDTO match : matches) {
            System.out.printf("  [%d] Room %s | %s | capacity %d | estimated total: %s%n",
                    match.getRoomId(), match.getRoomNumber(), match.getType(),
                    match.getCapacity(), match.getEstimatedTotalPrice());
        }
    }

    private static RoomSearchCriteria roomSearchCriteria(LocalDate checkIn, LocalDate checkOut) {
        RoomSearchCriteria criteria = new RoomSearchCriteria(checkIn, checkOut);
        System.out.println("Filters: 0. no filter  1. room type  2. min capacity  3. max price per night");
        int filterChoice = Main.input.readInt("Choose a filter (0-3): ");
        switch (filterChoice) {
            case 1 -> criteria.setType(Main.readRoomType());
            case 2 -> criteria.setMinCapacity(Main.input.readInt("Minimum capacity: "));
            case 3 -> criteria.setMaxPricePerNight(Main.input.readBigDecimal("Maximum price per night: "));
            case 0 -> System.out.println("No filter applied.");
            default -> System.out.println("Unknown filter, applying none.");
        }
        return criteria;
    }

    private static boolean matchesCriteria(Room room, RoomSearchCriteria criteria) {
        if (criteria.getType() != null && room.getType() != criteria.getType()) {
            return false;
        }
        if (criteria.getMinCapacity() != null && room.getCapacity() < criteria.getMinCapacity()) {
            return false;
        }
        return criteria.getMaxPricePerNight() == null
                || room.getPricePerNight().compareTo(criteria.getMaxPricePerNight()) <= 0;
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

        BigDecimal quote = Main.pricingService.quotePrice(room, checkIn, checkOut);
        System.out.println("Estimated total (HT): " + quote + " for "
                + ChronoUnit.DAYS.between(checkIn, checkOut) + " night(s)");

        PaymentMethod method = Main.readPaymentMethod();

        try {
            Reservation reservation = Main.reservationService.bookReservation(client, room, checkIn, checkOut, guests, method);
            System.out.println("Booked! Reservation code: " + reservation.getReservationCode());
        } catch (RoomNotAvailableException | RoomCapacityExceededException | InvalidReservationException
                 | InvalidInputException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    private static void listMyReservations(Client client) {
        List<ReservationSummaryDTO> summaries = Main.reservationService.getReservationsForClient(client).stream()
                .map(reservation -> new ReservationSummaryDTO(
                        reservation,
                        Main.paymentService.findByReservationId(reservation.getId())
                                .map(Payment::getTotal)
                                .orElse(BigDecimal.ZERO)))
                .toList();

        if (summaries.isEmpty()) {
            System.out.println("You have no reservations.");
            return;
        }
        for (ReservationSummaryDTO summary : summaries) {
            System.out.printf("[%d] %s | Room %s | %s to %s | status: %s | total HT: %s%n",
                    summary.getReservationId(), summary.getReservationCode(), summary.getRoomNumber(),
                    summary.getCheckIn(), summary.getCheckOut(), summary.getStatus(), summary.getTotalPrice());
        }
    }

    private static void updateReservationFlow(Client client) {
        String code = Main.input.readNonEmptyLine("Reservation code: ");
        LocalDate newCheckIn = Main.input.readDate("New check-in date (YYYY-MM-DD): ");
        LocalDate newCheckOut = Main.input.readDate("New check-out date (YYYY-MM-DD): ");
        int newGuests = Main.input.readInt("New number of guests: ");

        try {
            Reservation updated = Main.reservationService.updateReservation(client, code, newCheckIn, newCheckOut, newGuests);
            System.out.println("Reservation updated: " + updated.getCheckIn() + " to " + updated.getCheckOut()
                    + ", " + updated.getNumberOfGuests() + " guest(s).");
        } catch (ReservationNotFoundException | UnauthorizedReservationAccessException
                 | ReservationAlreadyCancelledException | RoomNotAvailableException
                 | RoomCapacityExceededException | InvalidReservationException | InvalidInputException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private static void cancelReservationFlow(Client client) {
        String code = Main.input.readNonEmptyLine("Reservation code: ");
        try {
            CanceledReservation canceled = Main.reservationService.cancelReservation(client, code);
            System.out.println("Cancelled. Type: " + canceled.getType()
                    + " | Refund amount: " + canceled.getRefundAmount());
        } catch (ReservationNotFoundException | UnauthorizedReservationAccessException
                 | ReservationAlreadyCancelledException | InvalidReservationException e) {
            System.out.println("Cancellation failed: " + e.getMessage());
        }
    }

    private static void viewInvoiceFlow(Client client) {
        String code = Main.input.readNonEmptyLine("Reservation code: ");

        Reservation reservation = Main.reservationService.getReservationsForClient(client).stream()
                .filter(r -> r.getReservationCode().equals(code))
                .findFirst()
                .orElse(null);

        if (reservation == null) {
            System.out.println("No reservation found with code: " + code);
            return;
        }

        Payment payment = Main.paymentService.findByReservationId(reservation.getId()).orElse(null);
        if (payment == null) {
            System.out.println("No payment found for that reservation.");
            return;
        }
        Invoice invoice = Main.invoiceService.findByPaymentId(payment.getId()).orElse(null);
        if (invoice == null) {
            System.out.println("No invoice found for that payment.");
            return;
        }

        System.out.println("--- Invoice for " + reservation.getReservationCode() + " ---");
        System.out.println("Payment id: " + payment.getId() + " | method: " + payment.getMethod()
                + " | status: " + payment.getStatus() + " | date: " + payment.getPaymentDate());
        System.out.println("HT: " + invoice.getOffTax());
        System.out.println("TVA (20%): " + invoice.getTax());
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
            System.out.println("5. View all reservations");
            System.out.println("6. View KPI report");
            System.out.println("0. Logout");
            int choice = Main.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> Main.createRoomFlow();
                case 2 -> Main.updateRoomStatusFlow();
                case 3 -> Main.updateRoomPriceFlow();
                case 4 -> Main.listAllRooms();
                case 5 -> Main.listAllReservations();
                case 6 -> Main.showReport();
                case 0 -> loggedIn = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void createRoomFlow() {
        String roomNumber = Main.input.readNonEmptyLine("Room number: ");
        RoomType type = Main.readRoomType();
        int capacity = Main.input.readInt("Capacity: ");
        BigDecimal price = Main.input.readBigDecimal("Price per night: ");

        try {
            Room room = Main.roomService.createRoom(roomNumber, type, capacity, price);
            System.out.println("Room created with id " + room.getId() + ".");
        } catch (ResourceAlreadyExistsException e) {
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
        } catch (RoomNotFoundException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void updateRoomPriceFlow() {
        int roomId = Main.input.readInt("Room id: ");
        BigDecimal newPrice = Main.input.readBigDecimal("New price per night: ");

        try {
            Main.roomService.updateRoomPrice(roomId, newPrice);
            System.out.println("Room price updated.");
        } catch (RoomNotFoundException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void listAllRooms() {
        List<Room> rooms = Main.roomService.findAllRooms();
        if (rooms.isEmpty()) {
            System.out.println("No rooms registered.");
            return;
        }
        for (Room room : rooms) {
            System.out.printf("[%d] %s | %s | capacity %d | %s/night | %s%n",
                    room.getId(), room.getRoomNumber(), room.getType(), room.getCapacity(),
                    room.getPricePerNight(), room.getStatus());
        }
    }

    private static void listAllReservations() {
        try {
            Main.authService.assertRole(Main.currentUser, "ADMIN");
        } catch (AuthenticationException e) {
            System.out.println(e.getMessage());
            return;
        }

        List<Reservation> reservations = Main.reservationService.getAllReservations();
        if (reservations.isEmpty()) {
            System.out.println("No reservations yet.");
            return;
        }
        for (Reservation reservation : reservations) {
            System.out.printf("[%d] %s | %s | Room %s | %s to %s | %d guest(s) | status: %s%n",
                    reservation.getId(), reservation.getReservationCode(),
                    reservation.getClient().getFullName(), reservation.getRoom().getRoomNumber(),
                    reservation.getCheckIn(), reservation.getCheckOut(),
                    reservation.getNumberOfGuests(), reservation.getStatus());
        }
    }

    private static void showReport() {
        try {
            Main.authService.assertRole(Main.currentUser, "ADMIN");
        } catch (AuthenticationException e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.println("--- KPI Report ---");
        System.out.println("Total revenue: " + Main.reportService.totalRevenue());
        System.out.println("Average booking value: " + Main.reportService.averageBookingValue());
        System.out.println("Occupancy rate: " + Main.reportService.occupancyRate() + "%");
        System.out.println("Total reservations: " + Main.reportService.totalReservationsCount());
        System.out.println("Cancelled reservations: " + Main.reportService.cancelledReservationsCount());

        System.out.println("Most booked rooms:");
        List<Map.Entry<Room, Long>> mostBooked = Main.reportService.mostBookedRooms(5);
        if (mostBooked.isEmpty()) {
            System.out.println("  (no reservations yet)");
        }
        for (Map.Entry<Room, Long> entry : mostBooked) {
            System.out.printf("  Room %s (%s): %d booking(s)%n",
                    entry.getKey().getRoomNumber(), entry.getKey().getType(), entry.getValue());
        }
    }

    // ####################Shared input helpers

    private static String promptValid(String prompt, Predicate<String> validator, String errorMessage) {
        while (true) {
            String value = Main.input.readNonEmptyLine(prompt);
            if (validator.test(value)) {
                return value;
            }
            System.out.println(errorMessage);
        }
    }

    private static RoomType readRoomType() {
        System.out.println("Type: 1. SINGLE  2. DOUBLE  3. SUITE");
        int typeChoice = Main.input.readInt("Choose: ");
        return switch (typeChoice) {
            case 2 -> RoomType.DOUBLE;
            case 3 -> RoomType.SUITE;
            default -> RoomType.SINGLE;
        };
    }

    private static PaymentMethod readPaymentMethod() {
        System.out.println("Payment method: 1. CREDIT_CARD  2. CASH");
        int methodChoice = Main.input.readInt("Choose: ");
        return methodChoice == 2 ? PaymentMethod.CASH : PaymentMethod.CREDIT_CARD;
    }
}
