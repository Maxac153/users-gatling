from dataclasses import dataclass
from typing import Any


@dataclass
class Maven:
    MODULE_NAME: str
    PERCENT_PROFILE: float

    @staticmethod
    def from_dict(obj: Any) -> 'Maven':
        _MODULE_NAME = str(obj.get("MODULE_NAME"))
        _PERCENT_PROFILE = float(obj.get("PERCENT_PROFILE"))
        return Maven(_MODULE_NAME, _PERCENT_PROFILE)

    def to_dict(self):
        return self.__dict__