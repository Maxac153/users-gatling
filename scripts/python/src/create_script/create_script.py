import json
from typing import Dict

from src.models.profile.property_json import PropertyJson


class Script:
    def __init__(self, profile: PropertyJson):
        self._percent_profile = profile.COMMON_SETTINGS.MAVEN.PERCENT_PROFILE
        self._common_settings = json.dumps(profile.COMMON_SETTINGS.PROPERTIES.to_dict())
        self._profile = profile

    def get_gatling_script_dict(self) -> Dict[str, str]:
        gatling_scripts = {}

        for test in self._profile.TESTS_PARAM:
            generator = test.JOB.GENERATOR

            for profile in test.PROFILE:
                for step in profile.STEPS:
                    step.STAR_TPS *= self._percent_profile / 100
                    step.END_TPS *= self._percent_profile / 100

            script = (
                f"mvn gatling:test -Dgatling.simulationClass={test.JOB.TEST_PATH}.{test.JOB.TEST_NAME} "
                f"-DCOMMON_SETTINGS='{self._common_settings}' "
                f"-DTEST_SETTINGS='{json.dumps(test.to_dict())}' & "
            )

            if generator in gatling_scripts:
                gatling_scripts[generator] += script
            else:
                gatling_scripts[generator] = script

        for key in gatling_scripts:
            gatling_scripts[key] += "wait"

        return gatling_scripts
