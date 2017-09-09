import com.mysql.jdbc.StringUtils;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * User: wangchongyang on 2017/4/24 0024.
 */
class DBUtil {

    private static Connection connection;
    private static PreparedStatement preparedStatement;
    private static DBUtil INSTANCE;


    private DBUtil(String jdbcConnFilePath) throws Exception {
        init(jdbcConnFilePath);
    }

    static DBUtil getInstance(String filePath) throws Exception {
        if (null == INSTANCE) {
            INSTANCE = new DBUtil(filePath);
        }
        return INSTANCE;
    }

    private static void init(String filePath) throws Exception {
        final Properties properties = new Properties();
        final FileReader fileReader = new FileReader(filePath);
        properties.load(fileReader);
//            properties.load(AlarmSendRecordUpgrade.class.getResourceAsStream("jdbc-config.properties"));
        final String driver = properties.getProperty("product.jdbc.driver");
        final String url = properties.getProperty("product.jdbc.url");
        final String username = properties.getProperty("product.jdbc.username");
        final String password = properties.getProperty("product.jdbc.password");
        if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(url) || StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password)) {
            throw new RuntimeException("driver,url,username and password must not be null or empty!");
        }
//            指定连接类型
        Class.forName(driver);
//            获取连接
        connection = DriverManager.getConnection(url, username, password);

    }

    ResultSet execQuery(String sql) throws SQLException {
        preparedStatement = connection.prepareStatement(sql);
        return preparedStatement.executeQuery(sql);
    }

    void execUpdate(String sql) throws SQLException {
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.executeUpdate(sql);
    }

    void close() throws Exception {
        if (null != preparedStatement && !preparedStatement.isClosed()) {
            preparedStatement.close();
        }

        if (null != connection && !connection.isClosed()) {
            connection.close();
        }
    }
}
