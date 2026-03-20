package ckwjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AddJob extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 初期表示用として、現在のログイン状態だけ返す。
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        response.setContentType("application/json; charset=UTF-8");
        response.getWriter()
            .append("{")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"currentUserId\":").append(String.valueOf(currentUserId))
            .append("}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // フォーム入力と session 情報を受け取る。
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        String title = trim(request.getParameter("title"));
        String salary = trim(request.getParameter("salary"));
        String area = trim(request.getParameter("area"));
        String description = trim(request.getParameter("description"));
        String message = "";
        String error = "";

        if (!isLoggedIn) {
            error = "求人登録を利用するにはログインしてください。";
        } else if (!"company".equals(currentRole)) {
            error = "求人登録は企業アカウントのみ利用できます。";
        } else if (title.isEmpty() || salary.isEmpty() || area.isEmpty() || description.isEmpty()) {
            error = "必須項目をすべて入力してください。";
        } else {
            try (Connection conn = DbUtil.getConnection()) {
                // 必須項目確認後、求人を新規登録する。
                String sql = "INSERT INTO jobs (company_user_id, title, salary, area, description, status) VALUES (?, ?, ?, ?, ?, 'open')";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, currentUserId);
                    stmt.setString(2, title);
                    stmt.setString(3, salary);
                    stmt.setString(4, area);
                    stmt.setString(5, description);
                    stmt.executeUpdate();
                    message = "求人を登録しました。";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "求人登録に失敗しました。";
            }
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"success\":").append(String.valueOf(error.isEmpty())).append(",")
            .append("\"message\":\"").append(jsonEscape(message)).append("\",")
            .append("\"error\":\"").append(jsonEscape(error)).append("\"")
            .append("}");
    }

    private static int toInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
