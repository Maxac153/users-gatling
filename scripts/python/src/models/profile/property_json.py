from dataclasses import dataclass
from typing import List, Any

from src.models.profile.common_settings import CommonSettings
from src.models.profile.test_param import TestParam


@dataclass
class PropertyJson:
    TESTS_PARAM: List[TestParam]
    COMMON_SETTINGS: CommonSettings

    @staticmethod
    def from_dict(obj: Any) -> 'PropertyJson':
        _TESTS_PARAM = [TestParam.from_dict(y) for y in obj.get("TESTS_PARAM")]
        _COMMON_SETTINGS = CommonSettings.from_dict(obj.get("COMMON_SETTINGS"))
        return PropertyJson(_TESTS_PARAM, _COMMON_SETTINGS)

    def to_dict(self):
        return {
            "TESTS_PARAM": [test_param.to_dict() for test_param in self.TESTS_PARAM],
            "COMMON_SETTINGS": self.COMMON_SETTINGS.to_dict()
        }
