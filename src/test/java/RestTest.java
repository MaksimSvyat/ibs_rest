import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
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

    @Test
    @DisplayName("Получение списка товаров")
    @Order(1)
    void testGetFoodList() {
        given()
                .baseUri(URL)
                .when()
                .get("/api/food")
                .then().log().all()
                .assertThat()
                .statusCode(200)
                .body("size()", equalTo(NUMBER_PRODUCTS));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    @DisplayName("Добавление товара")
    @Order(2)
    void testAddFood(String name, String type, Object isExotic) {
        given()
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

    @Test
    @DisplayName("Проверка наличия товара. В данном тесте товара №4")
    @Order(3)
    void testCheckFoodExistence() {
        given()
                .baseUri(URL)
                .when()
                .get("/api/food")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .body("[" + (NUMBER_PRODUCTS - 1) + "].name", equalTo("Яблоко"))
                .body("[" + (NUMBER_PRODUCTS - 1) + "].type", equalTo("FRUIT"))
                .body("[" + (NUMBER_PRODUCTS - 1) + "].exotic", equalTo(false));
    }

    @Test
    @DisplayName("Сброс тестовых данных")
    @Order(4)
    void testResetTestData() {
        given()
                .baseUri(URL)
                .when()
                .post("/api/data/reset")
                .then().log().all()
                .statusCode(200);
    }

    @Disabled("Не работает в локальной версии, так как после каждого запроса база обнуляется." +
            "И я бы не делал такой метод, был бы класс с отдельными методами. Это просто пример " +
            "с логикой как работало бы.")
    @ParameterizedTest
    @MethodSource("getParameters")
    @DisplayName("Тест рабочего процесса для товара")
    @Order(5)
    void testFoodWorkflow(String name, String type, Object isExotic) {
        given()
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

        given()
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

        given()
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
