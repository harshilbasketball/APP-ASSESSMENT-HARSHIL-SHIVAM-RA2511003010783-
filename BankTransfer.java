
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class BankTransfer {

    static final String URL = "jdbc:mysql://localhost:3306/bankdb";
    static final String USER = "root";
    static final String PASSWORD = "Harshil@1625";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter sender account ID: ");
        int senderId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter receiver name: ");
        String receiverName = sc.nextLine().trim();

        System.out.print("Enter amount to transfer: ");
        BigDecimal amount = sc.nextBigDecimal();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            sc.close();
            return;
        }

        String findAccount =
            "SELECT account_id, holder_name, balance " +
            "FROM accounts WHERE account_id = ? FOR UPDATE";

        String findReceiver =
            "SELECT account_id, holder_name FROM accounts " +
            "WHERE holder_name = ?";

        String debit =
            "UPDATE accounts SET balance = balance - ? " +
            "WHERE account_id = ? AND balance >= ?";

        String credit =
            "UPDATE accounts SET balance = balance + ? " +
            "WHERE account_id = ?";

        try (Connection con =
                 DriverManager.getConnection(URL, USER, PASSWORD)) {

            con.setAutoCommit(false);

            try {
                int receiverId = -1;
                String senderName;
                BigDecimal senderBalance;

                try (PreparedStatement ps =
                         con.prepareStatement(findAccount)) {

                    ps.setInt(1, senderId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Sender account does not exist."
                            );
                        }

                        senderName = rs.getString("holder_name");
                        senderBalance = rs.getBigDecimal("balance");
                    }
                }

                try (PreparedStatement ps =
                         con.prepareStatement(findReceiver)) {

                    ps.setString(1, receiverName);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Receiver account does not exist."
                            );
                        }

                        receiverId = rs.getInt("account_id");

                        if (rs.next()) {
                            throw new SQLException(
                                "Multiple accounts have this name. " +
                                "Please use a unique receiver name."
                            );
                        }
                    }
                }

                if (senderId == receiverId) {
                    throw new SQLException(
                        "Sender and receiver cannot be the same account."
                    );
                }

                if (senderBalance.compareTo(amount) < 0) {
                    throw new SQLException("Insufficient balance.");
                }

                try (PreparedStatement ps =
                         con.prepareStatement(debit)) {

                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, senderId);
                    ps.setBigDecimal(3, amount);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Debit failed.");
                    }
                }

                try (PreparedStatement ps =
                         con.prepareStatement(credit)) {

                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, receiverId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Credit failed.");
                    }
                }

                con.commit();

                System.out.println("\nTransfer successful!");
                System.out.println("Sender: " + senderName);
                System.out.println("Receiver: " + receiverName);
                System.out.println("Amount transferred: " + amount);

            } catch (SQLException e) {
                try {
                    con.rollback();
                    System.out.println("\nTransaction rolled back.");
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                    System.out.println("Rollback failed!");
                }

                System.out.println("Transfer failed: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println(
                "Database connection error: " + e.getMessage()
            );
        }

        sc.close();
    }
}

