JAVA := $(shell which java)

.PHONY: all
all: build

.PHONY: build
build:
	mvn -Pnative package

.PHONY: robot
robot:
	mvn exec:exec -Dexec.executable="$(JAVA)" -Dexec.args="-cp %classpath org.operaton.bpm.extension.robot.Robot ${SUITE}"

.PHONY: src/main/resources/META-INF/native-image/org.operaton.bpm.extension.robot/operaton-bpm-extension-robot/reachability-metadata.json
src/main/resources/META-INF/native-image/org.operaton.bpm.extension.robot/operaton-bpm-extension-robot/reachability-metadata.json:
	mvn exec:exec -Dexec.executable="$(JAVA)" -Dexec.args="-agentlib:native-image-agent=caller-filter-file=$(PWD)=caller-filter.json,config-output-dir=$(PWD)/src/main/resources/META-INF/native-image/org.operaton.bpm.extension.robot/operaton-bpm-extension-robot -cp %classpath org.operaton.bpm.extension.robot.Robot $(PWD)/example"

trace-output.json:
	mvn exec:exec -Dexec.executable="$(JAVA)" -Dexec.args="-agentlib:native-image-agent=trace-output=$(PWD)/trace-file.json -cp %classpath org.operaton.bpm.extension.robot.Robot $(PWD)/example"

.PHONY: clean
clean:
	$(RM) src/main/resources/META-INF/native-image/org.operaton.bpm.extension.robot/operaton-bpm-extension-robot/reachability-metadata.json

.PHONY: shell
shell:
	nix develop

