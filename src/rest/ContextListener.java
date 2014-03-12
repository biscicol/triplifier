package rest;

import dbmap.DBsystem;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Level;

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
        for (DBsystem dbs : DBsystem.values()) {
            try {
                System.out.println("Loading JBC driver: " + dbs.name());
                Class.forName(dbs.driver);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        // Set the log level so that D2RQ doesn't produce DEBUG messages.
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
    }

    /**
     * Does nothing; only included to meet the interface requirements.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}