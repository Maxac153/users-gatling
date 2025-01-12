from dataclasses import dataclass
from typing import Any

from src.models.profile.maven import Maven
from src.models.profile.properties import Properties


@dataclass
class CommonSettings:
    MAVEN: Maven
    PROPERTIES: Properties

    @staticmethod
    def from_dict(obj: Any) -> 'CommonSettings':
        _MAVEN = Maven.from_dict(obj.get("MAVEN"))
        _PROPERTIES = Properties.from_dict(obj.get("PROPERTIES"))
        return CommonSettings(_MAVEN, _PROPERTIES)

    def to_dict(self):
        return {
            "MAVEN": self.MAVEN.to_dict(),
            "PROPERTIES": self.PROPERTIES.to_dict()
        }