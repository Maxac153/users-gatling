from dataclasses import dataclass
from typing import Any, List

from src.models.profile.job import Job
from src.models.profile.profile import Profile
from src.models.profile.properties import Properties


@dataclass
class TestParam:
    JOB: Job
    PROFILE: List[Profile]
    PROPERTIES: Properties

    @staticmethod
    def from_dict(obj: Any) -> 'TestParam':
        _JOB = Job.from_dict(obj.get("JOB"))
        _PROFILE = [Profile.from_dict(y) for y in obj.get("PROFILE")]
        _PROPERTIES = Properties.from_dict(obj.get("PROPERTIES"))
        return TestParam(_JOB, _PROFILE, _PROPERTIES)

    def to_dict(self):
        return {
            "JOB": self.JOB.to_dict(),
            "PROFILE": [profile.to_dict() for profile in self.PROFILE],
            "PROPERTIES": self.PROPERTIES.to_dict()["PROPERTIES"]
        }