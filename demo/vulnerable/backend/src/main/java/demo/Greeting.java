package demo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Greeting {

    // SQL injection: the id is concatenated into the query. Deliberately vulnerable.
    public static String nameFor(Connection db, String id) throws SQLException {
        Statement stmt = db.createStatement();
        ResultSet rows = stmt.executeQuery("SELECT name FROM users WHERE id = '" + id + "'");
        return rows.next() ? rows.getString(1) : null;
    }
}
