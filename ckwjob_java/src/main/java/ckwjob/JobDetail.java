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

public class JobDetail extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 指定された求人 ID と session 情報を取得する。
        int jobId = parseInt(request.getParameter("id"));
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty();
        boolean isAlreadyApplied = false;
        String dbError = "";
        StringBuilder jobJson = new StringBuilder("null");

        if (jobId <= 0) {
            dbError = "求人IDが不正です。";
        }

        if (dbError.isEmpty()) {
            try (Connection conn = DbUtil.getConnection()) {
                // 詳細画面は指定 ID を 1 件取得する。
                String sql = "SELECT id, title, salary, area, description, status, created_at FROM jobs WHERE id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, jobId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            jobJson.setLength(0);
                            jobJson.append("{")
                                .append("\"id\":").append(String.valueOf(rs.getInt("id"))).append(",")
                                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                                .append("\"salary\":\"").append(jsonEscape(rs.getString("salary"))).append("\",")
                                .append("\"area\":\"").append(jsonEscape(rs.getString("area"))).append("\",")
                                .append("\"description\":\"").append(jsonEscape(rs.getString("description"))).append("\",")
                                .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\",")
                                .append("\"createdAt\":\"").append(jsonEscape(rs.getString("created_at"))).append("\"")
                                .append("}");
                        }
                    }
                }

                if (!"null".contentEquals(jobJson) && "job_seeker".equals(currentRole) && currentUserId > 0) {
                    // 応募済み判定を行い、二重応募の画面表示を防ぐ。
                    String appSql = "SELECT id FROM applications WHERE job_id = ? AND job_seeker_user_id = ? LIMIT 1";
                    try (PreparedStatement stmt = conn.prepareStatement(appSql)) {
                        stmt.setInt(1, jobId);
                        stmt.setInt(2, currentUserId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            isAlreadyApplied = rs.next();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                dbError = "求人詳細の取得に失敗しました。";
            }
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"jobId\":").append(String.valueOf(jobId)).append(",")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"currentUserId\":").append(String.valueOf(currentUserId)).append(",")
            .append("\"isAlreadyApplied\":").append(String.valueOf(isAlreadyApplied)).append(",")
            .append("\"dbError\":\"").append(jsonEscape(dbError)).append("\",")
            .append("\"job\":").append(jobJson)
            .append("}");
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return parseInt((String) value);
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
