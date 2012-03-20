all:
	mvn validate
	mvn compile
	mvn test
	mvn package

install:
	mvn install
	cp target/bennoadmin-*.jar ../build/bennoadmin.jar

clean:
	rm -rf target
