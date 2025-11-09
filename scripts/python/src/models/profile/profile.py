from dataclasses import dataclass
from typing import List, Any

from src.models.profile.step import Step


@dataclass
class Profile:
    SCENARIO_NAME: str
    STEPS: List[Step]

    @staticmethod
    def from_dict(obj: Any) -> 'Profile':
        _SCENARIO_NAME = str(obj.get("SCENARIO_NAME"))
        _STEPS = [Step.from_dict(y) for y in obj.get("STEPS")]
        return Profile(_SCENARIO_NAME, _STEPS)

    def to_dict(self):
        return {
            "SCENARIO_NAME": self.SCENARIO_NAME,
            "STEPS": [step.to_dict() for step in self.STEPS]
        }
