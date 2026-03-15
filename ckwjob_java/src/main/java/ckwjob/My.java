package ckwjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class My extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // session から現在ユーザー情報を取得する。
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        String error = "";
        StringBuilder historyJson = new StringBuilder();

        if (!isLoggedIn) {
            error = "個人ページを表示するにはログインしてください。";
        } else if (!"job_seeker".equals(currentRole)) {
            error = "このページは求職者アカウント専用です。";
        } else {
            try (Connection conn = DbUtil.getConnection()) {
                // 応募履歴を新しい順で返す。
                String sql = "SELECT j.id AS job_id, j.title, a.applied_at, a.status "
                    + "FROM applications a INNER JOIN jobs j ON a.job_id = j.id "
                    + "WHERE a.job_seeker_user_id = ? ORDER BY a.applied_at DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, currentUserId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        appendHistory(rs, historyJson);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "応募履歴の取得に失敗しました。";
            }
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"currentUserId\":").append(String.valueOf(currentUserId)).append(",")
            .append("\"error\":\"").append(jsonEscape(error)).append("\",")
            .append("\"history\":[").append(historyJson).append("]")
            .append("}");
    }

    private static void appendHistory(ResultSet rs, StringBuilder historyJson) throws SQLException {
        boolean first = true;
        while (rs.next()) {
            if (!first) {
                historyJson.append(",");
            }
            historyJson.append("{")
                .append("\"jobId\":").append(String.valueOf(rs.getInt("job_id"))).append(",")
                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                .append("\"appliedAt\":\"").append(jsonEscape(rs.getString("applied_at"))).append("\",")
                .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\"")
                .append("}");
            first = false;
        }
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
