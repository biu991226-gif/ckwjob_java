package ckwjob;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbUtil {
    // ローカル開発で使う DB 接続情報をまとめる。
    private static final String URL = "jdbc:mysql://localhost:3306/mzx991226_zhixin?useSSL=false&serverTimezone=Asia/Tokyo&characterEncoding=utf8";
    private static final String USER = "mzx991226_zhixin";
    private static final String PASSWORD = "mzx991226";

    static {
        try {
            // 自動登録が効かない場合に備えて、MySQL Driver を明示的に読み込む。
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC Driver の読み込みに失敗しました。", e);
        }
    }

    private DbUtil() {
    }

    public static Connection getConnection() throws SQLException {
        // 接続処理はここに寄せて、各 Servlet で重複しないようにする。
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
