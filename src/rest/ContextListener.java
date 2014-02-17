package rest;

import dbmap.DBsystem;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Registers/unregisters JDBC drivers listed in DBsystem
 * when context is initialized/destroyed.
 * Needs an entry in web.xml.
 */
public class ContextListener implements ServletContextListener
{

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
    }

    /**
     * Does nothing; only included to meet the interface requirements.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}