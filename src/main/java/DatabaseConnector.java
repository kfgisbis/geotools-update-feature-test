import java.sql.*;
import java.util.UUID;

public class DatabaseConnector {

    private final String dbUrl;
    private static final String USER = "postgres";
    private static final String PASS = "postgres";

    public DatabaseConnector(int port) {
        dbUrl = "jdbc:postgresql://localhost:" + port + "/postgres";
    }

    public String getRowById(String id) throws SQLException {
        String query = "SELECT ST_AsText(ST_GeomFromEWKB(geometry)) FROM bis.test WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, UUID.fromString(id));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next())
                    return null;

                return rs.getObject(1, String.class);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
