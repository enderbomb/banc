
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

                List<String> resultats = readStatementsEqualOrMore(cnn, 5.0);

                for (String resultat : resultats) {
                    System.out.println("Moviment amb id: " + resultat);
                }

                Scanner sc = new Scanner(System.in);
                System.out.println("Nom? ");
                String nom = sc.nextLine();
                System.out.println("Cognom? ");
                String cognom = sc.nextLine();
                System.out.println("DNI? ");
                String dni = sc.nextLine();
                String[] datosUsuario = {nom, cognom, dni};
                createUser(cnn, datosUsuario);

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
        }

        return resultatQuery;
    }

    public static void createUser(Connection conn, String[] user) throws SQLException {
        if (existUser(conn, user)) {
            System.out.println("El usuario ya existe");
        } else {
            String qrry = "INSERT INTO clients (nom,cognoms,id_fiscal) VALUES (?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(qrry)) {

                ps.setString(1, user[0]);
                ps.setString(2, user[1]);
                ps.setString(3, user[2]);

                int filas = ps.executeUpdate();

                if (filas > 0) {
                    System.out.println("Usuario creado correctamente");
                }
            }
        }
    }

    public static boolean existUser(Connection conn, String[] user) throws SQLException {
        String query = "SELECT * FROM clients WHERE id_fiscal = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, user[2]);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
