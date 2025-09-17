package cl.kanopus.jdbc.util;

import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestViewData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@ExtendWith(SpringExtension.class)
class JdbcCacheTest {

    @Test
    void validateTranslationMap() {
        Map<String, String> cache = JdbcCache.translationMap(TestData.class);

        Assertions.assertEquals("tmp_test_data.pk_test_data", cache.get("id"));
        Assertions.assertEquals("tmp_test_data.td_system_id", cache.get("systemId"));

        Assertions.assertEquals("tmp_test_data.td_login_id", cache.get("loginId"));
        Assertions.assertEquals("tmp_test_data.td_text", cache.get("group.text"));
        Assertions.assertEquals("tmp_test_data.td_status", cache.get("status"));
        Assertions.assertEquals("tmp_test_type.pk_test_type", cache.get("type.id"));
        Assertions.assertEquals("tmp_test_type.name", cache.get("type.name"));
    }

    @Test
    void validateSqlBaseLoadAll() {
        JdbcCache.SqlBase base = JdbcCache.sqlBase(TestData.class, true);
        Assertions.assertNotNull(base.getSql());
        Assertions.assertEquals("SELECT t1.pk_test_data, t1.td_system_id, t1.td_login_id, t1.td_date, t1.td_local_date, t1.td_local_date_time, t1.td_status, t1.td_color_id, t1.td_color_json, t1.td_data_json, t1.td_list_json, t2.pk_test_type, t2.name AS tt_name, t1.td_text FROM tmp_test_data t1 INNER JOIN tmp_test_type t2 ON t1.fk_test_type=t2.pk_test_type", base.getSql());
    }

    @Test
    void validateSqlBaseLazy() {
        JdbcCache.SqlBase baseLazy = JdbcCache.sqlBase(TestData.class, false);
        Assertions.assertNotNull(baseLazy.getSql());
        Assertions.assertEquals("SELECT t1.pk_test_data, t1.td_system_id, t1.td_login_id, t1.td_date, t1.td_local_date, t1.td_local_date_time, t1.td_status, t1.td_color_id, t1.td_color_json, t1.td_data_json, t1.td_list_json, t1.td_text FROM tmp_test_data t1", baseLazy.getSql());
    }

    @Test
    void validateSqlBaseView() {
        JdbcCache.SqlBase base = JdbcCache.sqlBase(TestViewData.class);
        Assertions.assertNotNull(base.getSql());
        Assertions.assertEquals("SELECT * FROM tmp_test_data", base.getSql());
    }

}
