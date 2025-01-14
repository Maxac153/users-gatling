import json
import sys

from src.create_script.create_script import Script
from src.models.gitlab.gitlab_credentials import GitLabConfig
from src.models.profile.property_json import PropertyJson
from src.run_jobs.run_job_gitlab import RunJobGitLab

# Пример команды для запуска скрипта
# python3 ./main.py resources/profiles/users/users_profile.json resources/profiles/users/users_profile.json
if __name__ == "__main__":
    if len(sys.argv) > 1:
        # Config gitlab
        gitlab_config = GitLabConfig('scripts/resources/gitlab_config.json')

        # Считываем из системы какие модули надо запустить
        profile_paths = sys.argv[1:]
        for profile_path in profile_paths:
            # Загрузка данных JSON профиля из файла
            with open(profile_path, 'r') as file:
                profile_json = json.load(file)

            # Преобразование Json в экземпляр класса
            profile = PropertyJson.from_dict(profile_json)

            # Создаем соединение
            run_job = RunJobGitLab(gitlab_config)

            # Подготавливаем скрипт для запуска тестов
            script = Script(profile)
            gatling_scripts = script.get_gatling_script_dict()

            # Запрос на запуск pipline
            for generator in gatling_scripts:
                run_job.run_test(
                    generator,
                    profile.COMMON_SETTINGS.MAVEN.MODULE_NAME,
                    gatling_scripts[generator]
                )

            # Закрываем соединение
            run_job.close_connect()
