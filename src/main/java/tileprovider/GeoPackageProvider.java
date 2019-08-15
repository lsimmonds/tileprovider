package tileprovider;

import android.content.Context;
import android.support.annotation.NonNull;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageCoreImpl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by joshua.johnson on 4/18/2019.
 * This class maintains a list of open GeoPackages and serves them to requesting objects.  Closed GeoPackages are "re-opened" (actually, replaced).
 * Therefore GeoPackages should not saved to memory and used in other objects without checking the open condition of the GeoPackage.
 *
 * The main reason for checking the open condition of a GeoPackage is that two separate GeoPackages to the same database will use two separate connections,
 * but closing one connection closes the other connection as well.  This class serves to insure an open connection is available at all times.
 *
 * This class uses a Singleton Model since there is only one database to maintain connections to.
 */

public class GeoPackageProvider {

    //Singleton
    private static GeoPackageProvider instance = null;

    public GeoPackageManager getGeoPackageManager() {
        return geoPackageManager;
    }

    /**
     * Singleton accessor
     * @return instance
     */
    public static GeoPackageProvider getInstance() {
        if (instance == null) {
            instance = new GeoPackageProvider();
        }

        return instance;
    }

    /**
     * Mapping of Database Name to related open GeoPackage (SQLite Database)
     */
    private Map<String, GeoPackage> geoPackageMap;

    /**
     * Library GeoPackage accessor
     */
    private GeoPackageManager geoPackageManager = null;

    /**
     * Constructor - Private to prevent instantiation (Singleton Model).  Use static method getInstance() instead.
     */
    private GeoPackageProvider() {
        if (geoPackageManager == null) {
            geoPackageManager = new GeoPackageManager();
        }
        geoPackageMap = new ConcurrentHashMap<>();
    }

    /**
     * Returns a GeoPackage associated to the given database name.  Registers to map if not previously registered.  Reopens (replaces)
     * GeoPackage if it had been previously closed.
     * @param databaseName - Database name as registered to GeoPackageManager
     * @return an open GeoPackage if found or Null if database is not registered to GeoPackageManager
     */
    public GeoPackage getGeoPackage(String databaseName) {
        GeoPackage geoPackage = geoPackageMap.get(databaseName);

        //If GeoPackage doesn't exist or is no longer open
        if (geoPackage == null ||
                !geoPackage.getConnection().getConnectionSource().isOpen(databaseName)) { //Nested methods never null
            //Open new GeoPackage and register it
            geoPackage = openGeoPackage(databaseName);
        }

        return geoPackage;
    }

    /**
     * Closes all GeoPackages currently opened through this provider (but not opened directly through
     * GeoPackageManager)
     */
    public void closeAll() {
        closeGeoPackages(geoPackageMap.keySet());
    }

    public void closeGeoPackages(Collection<String> databaseNames) {
        closeGeoPackages(databaseNames.toArray(new String[0]));
    }

    public void closeGeoPackages(String[] databaseNames) {
        for (String databaseName : databaseNames){
            closeGeoPackage(databaseName);
        }
    }

    /**
     * Close a specific GeoPackage by GeoPackageManager-registered database name
     * @param databaseName
     */
    public void closeGeoPackage(String databaseName) {
        GeoPackage geopackage = geoPackageMap.get(databaseName);
        if (geopackage != null) {
            geopackage.close();
            geoPackageMap.remove(databaseName);
        }
    }

    /**
     * opens a GeoPackage in ReadOnly mode by GeoPackageManager registered database name.
     * @param databaseName name registered in GeoPackageManager
     * @return open GeoPackage if registered or null if not
     */
    private GeoPackage openGeoPackage(String databaseName) {
        GeoPackage geoPackage = null;
        try {
            //attempt to open GeoPackage through GeoPackageManager
            geoPackage = geoPackageManager.open(databaseName, false);    //readonly
            //register to this object's map
            geoPackageMap.put(databaseName, geoPackage);
        } catch (GeoPackageException ex) {
            ex.printStackTrace();
        }

        //null if an error occurred
        return geoPackage;
    }


}
