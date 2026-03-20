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

public class Apply extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 応募対象の求人 ID と session 情報を取得する。
        int jobId = parseInt(request.getParameter("job_id"));
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        String message = "";
        String error = "";
        String jobTitle = "";

        if (jobId <= 0) {
            error = "求人IDが不正です。";
        } else if (!isLoggedIn) {
            error = "応募するにはログインが必要です。";
        } else if (!"job_seeker".equals(currentRole)) {
            error = "企業アカウントでは応募できません。";
        } else {
            try (Connection conn = DbUtil.getConnection()) {
                // 応募前に求人の存在と公開状態を確認する。
                String jobSql = "SELECT title, status FROM jobs WHERE id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(jobSql)) {
                    stmt.setInt(1, jobId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            error = "指定された求人は存在しません。";
                        } else if (!"open".equals(trim(rs.getString("status")))) {
                            error = "この求人は現在応募できません。";
                        } else {
                            jobTitle = trim(rs.getString("title"));
                        }
                    }
                }

                if (error.isEmpty()) {
                    String insertSql = "INSERT INTO applications (job_id, job_seeker_user_id, status) VALUES (?, ?, 'applied')";
                    try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        stmt.setInt(1, jobId);
                        stmt.setInt(2, currentUserId);
                        stmt.executeUpdate();
                        message = "「" + jobTitle + "」に応募しました。";
                    } catch (SQLException e) {
                        if (e.getErrorCode() == 1062) {
                            error = "この求人には既に応募済みです。";
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "応募処理に失敗しました。";
            }
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"success\":").append(String.valueOf(error.isEmpty())).append(",")
            .append("\"message\":\"").append(jsonEscape(message)).append("\",")
            .append("\"error\":\"").append(jsonEscape(error)).append("\",")
            .append("\"jobTitle\":\"").append(jsonEscape(jobTitle)).append("\"")
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
