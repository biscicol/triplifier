## Building the Triplifier ##

Running "ant", "ant all", or "ant war" will compile the Triplifier and build a web
archive (WAR file) suitable for deployment on an application server such as Apache
Tomcat or GlassFish.  The web archive file can be found at ./dist/triplifier.war.

The complete list of build targets is as follows.
	all      Builds all targets.
	clean    Deletes the build directory and its contents.
	compile  Compiles the Triplifier without building a Web archive.
	help     Displays detailed information about building the Triplifier.
	war      Builds the Triplifier Web archive for deployment.


## Deploying the Triplifier ##

Deploy the WAR archive as you would any other Java Web application.  After
deployment you will need to copy the JDBC driver JAR libraries from ./lib to the
shared libraries location of your application server installation.  For Tomcat, for
example, you would copy them to $CATALINA_HOME/lib.

The JDBC drivers are not included in the WEB-INF/lib directory of the WAR archive
because we ran into severe performance problems with JDBC after "hot"
re-deployments on a running server when the drivers were loaded this way.  This is
not an issue when the JDBC drivers are loaded from the common libraries location.

The following driver files need to be copied.

sudo cp ./lib/mysql-connector-java-*-bin.jar $CATALINA_HOME/lib
sudo cp ./lib/ojdbc6.jar 		$CATALINA_HOME/lib
sudo cp ./lib/postgresql-*.jdbc4.jar 	$CATALINA_HOME/lib
sudo cp ./lib/sqlite-jdbc-*.jar		$CATALINA_HOME/lib
sudo cp ./lib/sqljdbc4.jar		$CATALINA_HOME/lib

OR, for glassfish:

export $GLASSFISH/domain1/applications/triplifier/WEB-INF/lib
sudo cp ./lib/mysql-connector-java-*-bin.jar $TOUTLIB
sudo cp ./lib/ojdbc6.jar $TOUTLIB
sudo cp ./lib/postgresql-*.jdbc4.jar $TOUTLIB
sudo cp ./lib/sqlite-jdbc-*.jar $TOUTLIB
sudo cp ./lib/sqljdbc4.jar $TOUTLIB;

Once finished, then the servlet container will need to be restarted...

## Running the command-line triplifier ##
svn checkout http://triplifier.googlecode.com/svn/trunk/ triplifier-read-only
cd triplifier-read-only/
mkdir dist
ant
java -classpath .:./out/production/triplifier/:./lib/* triplify -h
java -classpath .:./out/production/triplifier/:./lib/* triplify myDarwinCoreArchive.zip
