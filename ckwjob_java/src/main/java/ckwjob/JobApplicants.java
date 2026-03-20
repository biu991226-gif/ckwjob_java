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

public class JobApplicants extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int jobId = parseInt(request.getParameter("id"));
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        String error = "";
        String jobTitle = "";
        StringBuilder applicantsJson = new StringBuilder();

        if (jobId <= 0) {
            error = "求人IDが不正です。";
        } else if (!isLoggedIn) {
            error = "応募者一覧を表示するにはログインしてください。";
        } else if (!"company".equals(currentRole)) {
            error = "応募者一覧は企業アカウントのみ利用できます。";
        } else {
            try (Connection conn = DbUtil.getConnection()) {
                // 他社求人の応募者一覧を見られないよう、所有者チェックを行う。
                String jobSql = "SELECT title FROM jobs WHERE id = ? AND company_user_id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(jobSql)) {
                    stmt.setInt(1, jobId);
                    stmt.setInt(2, currentUserId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            error = "対象求人が見つからないか、閲覧権限がありません。";
                        } else {
                            jobTitle = trim(rs.getString("title"));
                        }
                    }
                }

                if (error.isEmpty()) {
                    String sql = "SELECT u.name, u.email, u.phone, a.applied_at, a.status "
                        + "FROM applications a INNER JOIN users u ON a.job_seeker_user_id = u.id "
                        + "WHERE a.job_id = ? ORDER BY a.applied_at DESC";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, jobId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            boolean first = true;
                            while (rs.next()) {
                                if (!first) {
                                    applicantsJson.append(",");
                                }
                                applicantsJson.append("{")
                                    .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                                    .append("\"email\":\"").append(jsonEscape(rs.getString("email"))).append("\",")
                                    .append("\"phone\":\"").append(jsonEscape(rs.getString("phone"))).append("\",")
                                    .append("\"appliedAt\":\"").append(jsonEscape(rs.getString("applied_at"))).append("\",")
                                    .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\"")
                                    .append("}");
                                first = false;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "応募者一覧の取得に失敗しました。";
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
            .append("\"jobTitle\":\"").append(jsonEscape(jobTitle)).append("\",")
            .append("\"error\":\"").append(jsonEscape(error)).append("\",")
            .append("\"applicants\":[").append(applicantsJson).append("]")
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
