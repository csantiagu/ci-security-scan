package demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Greeting {

    // A bound parameter: the same lookup as the vulnerable demo, written safely.
    public static String nameFor(Connection db, String id) throws SQLException {
        try (PreparedStatement stmt = db.prepareStatement("SELECT name FROM users WHERE id = ?")) {
            stmt.setString(1, id);
            try (ResultSet rows = stmt.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }
}
