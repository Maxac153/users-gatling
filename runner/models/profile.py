from typing import List, Dict
from dataclasses import dataclass

# Определяем классы для вложенных структур
@dataclass
class Step:
    STAR_TPS: float
    END_TPS: float
    RAMP_TIME: float
    HOLD_TIME: float


@dataclass
class Profile:
    SCENARIO_NAME: str
    STEPS: List[Step]


@dataclass
class Job:
    GENERATOR: str
    SIMULATION_CLASS: str


@dataclass
class TestParam:
    JOB: Job
    PROFILE: List[Profile]
    PROPERTIES: Dict[str, str]


@dataclass
class BuildSettings:
    MODULE_NAME: str
    PERCENT_PROFILE: float


@dataclass
class CommonSettings:
    BUILD_SETTINGS: BuildSettings
    PROPERTIES: Dict[str, str]


@dataclass
class TestConfig:
    TESTS_PARAM: List[TestParam]
    COMMON_SETTINGS: CommonSettings
