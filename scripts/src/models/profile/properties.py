from dataclasses import dataclass
from typing import Any, Dict


@dataclass
class Properties:
    PROPERTIES: Dict[str, str]

    @staticmethod
    def from_dict(obj: Dict[str, str]) -> 'Properties':
        _PROPERTIES = obj
        return Properties(_PROPERTIES)

    def to_dict(self):
        return self.__dict__