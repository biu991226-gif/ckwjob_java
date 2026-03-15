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

public class Jobs extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 一覧検索で使うキーワードと session 情報を取得する。
        String keyword = trim(request.getParameter("keyword"));
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty();
        StringBuilder jobsJson = new StringBuilder();
        StringBuilder appliedJson = new StringBuilder();
        String dbError = "";

        try (Connection conn = DbUtil.getConnection()) {
            if (keyword.isEmpty()) {
                String sql = "SELECT id, title, salary, area, status, created_at FROM jobs ORDER BY created_at DESC LIMIT 50";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    appendJobs(rs, jobsJson);
                }
            } else {
                // キーワード検索時は部分一致でタイトルなどを絞り込む。
                String sql = "SELECT id, title, salary, area, status, created_at FROM jobs "
                    + "WHERE (title LIKE ? OR area LIKE ? OR salary LIKE ? OR description LIKE ?) "
                    + "ORDER BY created_at DESC LIMIT 50";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String likeKeyword = "%" + keyword + "%";
                    stmt.setString(1, likeKeyword);
                    stmt.setString(2, likeKeyword);
                    stmt.setString(3, likeKeyword);
                    stmt.setString(4, likeKeyword);
                    try (ResultSet rs = stmt.executeQuery()) {
                        appendJobs(rs, jobsJson);
                    }
                }
            }

            if (dbError.isEmpty() && "job_seeker".equals(currentRole) && currentUserId > 0) {
                // 一覧表示用に、当該ユーザーの応募済み求人IDを返す。
                String appSql = "SELECT job_id FROM applications WHERE job_seeker_user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(appSql)) {
                    stmt.setInt(1, currentUserId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                appliedJson.append(",");
                            }
                            appliedJson.append(String.valueOf(rs.getInt("job_id")));
                            first = false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dbError = "求人一覧の取得に失敗しました。";
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"keyword\":\"").append(jsonEscape(keyword)).append("\",")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"currentUserId\":").append(String.valueOf(currentUserId)).append(",")
            .append("\"dbError\":\"").append(jsonEscape(dbError)).append("\",")
            .append("\"jobs\":[").append(jobsJson).append("],")
            .append("\"appliedJobIds\":[").append(appliedJson).append("]")
            .append("}");
    }

    private static void appendJobs(ResultSet rs, StringBuilder jobsJson) throws SQLException {
        boolean first = true;
        while (rs.next()) {
            if (!first) {
                jobsJson.append(",");
            }
            jobsJson.append("{")
                .append("\"id\":").append(String.valueOf(rs.getInt("id"))).append(",")
                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                .append("\"salary\":\"").append(jsonEscape(rs.getString("salary"))).append("\",")
                .append("\"area\":\"").append(jsonEscape(rs.getString("area"))).append("\",")
                .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\",")
                .append("\"createdAt\":\"").append(jsonEscape(rs.getString("created_at"))).append("\"")
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
