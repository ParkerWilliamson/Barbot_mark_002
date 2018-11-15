package com.example.parker.barbot_mark_002;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;



public class DrinkLevelActivity extends AppCompatActivity {
    String name = "name";
    DatabaseHelper myDbHelper  = new DatabaseHelper(this);
    Cursor data;
    Cursor ingredientData;

    float percentToTime=45000; //goal is 250 mL
    //Calibration after init => 20000 results in  250 ml total drink volume
    //assumed total 5 oz drink at a rate of .25 oz/sec  #1.5 oz = shot #four count is about 1 shot => ~=5 oz for cocktail

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
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
        data = myDbHelper.getQuery(rowQuery);
        String garnishments;
        data.moveToFirst();
        garnishments = data.getString(data.getColumnIndex("Garnishments"));
        //parse ingredients
        String ingredientsQuery = "SELECT * FROM Liquids";
        ingredientData = myDbHelper.getQuery(ingredientsQuery);
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

        // set the recipe in the arduino when button is pressed
        Button btnPour = (Button) findViewById(R.id.btnPour);
        btnPour.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //ingredientData = all liquids from liquids table
                //data = selected drink row from drinks table
                String DrinkId = String.valueOf(data.getInt(0));
                String URLaddendem = "recipe="+DrinkId;
                //String ingredientsQuery = "SELECT * FROM Liquids";
                //ingredientData = myDbHelper.getQuery(ingredientsQuery);
                Toast.makeText(DrinkLevelActivity.this,"here",Toast.LENGTH_SHORT).show();
                ingredientData.moveToFirst();
                while(ingredientData.moveToNext()){
                    int LiquidId = ingredientData.getInt(0);
                    String Liquid = ingredientData.getString(1);
                    Float percentage =  data.getFloat(data.getColumnIndex(Liquid));
                    URLaddendem += "B"+ String.valueOf(LiquidId)+"=";
                    URLaddendem += Math.round(percentToTime*percentage);
                    //Toast.makeText(DrinkLevelActivity.this,String.valueOf(LiquidId),Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(DrinkLevelActivity.this,URLaddendem,Toast.LENGTH_SHORT).show();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new DrinkLevelActivity.Background_get().execute(URLaddendem);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new DrinkLevelActivity.Background_get().execute(URLaddendem);
                }
                return true;
            }
        });
    }

    //writes the get command
    private class Background_get extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                /* Change the IP to the IP you set in the arduino sketch */
                URL url = new URL("http://192.168.1.177/?" + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    result.append(inputLine).append("\n");

                in.close();
                connection.disconnect();
                return result.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }
}