package rest;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Registers/unregisters JDBC drivers listed in DBsystem
 * when context is initialized/destroyed.
 * Needs an entry in web.xml.
 */
public class ContextListener implements ServletContextListener {

	/**
	 * Register supported JDBC drivers.
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		for (DBsystem dbs : DBsystem.values())
			try {
				Class.forName(dbs.driver);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}		
	}

	/**
	 * Unregister all registered JDBC drivers.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(drivers.nextElement());
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

	}
}
