import java.sql.ResultSet;

/**
 * User: wangchongyang on 2017/6/6 0006.
 */
public class AlarmSendRecordUpgrade {
    public static void main(String[] args) {
        final String queryResNameNullAlarmSendRecordSQL = "SELECT ufm.c_id,de.c_res_name,de.c_res_type_name,de.c_res_ip,de.c_event_name FROM t_util_forward_message ufm LEFT JOIN t_data_event de ON ufm.c_business_id = de.c_id WHERE de.c_id IS NOT NULL AND ufm.c_business_type = 'EventCenter' AND ufm.c_res_name IS NULL UNION ALL SELECT ufm1.c_id,deh.c_res_name,deh.c_res_type_name,deh.c_res_ip,deh.c_event_name FROM t_util_forward_message ufm1 LEFT JOIN t_data_event_his deh ON ufm1.c_business_id = deh.c_id WHERE deh.c_id IS NOT NULL AND ufm1.c_business_type = 'EventCenter' AND ufm1.c_res_name IS NULL;";
        try {
            System.out.println("Query resName is null of t_util_forward_message ...");
            System.out.println("------------------START------------------");
            final ResultSet resultSet = DBUtil.getInstance(args[0]).execQuery(queryResNameNullAlarmSendRecordSQL);
            System.out.println("-------------------END-------------------");
            resultSet.last();
            System.out.printf("resName is null of t_util_forward_message :%d Rows%n", resultSet.getRow());
            resultSet.beforeFirst();
            while (resultSet.next()) {
                String id = resultSet.getString("c_id");
                String resName = resultSet.getString("c_res_name");
                String resTypeName = resultSet.getString("c_res_type_name");
                String resIp = resultSet.getString("c_res_ip");
                String eventName = resultSet.getString("c_event_name");
                final String update4FieldSQL = String.format("UPDATE t_util_forward_message SET c_res_name='%s',c_res_ip='%s',c_send_content='%s',c_res_type_name='%s' WHERE c_id='%s';", resName, resIp, eventName, resTypeName, id);
                DBUtil.getInstance(args[0]).execUpdate(update4FieldSQL);
                System.out.println("EXECUTE update c_res_name,c_res_ip,c_send_content,c_res_type_name OK:" + id);
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        } finally {
            try {
                DBUtil.getInstance(args[0]).close();
            } catch (Exception e) {
                System.out.println(ExceptionUtils.getStackTrace(e));
            }
        }
    }
}
