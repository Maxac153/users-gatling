# gatling-users

![gatling_logo.png](img/gatling_logo.png)

Пример тестового фреймворка для быстрого старта на ([Gatling](https://docs.gatling.io/)).

## Структура каталогов

![work_folder.png](img/work_folder.png)

### Структура папки common

* **common** - Общие классы для тестов;
  * **helpers** - Вспомогательные классы;
  * **models** - PoJo классы для сериализации и десериализации;
  * **steps** - Общие шаги для тестов;

### Структура модуля

* **users** - Модуль;
  * **authorization** - Тестовый сценарий модуля users;
  * **loading_avatar** - Тестовый сценарий модуля users;
    * **scenario** - Сценарии;
    * **steps** - Шаги в сценарии;
  * **registration** - Тестовый сценарий модуля users.

## Структура тестов

Пример тестового класса:

```java
public class AuthorizationAdminTest extends Simulation {
  public AuthorizationAdminTest() {
    //Загрузка Properties
    Map<String, Object> property = PropertyHelper.readProperties(
            "common/common_properties.json",
            "tests/users/authorization/authorization_admin_profile.json"
    );
    // Нагрузочный профиль
    HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getProfile(property);

    // Настройки протокола
    HttpProtocolBuilder httpProtocol = HttpDsl.http
            .baseUrl(property.get("PROTOCOL") + "://" + property.get("HOST"))
            .disableCaching()
            .userAgentHeader("Gatling/Performance Test");
    
    this.setUp(
            // Тестовый сценарий
            AuthorizationScenario.authorizationAdminScenario(property)
                    // Профиль нагрузки
                    .injectOpen(profile.get("AUTHORIZATION_ADMIN_SCENARIO"))
    ).protocols(httpProtocol);
  }
}
```

## Работа с Properties в проекте

Подробнее с диаграммой можно ознакомиться **./diagrams/property.drawio**.

![property.png](img/property.png)

Пример использования в коде:

```java
Map<String, Object> property = PropertyHelper.readProperties(
        "common/common_properties.json", // Common Property
        "common/redis_properties.json", // Common Property Redis Param
        "tests/users/authorization/authorization_user_profile.json" // Test Property
);
HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getProfile(property); // Нагрузочный профиль
```

## Правила именования

### Наименование переменных:

* Для имен переменных Java использовать **lowerCamelCase**;
* Для названия классов Java использовать **UpperCamelCase**;
* Аббревиатуры также писать CamelCase, например **SqlSelect**, а не **SQLSelect**;
* Для названия каталогов и файлов ресурсов использовать **lower_snake_case**;
* Для имен переменных (**session vars** - локальные переменные) использовать **lower_snake_case**;
* Для имен переменных (**session properties** - переменные из системы) использовать **UPPER_SNAKE_CASE**.

### Наименование сценариев и шагов:

* Использовать **lower_snake_case**;
* Названия сценария начинать с **uc_<user_case>**;
* Названия HTTP шагов начинать с **ur_<user_request>**;
* Названия запросов к Database или Redis начинать с **db_<database>**;
* Для сообщений лога использовать английский язык. Пример формата - **«Message Something Data»**.

## Запуск тестов через CLI

Пример команды запуска:

```bash
mvn gatling:test -Dgatling.simulationClass=gatling.users.authorization.AuthorizationAdminTest
```

## Запуск тестов через Gitlab CI-CD

Для запуска нагрузочных тестов используется Json.

Пример Json профиля нагрузки:

```json
{
  "TESTS_PARAM": [
    {
      "JOB": {
        "GENERATOR": "load_generator",
        "TEST_NAME": "AuthorizationAdminTest",
        "TEST_PATH": "gatling.users.authorization"
      },
      "PROFILE": [
        {
          "SCENARIO_NAME": "AUTHORIZATION_USER_SCENARIO",
          "STEPS": [
            {
              "STAR_TPS": 0.0,
              "END_TPS": 1.0,
              "RAMP_TIME": 1.0,
              "HOLD_TIME": 2.0
            },
            {
              "STAR_TPS": 0.0,
              "END_TPS": 1.0,
              "RAMP_TIME": 1.0,
              "HOLD_TIME": 2.0
            }
          ]
        }
      ],
      "PROPERTIES": {
        "GROUP": "2"
      }
    }
  ],
  "COMMON_SETTINGS": {
    "MAVEN": {
      "MODULE_NAME": "USERS",
      "PERCENT_PROFILE": 100.0
    },
    "PROPERTIES": {
      "WAIT": 10,
      "DEBUG_ENABLE": "true",
      "REDIS_KEY_READ": "users_credentials"
    }
  }
}
```

Описание параметров:

* **TESTS_PARAM** - Параметры тестов;
  * **JOB** - Параметры для Java машины;
    * **GENERATOR** - Где будет запускаться тесты;
    * **TEST_NAME** - Наименования тестового класса;
    * **TEST_FOLDER** - Путь до класса в проекте;
  * **PROFILE** - Параметры профиля нагрузки;
    * **Array profile** - Массив профилей для разных катушек;
      * **SCENARIO_NAME** - Наименование катушки;
      * **STEPS** - Шаги профиля: **STAR_TPS** - подаваемая нагрузка (с какого значения начинаем), **END_TPS** - подаваемая нагрузка (на какое значение выходим), **RAMP_TIME** - выход на заданную интенсивность (мин) и **HOLD_TIME** - удержание нагрузки (мин);
  * **PROPERTIES** - Дополнительные параметры для теста.


* **COMMON_SETTINGS** - Параметры для все тестов;
  * **MAVEN** - Параметры для bash скрипта;
    * **MODULE_NAME** - Название модуля (используется для сбора логов);
    * **PERCENT_PROFILE** - Процент от профиля;
  * **PROPERTIES** - Дополнительные общие параметры для всех тестов.