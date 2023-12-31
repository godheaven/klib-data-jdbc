package cl.kanopus.jdbc.impl.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public class SQLServerEngineTest {

    @Test
    public void testPrepareSQL2Engine() {

        String result = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT count(*) FROM table");
        Assertions.assertEquals("SELECT count_big(*) FROM table", result);

        String result2 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT count(*) FROM table UNION SELECT count(*) FROM table2");
        Assertions.assertEquals("SELECT count_big(*) FROM table UNION SELECT count_big(*) FROM table2", result2);

        
        String result3 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT * FROM table WHERE field1 LIKE '%' || :param1 || '%' AND field2 LIKE '%'||:param2 LIMIT 1");
        Assertions.assertEquals("SELECT * FROM table WHERE field1 LIKE '%' + :param1 + '%' AND field2 LIKE '%'+:param2 LIMIT 1", result3);
        
        
        String result4 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT * FROM table WHERE field1::date = :param1 AND field_2::date >= :param2 AND field-3::date >= :param3");
        Assertions.assertEquals("SELECT * FROM table WHERE CAST(field1 as date) = :param1 AND CAST(field_2 as date) >= :param2 AND CAST(field-3 as date) >= :param3", result4);
        
    }

}
