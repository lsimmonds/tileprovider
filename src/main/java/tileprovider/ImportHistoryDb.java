package tileprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Joshua.Johnson on 9/28/2018.
 * This class maintains a database storing filenames and their last modified date
 */

class ImportHistoryDb {

    private static ImportHistoryDb _instance = null;
    private ImportHistoryDbHelper mDbHelper = null;

    static ImportHistoryDb getInstance(Context context) {
        if (_instance == null) {
            _instance = new ImportHistoryDb(context);
        }

        return _instance;
    }

    //Prevent Instantiation
    private ImportHistoryDb(Context context) {
        mDbHelper = new ImportHistoryDbHelper(context);
    }

    private static class Entry implements BaseColumns {
        static final String TABLE_NAME = "entry";
        static final String COLUMN_NAME_FILEPATH = "filePath";
        static final String COLUMN_NAME_LASTMODIFIED = "lastModified";
    }

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + Entry.TABLE_NAME + " (" +
            Entry._ID + " INTEGER PRIMARY KEY," +
            Entry.COLUMN_NAME_FILEPATH + " TEXT," +
            Entry.COLUMN_NAME_LASTMODIFIED + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + Entry.TABLE_NAME;

    private static class ImportHistoryDbHelper extends SQLiteOpenHelper {
        //if schema changes, version must be incremented
        static final int DATABASE_VERSION = 2;
        static final String DATABASE_NAME = "ImportHistoryDb.db";

        ImportHistoryDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //discard and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    /**
     * Inserts new data into database
     * @param fileName
     * @param dateModified
     * @return
     */
    long insert(String fileName, long dateModified) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_NAME_FILEPATH, fileName);
        values.put(Entry.COLUMN_NAME_LASTMODIFIED, dateModified);

        return db.insert(Entry.TABLE_NAME, null, values);
    }

    /**
     * Updates data in database
     * @param fileName
     * @param dateModified
     * @return
     */
    long update(String fileName, long dateModified) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Entry.COLUMN_NAME_FILEPATH, fileName);
        values.put(Entry.COLUMN_NAME_LASTMODIFIED, dateModified);

        String selection = Entry.COLUMN_NAME_FILEPATH + " = ?";
        String[] selectionArgs = { fileName };

        return db.update(Entry.TABLE_NAME, values, selection, selectionArgs);
    }

    /**
     * Find when the file was last modified
     * @param fileName
     * @return
     */
    Long getLastModified(String fileName) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                BaseColumns._ID,
                Entry.COLUMN_NAME_FILEPATH,
                Entry.COLUMN_NAME_LASTMODIFIED
        };

        //Filter results WHERE
        String selection = Entry.COLUMN_NAME_FILEPATH + " = ?";
        String[] selectionArgs = { fileName };

        Cursor cursor = db.query(
                Entry.TABLE_NAME,   //The table to query
                projection,     // The array of Columns
                selection,      //The columns for WHERE clause
                selectionArgs,  //The values for WHERE clause
                null,       //Don't group the rows
                null,       //Don't filter by row groups
                null        //Don't sort the results
        );

        Long result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getLong(cursor.getColumnIndexOrThrow(Entry.COLUMN_NAME_LASTMODIFIED));
        }

        cursor.close();

        return result;
    }

    /**
     * Delete specific entry in table
     * @param fileName
     * @return
     */
    int delete(String fileName) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String selection = Entry.COLUMN_NAME_FILEPATH + " = ?";
        String[] selectionArgs = { fileName };

        return db.delete(Entry.TABLE_NAME, selection, selectionArgs);
    }


    /**
     * Delete all entries in the table
     * @return
     */
    int deleteAll() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String selection = "1 = ?";
        String[] selectionArgs = { "1" };

        return db.delete(Entry.TABLE_NAME, selection, selectionArgs);
    }
}
