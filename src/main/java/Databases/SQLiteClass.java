package Databases;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.*;

//TODO
//add finally

import java.sql.*;

public class SQLiteClass {
    public static Connection conn;
    public static Statement stat;
    public static ResultSet rs;

    public static void Conn() throws ClassNotFoundException, SQLException, NamingException {
        Class.forName("org.postgresql.Driver");

        //полный путь к базе данных

        conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1/videochat","postgres","ve;br");

    }

    //true
    public static boolean checkKeyGenDb(String keyGen) throws ClassNotFoundException, SQLException, NamingException {
        stat = conn.createStatement();

        //если найдено значение неиспользованное
        ResultSet rs = stat.executeQuery("select id from keyGens where keyGen = '" + keyGen + "'" +
                "and marker != " + "'registrated'");
        while (rs.next()) {
            rs.close();
            stat.close();
            return true;
        }

        rs.close();
        stat.close();
        return false;
    }

    //добавить пользователя в базу данных
    public static void addUserDatabase(String userName, String keyGen, String ip) throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();

        int n = stat.executeUpdate("UPDATE keyGens SET marker = 'registrated' WHERE keyGen = '" + keyGen + "'");

        stat.close();

        PreparedStatement statement = conn.prepareStatement("INSERT INTO freeUsers (name,  userKeyGen, userIp) VALUES ( ?, ?, ?)");
        statement.setString(1, userName);
        statement.setString(2, keyGen);
        statement.setString(3, ip);

        statement.execute();
        statement.close();
    }

    //получить имя по ключу
    public static String getNameDb(String keyGen) throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select name from freeUsers where userKeyGen = '" + keyGen + "'");

        while (rs.next()) {
            String answer = rs.getString("name");
            rs.close();
            stat.close();
            return answer;
        }

        rs.close();
        stat.close();

        return "";
    }

    //true
    public static void addUserIP(String ip) throws ClassNotFoundException, SQLException, NamingException {

        PreparedStatement statement = conn.prepareStatement("INSERT INTO usersIP (ip) VALUES (?)");

        try {
            statement.setString(1, ip);
            statement.execute();
        }
        catch (Exception e)
        {

        }
        finally {
            statement.close();
        }
    }

    public static boolean checkIP(String ip) throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();

        //если найдено значение неиспользованное
        ResultSet rs = stat.executeQuery("select id from usersIP where ip = '" + ip + "'");
        while (rs.next()) {
            rs.close();
            stat.close();
            return true;
        }

        rs.close();
        stat.close();

        return false;
    }


    public static String generateKeygen() throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select keyGen from keyGens where marker = " + "'not_used'");

        while (rs.next()) {
            String answer = rs.getString("keyGen");

            int n = stat.executeUpdate("UPDATE keyGens SET marker = 'sent' WHERE keyGen =" + "'" + answer + "'");

            rs.close();
            stat.close();

            return answer;
        }

        rs.close();
        stat.close();

        return "";
    }

    public static void CloseDB() throws ClassNotFoundException, SQLException {
        //conn.commit();
        conn.close();
    }

    public static String addUser(String userName, String keyGen, String ip) throws ClassNotFoundException, SQLException, NamingException {
        stat = conn.createStatement();

        ResultSet rs1 = stat.executeQuery("select userIp from freeUsers where userIp = '" + ip + "'" + " and name = '"
                + userName + "'");

        while (rs1.next()) {
            //rs.close();
            stat.close();
            return "added";
        }


        boolean marker = false;
        stat = conn.createStatement();

        //если найдено значение неиспользованное
        ResultSet rs = stat.executeQuery("select userIp from freeUsers where userIp = '" + ip + "'" + " and name != '"
                + userName + "'");
        while (rs.next()) {
            //rs.close();

            marker = true;
            break;
        }

        rs.close();
        //stat.close();

        if (marker) {
            int n = stat.executeUpdate("UPDATE freeUsers SET name = " + "'" + userName + "'" +
                    ",userKeyGen = " + "'" + keyGen + "'"
                    + "WHERE userIp =" + "'" + ip + "'");
            stat.close();
            return "added";
        }

        stat.close();


        PreparedStatement statement = conn.prepareStatement("INSERT INTO freeUsers (name,  userKeyGen, userIp) VALUES ( ?, ?, ?)");
        statement.setString(1, userName);
        statement.setString(2, keyGen);
        statement.setString(3, ip);

        statement.execute();
        statement.close();

        return "added";
    }

    public static void updateIP(String KeyGen, String IP) throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("select ip from freeUsers where keyGen = " + "'" + KeyGen + "'");

        String ipGet = "";

        while (rs.next()) {
            ipGet = rs.getString("ip");
        }

        int n = stat.executeUpdate("UPDATE freeUsers SET ip = " + "'" + IP + "'" +
                "WHERE keyGen =" + "'" + KeyGen + "'");

        int n1 = stat.executeUpdate("DELETE FROM usersIP WHERE ip = '" + ipGet + "'");

        stat.close();
    }

    public static void updateName(String newName, String IP) throws ClassNotFoundException, SQLException, NamingException {

        stat = conn.createStatement();

        int n = stat.executeUpdate("UPDATE freeUsers SET name = " + "'" + newName + "'" +
                "WHERE userIp =" + "'" + IP + "'");

        stat.close();
    }
}