
package iticbcn.banc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class Main {

    public static void main(String[] args) {
        Properties prop = new Properties();

        try {
            prop.load(Files.newBufferedReader(
                    Path.of("src", "main", "resources", "connection.properties")
            ));
        } catch (IOException e) {
            System.out.println("Error llegint el fitxer de configuració");
            e.printStackTrace();
            return;
        }

        HikariConfig hkcfg = new HikariConfig();

        hkcfg.setJdbcUrl(prop.getProperty("DB_URL"));
        hkcfg.setUsername(prop.getProperty("POSTGRES_USER"));
        hkcfg.setPassword(prop.getProperty("POSTGRES_PASSWORD"));

        hkcfg.setMaximumPoolSize(
                Integer.parseInt(prop.getProperty("DB_MAX_POOL", "5"))
        );

        try (HikariDataSource hkds = new HikariDataSource(hkcfg)) {

            try (Connection cnn = hkds.getConnection()) {

                System.out.println("Connectat!");

                // Llegim tots els moviments
                for (String resultat : readAllStatements(cnn)) {
                    System.out.println("Moviment amb id: " + resultat);
                }

                // Demanem una quantitat
                Scanner sc = new Scanner(System.in);
                System.out.println("Quantitat:");
                double quantitat = sc.nextDouble();
                sc.nextLine();

                // Llegim els moviments amb import igual o superior
                for (String resultat : readStatementsEqualOrMore(cnn, quantitat)) {
                    System.out.println(resultat);
                }

                // Demanem les dades de l'usuari
                System.out.println("Nom? ");
                String nom = sc.nextLine();

                System.out.println("Cognom? ");
                String cognom = sc.nextLine();

                System.out.println("DNI? ");
                String dni = sc.nextLine();

                String[] datosUsuario = {nom, cognom, dni};

                System.out.println(createUser(cnn, datosUsuario));

            } catch (SQLException e) {
                System.out.println("No connectat!");
                e.printStackTrace();
            }

        } catch (PoolInitializationException e) {
            System.out.println(
                    "No s'ha pogut connectar a la base de dades: "
                            + e.getMessage()
            );
        }
    }

    public static List<String> readAllStatements(Connection conn)
            throws SQLException {

        List<String> resultatQuery = new ArrayList<>();

        String query = "SELECT * FROM statements";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String resultat = String.join(";",
                        rs.getString("id_statement"),
                        rs.getString("tipus_moviment"),
                        rs.getString("import"),
                        rs.getString("divisa")
                );

                resultatQuery.add(resultat);
            }
        }

        return resultatQuery;
    }

    public static List<String> readStatementsEqualOrMore(Connection conn, double num)
            throws SQLException {

        List<String> resultatQuery = new ArrayList<>();

        String query = "SELECT * FROM statements WHERE import >= ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setDouble(1, num);

            try (ResultSet rs = ps.executeQuery()) {

                // Obtenim les metadades
                ResultSetMetaData meta = rs.getMetaData();

                // Construïm la capçalera
                String header = String.join(";",
                        meta.getColumnName(1),
                        meta.getColumnName(3),
                        meta.getColumnName(8),
                        meta.getColumnName(10)
                );

                resultatQuery.add(header);

                // Recorrem els resultats
                while (rs.next()) {

                    String resultat = String.join(";",
                            rs.getString(1),
                            rs.getString(3),
                            rs.getString(8),
                            rs.getString(10)
                    );

                    resultatQuery.add(resultat);
                }
            }
        }

        return resultatQuery;
    }

    public static boolean createUser(Connection conn, String[] user) throws SQLException {

        if (existUser(conn, user)) {
            return false;
        }

        String qrry = "INSERT INTO clients (nom,cognoms,id_fiscal) VALUES (?,?,?)";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(qrry)) {

                ps.setString(1, user[0]);
                ps.setString(2, user[1]);
                ps.setString(3, user[2]);

                int filas = ps.executeUpdate();

                if (filas == 1) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            conn.rollback();
            throw e;

        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static boolean existUser(Connection conn, String[] user) throws SQLException {

        String query = "SELECT * FROM clients WHERE id_fiscal = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, user[2]);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
