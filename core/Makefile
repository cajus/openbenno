all:
	mvn validate
	mvn compile
	mvn test
	mvn package

install:
	mvn install
	[ ! -d ../build ] || rm -rf ../build
	mkdir ../build
	cp -a target/package/all-in-one/* ../build
	mv ../build/bennocore*.jar ../build/bennocore.jar
	echo -e "#!/bin/sh\n\njava -jar bennocore.jar -p 2500 -r /srv/mailarchive/repo -i /srv/mailarchive/index\n" >> ../build/run.sh
	chmod +x ../build/run.sh

clean:
	rm -rf target
	rm -rf ../build
