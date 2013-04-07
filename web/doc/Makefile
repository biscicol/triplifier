SRC = ../../src

doc: $(SRC)/reader/*.java $(SRC)/reader/plugins/*.java
	-javadoc -sourcepath $(SRC) -subpackages reader
	touch doc

clean:
	-rm -r *.html
	-rm resources/*
	-rm plugins/*
	-rm package-list
	-rm *.css
	-rm doc

