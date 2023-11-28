package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.util.parser.EnumParser;
import cl.kanopus.jdbc.entity.annotation.ColumnGroup;
import cl.kanopus.jdbc.entity.annotation.View;
import cl.kanopus.jdbc.util.parser.JsonListParser;
import cl.kanopus.jdbc.util.parser.JsonParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@View("SELECT * FROM tmp_test_data")
public class TestViewData extends Mapping {

    @Column(name = "pk_test_data", serial = true)
    private long id;

    @Column(name = "td_system_id")
    private long systemId;

    @Column(name = "td_login_id", length = 10)
    private String loginId;

    @Column(name = "td_date")
    private Date date;

    @Column(name = "td_local_date")
    private LocalDate localDate;

    @Column(name = "td_local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "td_status", parser = EnumParser.class, parserResult = Status.class)
    private Status status;

    @Column(name = "td_color_id", parser = EnumParser.class, parserResult = Color.class)
    private Color color;

    @Column(name = "td_color_json", parser = JsonListParser.class, parserResult = Color.class)
    private List<Color> colors;

    @Column(name = "td_data_json", parser = JsonParser.class, parserResult = JsonData.class)
    private JsonData json;

    @Column(name = "td_list_json", parser = JsonListParser.class, parserResult = JsonData.class)
    private List<JsonData> jsonList;

    @ColumnGroup(result = TestDataGroup.class, nullable = true)
    private TestDataGroup group;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSystemId() {
        return systemId;
    }

    public void setSystemId(long systemId) {
        this.systemId = systemId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public List<Color> getColors() {
        return colors;
    }

    public void setColors(List<Color> colors) {
        this.colors = colors;
    }

    public JsonData getJson() {
        return json;
    }

    public void setJson(JsonData json) {
        this.json = json;
    }

    public List<JsonData> getJsonList() {
        return jsonList;
    }

    public void setJsonList(List<JsonData> jsonList) {
        this.jsonList = jsonList;
    }

    public TestDataGroup getGroup() {
        return group;
    }

    public void setGroup(TestDataGroup group) {
        this.group = group;
    }

    public static class TestDataGroup {

        @Column(name = "td_text", length = 100, auditable = false)
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

    }

    public static class JsonData {

        private int id;
        private String text;
        private Date date;
        private boolean enabled;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }
}
