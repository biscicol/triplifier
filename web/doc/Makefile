SRC = ../../src

doc: $(SRC)/plugins/*.java $(SRC)/ReaderManager.java
	-javadoc -sourcepath $(SRC) plugins $(SRC)/ReaderManager.java $(SRC)/TabularDataConverter.java
	touch doc

clean:
	-rm -r *.html
	-rm resources/*
	-rm package-list
	-rm *.css
	-rm doc

