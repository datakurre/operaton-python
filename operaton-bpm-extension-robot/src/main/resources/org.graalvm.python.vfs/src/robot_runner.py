from robot import running
from robot import result
from robot.libraries import BuiltIn
from typing import Set

import json

# Update Output Variable
#     [Arguments]    ${name}    ${value}    ${scope}=global
#     VAR    ${${name}}    ${value}    scope=${scope}

WORK_ITEM = {}

OUTPUT_MAPPING = {}

if "variables" in locals():
    WORK_ITEM.update(json.loads(variables.toString()))


class InputVariablesListener(ListenerV3):
    initial_global_variables: Set[str]

    def start_suite(self, data: running.TestSuite, result: result.TestSuite):
        builtin = BuiltIn.BuiltIn()
        self.initial_global_variables = set(builtin.get_variables().keys())
        for k, v in WORK_ITEM.items():
            builtin.set_global_variable(f"${{{k}}}", v)

    def end_suite(self, data: running.TestSuite, result: result.TestSuite):
        builtin = BuiltIn.BuiltIn()
        all_variables = builtin.get_variables()
        if result.passed:
            for k in set(all_variables) - self.initial_global_variables:
                OUTPUT_MAPPING[k] = all_variables[k]


model = get_model(StringIO(locals()[".robot"]))
suite = TestSuite.from_model(model)
suite.run(output=None, listener=InputVariablesListener())


S(json.dumps(OUTPUT_MAPPING))
