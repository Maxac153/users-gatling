from dataclasses import dataclass
from typing import Any


@dataclass
class Step:
    STAR_TPS: float
    END_TPS: float
    RAMP_TIME: float
    HOLD_TIME: float

    @staticmethod
    def from_dict(obj: Any) -> 'Step':
        _STAR_TPS = float(obj.get("STAR_TPS"))
        _END_TPS = float(obj.get("END_TPS"))
        _RAMP_TIME = float(obj.get("RAMP_TIME"))
        _HOLD_TIME = float(obj.get("HOLD_TIME"))
        return Step(_STAR_TPS, _END_TPS, _RAMP_TIME, _HOLD_TIME)

    def to_dict(self):
        return self.__dict__