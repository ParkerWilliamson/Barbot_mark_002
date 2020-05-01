package com.example.parker.barbot_mark_002;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DatabaseHelper extends SQLiteOpenHelper {

    //1
    private final Context myContext;
    private static String DB_PATH = "/data/data/com.example.parker.barbot_mark_002/";
    private static String DB_NAME = "init_drink_info_05.db";
    private SQLiteDatabase myDataBase;

    //1
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    //1
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    //1
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion)
            try {
                copyDataBase();
            } catch (IOException e) {
                e.printStackTrace();

            }
    }

    //1
    @Override
    public synchronized void close() {
        if (myDataBase != null)
            myDataBase.close();
        super.close();
    }

    //1 unused
    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();
        if (dbExist) {
        } else {
            //String stackTrace = Log.getStackTraceString(ioe);
            //Toast.makeText(this.myContext,"DB doesn't exist",Toast.LENGTH_SHORT).show();
            this.getWritableDatabase();
            try {
                copyDataBase();
            } catch (IOException e) {
                String stackTrace = Log.getStackTraceString(e);
                Toast.makeText(this.myContext,stackTrace,Toast.LENGTH_SHORT).show();
                //throw new Error("Error copying database");
            }
        }
    }

    //1
    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
        } catch (SQLiteException e) {
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    //1
    private void copyDataBase() throws IOException {
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        String outFileName = DB_PATH + DB_NAME;
        OutputStream myOutput = new FileOutputStream(outFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        myOutput.flush();
        myOutput.close();
        myInput.close();
        //Toast.makeText(this.myContext,"copied?",Toast.LENGTH_SHORT).show();

    }

    //1
    public void openDataBase() throws SQLException {
        String myPath = DB_PATH + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);

    }

    //2
    /**
     * Returns all the data from database
     * @return
     */
    public Cursor getData(String TABLE_NAME){
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = myDataBase.rawQuery(query, null);
        return data;
    }

    //2
    /**
     * Returns only the ID that matches the name passed in
     * @param name
     * @return
     */
    public Cursor getItemID(String name, String TABLE_NAME){
        String identifier;
        if (TABLE_NAME == "Drinks") {
            identifier = "Drink";
        } else {
            identifier = "Liquid";
        }
        String query = "SELECT _id FROM " + TABLE_NAME +
                " WHERE " + identifier + " = '" + name + "'";
        Cursor data = myDataBase.rawQuery(query, null);
        return data;
    }

    public Cursor getQuery(String query){
        Cursor data = myDataBase.rawQuery(query, null);
        return data;
    }

    public String updateDrinkAdj(String Liquid, Float Adj) {
        ContentValues args = new ContentValues();
        Cursor singlePointer = getQuery( "SELECT * From Liquids Where Liquid = '" + Liquid + "'"); //
        singlePointer.moveToFirst();
        Float initialValue = singlePointer.getFloat(3);
        Float nextValue = Adj; //initial value is 1 for some reason
        args.put("CurrentDrinkAdjustments", nextValue);
        int rowsAffected = myDataBase.update("Liquids", args, "Liquid = ?", new String[]{Liquid});  //this is causing an error
        Cursor singlePointer2 = getQuery( "SELECT * From Liquids Where Liquid = '" + Liquid + "'");
        singlePointer2.moveToFirst();
        Float ABC123 = singlePointer2.getFloat(3); //this is causing an error (worked with index located at 0, maybe it is still using the old table
        return "" +ABC123;
    }

    public String setAdjtoZero() {
        ContentValues args = new ContentValues();
        args.put("CurrentDrinkAdjustments", (float) 0);
        int rowsAffected = myDataBase.update("Liquids", args, null,null);  //this is causing an error
        return "number of rows affected "+rowsAffected;
    }

    //2
    /**
     * Delete from database
     * @param id
     * @param name

    public void deleteName(int id, String name){
    }
     */

}