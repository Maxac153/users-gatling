import http.client
import json

from src.models.gitlab.gitlab_credentials import GitLabConfig


class RunJobGitLab:
    __HEADERS = {
        'Content-Type': 'application/json'
    }

    def __init__(self, gitlab_config: GitLabConfig):
        self.job_name = gitlab_config.job_name
        self.conn = http.client.HTTPConnection(gitlab_config.gitlab_url)
        self.url = f"/api/v4/projects/{gitlab_config.project_id}/trigger/pipeline?token={gitlab_config.trigger_token}"
        self.ref = gitlab_config.ref
        self.job_name = gitlab_config.job_name

    def run_test(
            self,
            generator: str,
            module_name: str,
            gatling_script: str
    ) -> None:
        # Выполняем POST-запрос для запуска джобы
        payload = {
            'ref': self.ref,
            'variables': {
                'JOB_NAME': self.job_name,
                'GENERATOR': generator,
                'GATLING_SCRIPT': gatling_script
            }
        }

        self.conn.request("POST", self.url, body=json.dumps(payload), headers=self.__HEADERS)

        # Получаем ответ
        response = self.conn.getresponse()
        response_data = response.read()

        # Проверка ответа
        if response.status == 201:
            mn = f"| Название модуля: '{module_name}'"
            g = f"| Генератор нагрузки: '{generator}'"
            jn = f"| Job '{self.job_name}' успешно запущена"

            max_len = len(max([mn, g, jn], key=len))
            print("|" + "-" * max_len + "|")
            print(mn + " " * (max_len - len(mn)) + " |")
            print("|" + "-" * max_len + "|")
            print(g + " " * (max_len - len(g)) + " |")
            print("|" + "-" * max_len + "|")
            print(jn + " " * (max_len - len(jn)) + " |")
            print("|" + "-" * max_len + "|")
        else:
            print(f"Ошибка при запуске pipline: {response.status} - {response_data.decode()}")

    def close_connect(self) -> None:
        self.conn.close()
