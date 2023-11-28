package cl.kanopus.jdbc.util;

import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestViewData;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@ExtendWith(SpringExtension.class)
public class JdbcCacheTest {

    @Test
    public void validateTranslationMap() {
        Map<String, String> cache = JdbcCache.translationMap(TestData.class);

        Assertions.assertTrue(cache.containsKey("id"));
        Assertions.assertTrue(cache.containsKey("systemId"));
        Assertions.assertTrue(cache.containsKey("loginId"));
        Assertions.assertTrue(cache.containsKey("group.text"));
        Assertions.assertTrue(cache.containsKey("status"));
        Assertions.assertTrue(cache.containsKey("type.id"));
        Assertions.assertTrue(cache.containsKey("type.name"));
    }

    @Test
    public void validateSqlBaseLoadAll() throws SQLException {
        String sql = JdbcCache.sqlBase(TestData.class, true);
        Assertions.assertNotNull(sql);
        Assertions.assertEquals("SELECT t1.pk_test_data, t1.td_system_id, t1.td_login_id, t1.td_date, t1.td_local_date, t1.td_local_date_time, t1.td_status, t1.td_color_id, t1.td_color_json, t1.td_data_json, t1.td_list_json, t2.pk_test_type, t2.name AS tt_name, t1.td_text FROM tmp_test_data t1 INNER JOIN tmp_test_type t2 ON t1.fk_test_type=t2.pk_test_type", sql);
    }

    @Test
    public void validateSqlBaseLazy() throws SQLException {
        String sqlLazy = JdbcCache.sqlBase(TestData.class, false);
        Assertions.assertNotNull(sqlLazy);
        Assertions.assertEquals("SELECT t1.pk_test_data, t1.td_system_id, t1.td_login_id, t1.td_date, t1.td_local_date, t1.td_local_date_time, t1.td_status, t1.td_color_id, t1.td_color_json, t1.td_data_json, t1.td_list_json, t1.td_text FROM tmp_test_data t1", sqlLazy);
    }

    @Test
    public void validateSqlBaseView() throws SQLException {
        String sql = JdbcCache.sqlBase(TestViewData.class);
        Assertions.assertNotNull(sql);
        Assertions.assertEquals("SELECT * FROM tmp_test_data", sql);
    }

}
