JAVA := $(shell which java)
JAVA_FILES := $(shell find . -name "*.java" -path "*/src/*" -type f)

.PHONY: all
all: build

.PHONY: build
build:
	mvn package -DskipTests

.PHONY: test
test:
	mvn test

.PHONY: check
check:
	mvn verify

.PHONY: format
format:
	google-java-format -i $(JAVA_FILES)

.PHONY: clean
clean:
	mvn clean

.PHONY: run
run:
	mvn -pl operaton-bpm-extension-example spring-boot:run

.PHONY: start
start:
	mvn install -DskipTests
	mvn -pl operaton-bpm-extension-example spring-boot:run
