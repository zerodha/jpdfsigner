BIN := target/jpdfsigner-1.0-SNAPSHOT.jar

$(BIN): $(shell find src/main/java/com/zerodha/jpdfsigner -type f -name "*.go" -o -name "*.java") pom.xml
	mvn package

.PHONY: run
run: $(BIN)
	java -jar $(BIN)