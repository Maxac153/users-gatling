from dataclasses import dataclass
from typing import Any


@dataclass
class Job:
    GENERATOR: str
    TEST_NAME: str
    TEST_PATH: str

    @staticmethod
    def from_dict(obj: Any) -> 'Job':
        _GENERATOR = str(obj.get("GENERATOR"))
        _TEST_NAME = str(obj.get("TEST_NAME"))
        _TEST_PATH = str(obj.get("TEST_PATH"))
        return Job(_GENERATOR, _TEST_NAME, _TEST_PATH)

    def to_dict(self):
        return self.__dict__
