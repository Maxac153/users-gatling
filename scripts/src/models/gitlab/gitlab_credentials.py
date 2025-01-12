import json


class GitLabConfig:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self.config = self.load_config()

    def load_config(self):
        with open(self.file_path, 'r') as file:
            return json.load(file)

    @property
    def gitlab_url(self) -> str:
        return self.config.get('GITLAB_URL')

    @property
    def project_id(self) -> str:
        return self.config.get('PROJECT_ID')

    @property
    def trigger_token(self) -> str:
        return self.config.get('TRIGGER_TOKEN')

    @property
    def ref(self) -> str:
        return self.config.get('REF')

    @property
    def job_name(self) -> str:
        return self.config.get('JOB_NAME')
