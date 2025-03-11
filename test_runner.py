import json
import os
import sys
from dataclasses import asdict
from runner.models.profile import TestConfig, Step, Profile, Job, TestParam, BuildSettings, CommonSettings

def load_json_file(file_path: str) -> dict:
    """Загружает JSON файл и возвращает его содержимое как словарь."""
    try:
        with open(file_path, "r", encoding="utf-8") as file:
            return json.load(file)
    except FileNotFoundError:
        print(f"Ошибка: Файл '{file_path}' не найден.")
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"Ошибка: Не удалось декодировать JSON из файла '{file_path}'.")
        sys.exit(1)

def parse_profile(data: dict) -> TestConfig:
    """Преобразует данные из JSON в объект TestConfig."""
    tests_param = [
        TestParam(
            JOB=Job(**param["JOB"]),
            PROFILE=[
                Profile(
                    SCENARIO_NAME=profile_data["SCENARIO_NAME"],
                    STEPS=[Step(**step) for step in profile_data["STEPS"]]
                )
                for profile_data in param["PROFILE"]
            ],
            PROPERTIES=param["PROPERTIES"]
        )
        for param in data["TESTS_PARAM"]
    ]

    build_settings = BuildSettings(**data["COMMON_SETTINGS"]["BUILD_SETTINGS"])
    common_settings = CommonSettings(
        BUILD_SETTINGS=build_settings,
        PROPERTIES=data["COMMON_SETTINGS"]["PROPERTIES"]
    )

    return TestConfig(TESTS_PARAM=tests_param, COMMON_SETTINGS=common_settings)

def generate_command(test_config: TestConfig) -> str:
    """Сборка команды запуска."""
    test_common_settings = json.dumps(test_config.COMMON_SETTINGS.PROPERTIES)
    percent_profile = test_config.COMMON_SETTINGS.BUILD_SETTINGS.PERCENT_PROFILE / 100

    commands = []
    for test_param in test_config.TESTS_PARAM:
        profiles_data = [
            {
                "SCENARIO_NAME": p.SCENARIO_NAME,
                "STEPS": [
                    {
                        **asdict(step),
                        "STAR_TPS": step.STAR_TPS * percent_profile,
                        "END_TPS": step.END_TPS * percent_profile
                    }
                    for step in p.STEPS
                ]
            }
            for p in test_param.PROFILE
        ]

        tests_profile = json.dumps(profiles_data)
        test_settings = json.dumps(test_param.PROPERTIES)

        command = (
            f'mvn gatling:test '
            f'-Dgatling.simulationClass={test_param.JOB.SIMULATION_CLASS} '
            f'-DCOMMON_SETTINGS="{test_common_settings}" '
            f'-DTEST_PROFILE="{tests_profile}" '
            f'-DTEST_SETTINGS="{test_settings}" & '
        )
        commands.append(command)

    return ''.join(commands)


# Команда для остановки процесса на генераторе нагрузки
# sudo kill $(pgrep -f <scenario name>)
if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Использование: python test_runner.py <путь_к_профилю>")
        sys.exit(1)

    profile_path = sys.argv[1]
    json_data = load_json_file(profile_path)
    profile = parse_profile(json_data)
    commands = generate_command(profile)

    # Запуск тестов параллельно
    print(commands)
    os.system(commands)
