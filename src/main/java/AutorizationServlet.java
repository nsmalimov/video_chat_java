import Databases.SQLiteClass;
import org.json.JSONObject;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.UUID;

public class AutorizationServlet extends HttpServlet {

    public static String ipPrepare(String ip) {
        String[] partsIP = ip.split("\\.");

        String newIP = "";

        boolean first = true;

        for (int i = 0; i < partsIP.length - 1; i++) {
            if (first) {
                newIP = newIP + partsIP[i];
                first = false;
            } else {
                newIP = newIP + "." + partsIP[i];
            }
        }
        return newIP;
    }

    //TODO проверка по ip
    public static boolean checkIp(String ip) throws ClassNotFoundException, SQLException, NamingException {
        boolean checkIp = false;
        boolean answerCheck = false;

        String lastIp = ipPrepare(ip);

        checkIp = SQLiteClass.checkIP(lastIp);

        if (!checkIp) {
            answerCheck = SQLiteClass.checkIP(ip);
        } else {
            return true;
        }

        return answerCheck;
    }

    public static boolean checkKeyGen(String name, String key, String ip) throws ClassNotFoundException, SQLException, NamingException {

        boolean answer = SQLiteClass.checkKeyGenDb(key);

        if (answer) {
            //запись в базу данных
            SQLiteClass.addUserDatabase(name, key, ip);
            SQLiteClass.addUserIP(ip);
        }

        return answer;
    }

    public static String checkCookies(HttpServletRequest request) throws ClassNotFoundException, SQLException, NamingException {
        Cookie[] cookies = null;
        cookies = request.getCookies();

        String userName = "";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userKey".equals(cookie.getName())) {

                    userName = SQLiteClass.getNameDb(cookie.getValue());

                    return userName;
                }
            }
            return userName;
        }
        return "";
    }

    public static String getUserKey(HttpServletRequest request) throws ClassNotFoundException, SQLException, NamingException {
        Cookie[] cookies = null;
        cookies = request.getCookies();

        String userName = "";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userKey".equals(cookie.getName())) {

                    userName = SQLiteClass.getNameDb(cookie.getValue());

                    return userName;
                }
            }
            return userName;
        }
        return "";
    }

    public static String getKeyGenCook(HttpServletRequest request) throws ClassNotFoundException, SQLException, NamingException {
        Cookie[] cookies = null;
        cookies = request.getCookies();

        String userName = "";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userKey".equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
            return userName;
        }
        return "";
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //регистрация нового пользователя
        StringBuilder jb = new StringBuilder();
        String line = null;

        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) {
        }

        try {

            SQLiteClass.Conn();
            JSONObject jsonObject = new JSONObject(jb.toString());

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            int command = jsonObject.getInt("command");

            //проверяем правильность куки

            switch (command) {
                case 0:  //авторизация

                    String userNameCookies = checkCookies(request);

                    String ip = jsonObject.getString("ip");

                    boolean checkIp = true;

                    //ip верен cooki верны
                    if (checkIp && !userNameCookies.equals("")) {
                        JSONObject jsonToReturn = new JSONObject();

                        String controlsum = ControlSum.createControlSum(ip);
                        jsonToReturn.put("ctrlsum", controlsum);

                        jsonToReturn.put("answer", "ok");
                        jsonToReturn.put("name", userNameCookies);
                        out.println(jsonToReturn.toString());
                    }

                    //ip верен cooki не верны или отсутствуют
                    if (checkIp && userNameCookies.equals("")) {
                        JSONObject jsonToReturn = new JSONObject();
                        jsonToReturn.put("answer", "name");
                        out.println(jsonToReturn.toString());
                    }

                    //ip не правильный, но куки пришли
                    //нужно куки проверить
                    //динамический ip?
                    if (!checkIp && !userNameCookies.equals("")) {
                        //добавить ip в базу данных

                        SQLiteClass.addUserIP(ip);

                        String key = getKeyGenCook(request);

                        SQLiteClass.updateIP(key, ip);

                        //обновить ip?

                        JSONObject jsonToReturn = new JSONObject();

                        String controlsum = ControlSum.createControlSum(ip);
                        jsonToReturn.put("ctrlsum", controlsum);

                        jsonToReturn.put("answer", "ok");
                        jsonToReturn.put("name", userNameCookies);
                        out.println(jsonToReturn.toString());
                    }

                    //отправить форму регистрации
                    if (!checkIp && userNameCookies.equals("")) {
                        JSONObject jsonToReturn = new JSONObject();
                        jsonToReturn.put("answer", "ip");
                        out.println(jsonToReturn.toString());
                    }

                    SQLiteClass.CloseDB();
                    break;

                case 2: //имя
                    String name = jsonObject.getString("name");

                    String userIp = jsonObject.getString("ip");

                    String uuid = UUID.randomUUID().toString();

                    String answer = SQLiteClass.addUser(name, uuid, userIp); //simple add user

                    if (!answer.equals("added")) {
                        JSONObject jsonToReturn = new JSONObject();
                        jsonToReturn.put("answer", "error");
                        jsonToReturn.put("name", answer);
                        out.println(jsonToReturn.toString());
                    } else {
                        JSONObject jsonToReturn = new JSONObject();

                        String controlsum = ControlSum.createControlSum(userIp);
                        jsonToReturn.put("ctrlsum", controlsum);

                        jsonToReturn.put("answer", "ok");
                        jsonToReturn.put("name", name);
                        out.println(jsonToReturn.toString());
                    }

                    Cookie userKeyCook = new Cookie("userKey", uuid);
                    userKeyCook.setMaxAge(60 * 60 * 24 * 5);
                    response.addCookie(userKeyCook);

                    SQLiteClass.CloseDB();
                    break;

                case 1: //по имени и ключу

                    String userName = (String) jsonObject.get("name");
                    String keyGen = (String) jsonObject.get("keyGen");

                    //полный ip
                    String userIP = (String) jsonObject.get("ip");
                    boolean isOk = checkKeyGen(userName, keyGen, userIP);

                    //если всё нормально, то отправить куки
                    if (isOk) {
                        JSONObject jsonToReturn1 = new JSONObject();

                        String controlsum = ControlSum.createControlSum(userIP);
                        jsonToReturn1.put("ctrlsum", controlsum);

                        jsonToReturn1.put("answer", "ok");
                        jsonToReturn1.put("name", userName);
                        out.println(jsonToReturn1.toString());

                        Cookie userKeyCook1 = new Cookie("userKey", keyGen);
                        userKeyCook1.setMaxAge(60 * 60 * 24 * 5);
                        response.addCookie(userKeyCook1);
                    } else {
                        //ошибка или не правильный ключ
                        JSONObject jsonToReturn1 = new JSONObject();
                        jsonToReturn1.put("answer", "wrong");
                        out.println(jsonToReturn1.toString());
                    }

                    SQLiteClass.CloseDB();
                    break;
                default:
                    SQLiteClass.CloseDB();
                    break;

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
