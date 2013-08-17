SRC = ../../src

doc: $(SRC)/reader/*.java $(SRC)/reader/plugins/*.java $(SRC)/rest/*.java
	-javadoc -classpath .:../../lib/* -sourcepath $(SRC) rest -subpackages reader
	touch doc

clean:
	-rm -r *.html
	-rm resources/*
	-rm plugins/*
	-rm package-list
	-rm *.css
	-rm doc

