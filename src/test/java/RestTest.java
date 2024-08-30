import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ibs.ConfigProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RestTest {
    private final static String URL = ConfigProvider.readConfig().getString("url");
    private final static int NUMBER_PRODUCTS = 4;
    private static final Map<String, String> TYPE_MAPPING = Map.of(
            "Фрукт", "FRUIT",
            "Овощ", "VEGETABLE"
    );

    private String name;
    private String type;
    private Object isExotic;
    SessionFilter sessionFilter = new SessionFilter();

    @ParameterizedTest
    @MethodSource("getParameters")
    @DisplayName("Тест рабочего процесса для товара")
    void testFoodWorkflow(String name, String type, Object isExotic) {
        this.name = name;
        this.type = type;
        this.isExotic = isExotic;
        testGetFoodList();
        testAddFood();
        testCheckFoodExistence();
        testResetTestData();
    }

    @Disabled("Запускается только в testFoodWorkflow")
    @Test
    @DisplayName("Получение списка товаров")
    @Order(1)
    void testGetFoodList() {
        given()
                .filter(sessionFilter)
                .baseUri(URL)
                .when()
                .get("/api/food")
                .then().log().all()
                .assertThat()
                .statusCode(200)
                .body("size()", equalTo(NUMBER_PRODUCTS));
    }

    @Disabled("Запускается только в testFoodWorkflow")
    @Test
    @DisplayName("Добавление товара")
    @Order(2)
    void testAddFood() {
        given()
                .filter(sessionFilter)
                .baseUri(URL)
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"name\": \"" + name + "\",\n" +
                        "  \"type\": \"" + type + "\",\n" +
                        "  \"exotic\": " + isExotic + "\n" +
                        "}")
                .when()
                .post("/api/food")
                .then().log().all()
                .statusCode(200);
    }

    @Disabled("Запускается только в testFoodWorkflow")
    @Test
    @DisplayName("Проверка наличия товара.")
    @Order(3)
    void testCheckFoodExistence() {
        given()
                .filter(sessionFilter)
                .baseUri(URL)
                .when()
                .get("/api/food")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .body("[" + NUMBER_PRODUCTS + "].name", equalTo(name))
                .body("[" + NUMBER_PRODUCTS + "].type", equalTo(type))
                .body("[" + NUMBER_PRODUCTS + "].exotic", equalTo(isExotic));
    }

    @Disabled("Запускается только в testFoodWorkflow")
    @Test
    @DisplayName("Сброс тестовых данных")
    @Order(4)
    void testResetTestData() {
        given()
                .filter(sessionFilter)
                .baseUri(URL)
                .when()
                .post("/api/data/reset")
                .then().log().all()
                .statusCode(200);
    }

    public Stream<Arguments> getParameters() {
        Config config = ConfigFactory.load("app.conf");
        List<? extends ConfigObject> params = config.getObjectList("testData");
        return params.stream().map(param -> {
            String name = (String) param.get("name").unwrapped();
            String typeKey = Objects.requireNonNull(param.get("type").unwrapped()).toString();
            String type = TYPE_MAPPING.getOrDefault(typeKey, "Овощ");
            Object isExotic = param.get("isExotic").unwrapped();
            return Arguments.of(name, type, isExotic);
        });
    }
}
