package com.example.parker.barbot_mark_002;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class DrinkLevelActivity extends AppCompatActivity {
    int id = -1;
    String name = "name";
    DatabaseHelper myDbHelper  = new DatabaseHelper(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("id");
            name = extras.getString("name");
        }
        setTitle(name);
        setContentView(R.layout.drink_activity);

        //open database connection
        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }
        try {
            myDbHelper.openDataBase();
        }catch(SQLiteException sqle){
            throw sqle;
        }

        String rowQuery = "SELECT * FROM Drinks WHERE Drink = '"+ name + "'";
        //get the garnishment test to display
        Cursor data = myDbHelper.getQuery(rowQuery);
        String garnishments;
        data.moveToFirst();
        garnishments = data.getString(data.getColumnIndex("Garnishments"));
        //parse ingredients
        String ingredientsQuery = "SELECT * FROM Liquids";
        Cursor ingredientData = myDbHelper.getQuery(ingredientsQuery);
        String ingredients = "";
        while(ingredientData.moveToNext()){
            //get the value from the database in column 1
            //then add it to the ArrayList
            String Liquid = ingredientData.getString(1);
            Float percentage =  data.getFloat(data.getColumnIndex(Liquid));
            if (percentage>0) {
                ingredients += Liquid + ": ";
                ingredients += String.valueOf(Math.round(percentage*100));
                ingredients += "%\n";
            }
        }
        final TextView garnishmentsTxt = (TextView) findViewById(R.id.txtGarnishments);
        final TextView ingredientsTxt = (TextView) findViewById(R.id.txtIngredients);
        garnishmentsTxt.setText("Garnishments: " + "\n" + garnishments);
        ingredientsTxt.setText("Ingredients: " + "\n" +ingredients);

        //Image of drink
        ImageView image = (ImageView) findViewById(R.id.imgDrink);
        //int temp = data.getColumnIndex("Photo");
        byte[] Blob = data.getBlob(data.getColumnIndex("Photo"));
        if (Blob==null){
            image.setImageBitmap(null);
            //Toast.makeText(this,"Image/blob is null",Toast.LENGTH_SHORT).show();
        }else{
            Bitmap drinkImage = BitmapFactory.decodeByteArray(Blob, 0, Blob.length);
            image.setImageBitmap(drinkImage);
            //Toast.makeText(this,"selection success: "+(Blob.length),Toast.LENGTH_SHORT).show();
        }


        //constantly updated status
        Timer modded = new Timer();
        modded.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Write code for your refresh logic
                        WebView mWebView = (WebView)findViewById(R.id.arduinoWindow);
                        mWebView.clearCache(true);
                        mWebView.getSettings().setJavaScriptEnabled(true);
                        mWebView.loadUrl("http://192.168.1.177");
                    }
                });
            }
        }, 0, 1000);
    }


}