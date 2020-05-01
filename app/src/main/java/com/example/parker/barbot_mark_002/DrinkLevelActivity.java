package com.example.parker.barbot_mark_002;

import android.content.ContentValues;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


public class DrinkLevelActivity extends AppCompatActivity {
    String name = "name";
    DatabaseHelper myDbHelper  = new DatabaseHelper(this);
    Cursor data;
    Cursor ingredientData;
    ArrayList<Boolean> alcoholic = new ArrayList<>();
    float step = 5;

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
        //get the garnishment text to display
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
                int AlcLiq = ingredientData.getInt(2);
                ingredients += Liquid + ": ";
                percentage = percentage;
                ingredients += String.valueOf(Math.round(percentage*100));
                ingredients += "%\n";
            }

        }
        final TextView garnishmentsTxt = (TextView) findViewById(R.id.txtGarnishments);
        final TextView ingredientsTxt = (TextView) findViewById(R.id.txtIngredients);
        garnishmentsTxt.setText("Garnishments: " + "\n" + garnishments);
        ingredientsTxt.setText("Ingredients: " + "\n" +ingredients);

        //set adjustments of this drink to 0
        String ZeroedRows = myDbHelper.setAdjtoZero();



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
  //              Toast.makeText(DrinkLevelActivity.this,"here",Toast.LENGTH_SHORT).show();
                ingredientData.moveToFirst();
                int LiquidId = ingredientData.getInt(0); //N
                String Liquid = ingredientData.getString(1); //N
                Float percentage =  data.getFloat(data.getColumnIndex(Liquid)); //N
                Float percAjustment = ingredientData.getFloat(3); //N
                percentage = percentage + percAjustment;
                URLaddendem += "B"+ String.valueOf(LiquidId)+"="; //N
                URLaddendem += Math.round(percentToTime*percentage); //N
                while(ingredientData.moveToNext()){
                    LiquidId = ingredientData.getInt(0);
                    Liquid = ingredientData.getString(1);
                    percentage =  data.getFloat(data.getColumnIndex(Liquid));
                    percAjustment = ingredientData.getFloat(3);
                    percentage = percentage + percAjustment;
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

    //add to alcoholic liquid percentages
    public void MakeStronger (View view){
        //initialize
        int drink_ingredients_count = 0;
        int alchol_drink_ingredients = 0;
        int non_alchol_drink_ingredients = 0;
        float alch_total = 0;
        String drinkQuery = "SELECT * FROM Drinks WHERE Drink = '"+ name + "'";

        //get the garnishment text to display
        Cursor drinkRow = myDbHelper.getQuery(drinkQuery);
        drinkRow.moveToFirst();
        //parse ingredients to get count of alch, non-alch
        String ingredientsQuery = "SELECT * FROM Liquids";
        ingredientData = myDbHelper.getQuery(ingredientsQuery);
        Boolean First = TRUE;
        ingredientData.moveToFirst();
        while(ingredientData.moveToNext()){
            if (First == TRUE) {
                ingredientData.moveToFirst();
                First = FALSE;
            }
            //get the value from the database in column 1
            //then add it to the ArrayList
            String Liquid = ingredientData.getString(1);
            Float liquidAdj = ingredientData.getFloat(3);
            Float percentage =  drinkRow.getFloat(drinkRow.getColumnIndex(Liquid));
            if (percentage>0) {
                int AlcLiq = ingredientData.getInt(2);
                if(AlcLiq==0){
                    non_alchol_drink_ingredients = non_alchol_drink_ingredients+1;
                    drink_ingredients_count = drink_ingredients_count+1;
//                    Toast.makeText(DrinkLevelActivity.this, Liquid+ ":" + percentage + "+" + liquidAdj +" = " + (percentage + liquidAdj), Toast.LENGTH_SHORT).show();
                } else if(AlcLiq==1){
                    alchol_drink_ingredients = alchol_drink_ingredients+1;
                    drink_ingredients_count = drink_ingredients_count+1;
                    alch_total = alch_total + percentage + liquidAdj;  //Total amount of alch so far + init drink quant of liquid + liquid adjustment seems wrong..
//                    Toast.makeText(DrinkLevelActivity.this, Liquid + ":" + percentage + "+" + liquidAdj +" = " + (percentage + liquidAdj), Toast.LENGTH_SHORT).show();
                }
            }

        }

        //make sure the drink doesn't go >0 non-alch
        if (alchol_drink_ingredients>0 & non_alchol_drink_ingredients>0) {
            //get the garnishment text to display
            drinkRow.moveToFirst();
            //parse ingredients
            String garnishments;
            garnishments = drinkRow.getString(drinkRow.getColumnIndex("Garnishments"));
            //parse ingredients
            ingredientData.moveToFirst(); //must check the first one instead
            String ingredients = "";
            Boolean FirstB = TRUE;
            while (ingredientData.moveToNext()) {
                if (FirstB == TRUE) {
                    ingredientData.moveToFirst();
                    FirstB = FALSE;
                }
                //get the value from the database in column 1
                //then add it to the ArrayList
                String Liquid = ingredientData.getString(1);
                float liquidAdj = ingredientData.getFloat(3);
                Float percentage = drinkRow.getFloat(drinkRow.getColumnIndex(Liquid));
                if (percentage > 0) {
                    int AlcLiq = ingredientData.getInt(2);

                    //Non-Alch adjustment
                    if (AlcLiq == 0) {
                        liquidAdj = liquidAdj - (percentage + liquidAdj) * (float) .1;  //needs to be liquidAdj+initial liquid
                        //update adjustment in db
                        String updateText = myDbHelper.updateDrinkAdj(Liquid, liquidAdj);
//                        Toast.makeText(DrinkLevelActivity.this, Liquid + "/ input:" + liquidAdj+" -(" + percentage+ liquidAdj + ")*" + .1 + " = "  + updateText + " + " + (percentage+liquidAdj), Toast.LENGTH_SHORT).show();
                    //alch adjustment
                    } else if (AlcLiq == 1) {
                        liquidAdj = liquidAdj + ((1 - alch_total) * ((float) .1) * ((percentage + liquidAdj) / alch_total));
                        //update adjustment in db
                        String updateText = myDbHelper.updateDrinkAdj(Liquid, liquidAdj);
//                       Toast.makeText(DrinkLevelActivity.this, Liquid + "/ inputs: ((1-" + alch_total + ")*" + .1 + ")*((" + percentage+liquidAdj + ")/" +alch_total+ ")) = " + (percentage+liquidAdj) + " + " + updateText, Toast.LENGTH_SHORT).show();

                    }
                    ingredients += Liquid + ": ";
                    ingredients += String.valueOf(Math.round((percentage + liquidAdj) * 100));
                    ingredients += "%\n";
                }

            }

            final TextView garnishmentsTxt = (TextView) findViewById(R.id.txtGarnishments);
            final TextView ingredientsTxt = (TextView) findViewById(R.id.txtIngredients);
            garnishmentsTxt.setText("Garnishments: " + "\n" + garnishments);
            ingredientsTxt.setText("Ingredients: " + "\n" + ingredients);
        }
    }

    //subtract from alcoholic liquid percentages
    public void MakeWeaker (View view){
        //initialize
        int drink_ingredients_count = 0;
        int alchol_drink_ingredients = 0;
        int non_alchol_drink_ingredients = 0;
        float alch_total = 0;
        String drinkQuery = "SELECT * FROM Drinks WHERE Drink = '"+ name + "'";

        //get the garnishment text to display
        Cursor drinkRow = myDbHelper.getQuery(drinkQuery);
        drinkRow.moveToFirst();
        //parse ingredients to get count of alch, non-alch
        String ingredientsQuery = "SELECT * FROM Liquids";
        ingredientData = myDbHelper.getQuery(ingredientsQuery);
        Boolean First = TRUE;
        ingredientData.moveToFirst();
        while(ingredientData.moveToNext()){
            if (First == TRUE) {
                ingredientData.moveToFirst();
                First = FALSE;
            }
            //get the value from the database in column 1
            //then add it to the ArrayList
            String Liquid = ingredientData.getString(1);
            Float liquidAdj = ingredientData.getFloat(3);
            Float percentage =  drinkRow.getFloat(drinkRow.getColumnIndex(Liquid));
            if (percentage>0) {
                int AlcLiq = ingredientData.getInt(2);
                if(AlcLiq==0){
                    non_alchol_drink_ingredients = non_alchol_drink_ingredients+1;
                    drink_ingredients_count = drink_ingredients_count+1;
//                    Toast.makeText(DrinkLevelActivity.this, Liquid+ ":" + percentage + "+" + liquidAdj +" = " + (percentage + liquidAdj), Toast.LENGTH_SHORT).show();
                } else if(AlcLiq==1){
                    alchol_drink_ingredients = alchol_drink_ingredients+1;
                    drink_ingredients_count = drink_ingredients_count+1;
                    alch_total = alch_total + percentage + liquidAdj;  //Total amount of alch so far + init drink quant of liquid + liquid adjustment seems wrong..
//                    Toast.makeText(DrinkLevelActivity.this, Liquid + ":" + percentage + "+" + liquidAdj +" = " + (percentage + liquidAdj), Toast.LENGTH_SHORT).show();
                }
            }

        }

        //make sure the drink doesn't go >0 non-alch
        if (alchol_drink_ingredients>0 & non_alchol_drink_ingredients>0) {
            //get the garnishment text to display
            drinkRow.moveToFirst();
            //parse ingredients
            String garnishments;
            garnishments = drinkRow.getString(drinkRow.getColumnIndex("Garnishments"));
            //parse ingredients
            ingredientData.moveToFirst(); //must check the first one instead
            String ingredients = "";
            Boolean FirstB = TRUE;
            while (ingredientData.moveToNext()) {
                if (FirstB == TRUE) {
                    ingredientData.moveToFirst();
                    FirstB = FALSE;
                }
                //get the value from the database in column 1
                //then add it to the ArrayList
                String Liquid = ingredientData.getString(1);
                float liquidAdj = ingredientData.getFloat(3);
                Float percentage = drinkRow.getFloat(drinkRow.getColumnIndex(Liquid));
                if (percentage > 0) {
                    int AlcLiq = ingredientData.getInt(2);

                    //Non-Alch adjustment
                    if (AlcLiq == 0) {
                        liquidAdj = liquidAdj + (alch_total * ((float) .1) * ((percentage + liquidAdj) / (1 - alch_total)));
                        //update adjustment in db
                        String updateText = myDbHelper.updateDrinkAdj(Liquid, liquidAdj);
//                        Toast.makeText(DrinkLevelActivity.this, Liquid + "/ input:" + liquidAdj+" -(" + percentage+ liquidAdj + ")*" + .1 + " = "  + updateText + " + " + (percentage+liquidAdj), Toast.LENGTH_SHORT).show();
                        //alch adjustment
                    } else if (AlcLiq == 1) {
                        liquidAdj = liquidAdj - (percentage + liquidAdj) * (float) .1;
                        //update adjustment in db
                        String updateText = myDbHelper.updateDrinkAdj(Liquid, liquidAdj);
//                       Toast.makeText(DrinkLevelActivity.this, Liquid + "/ inputs: ((1-" + alch_total + ")*" + .1 + ")*((" + percentage+liquidAdj + ")/" +alch_total+ ")) = " + (percentage+liquidAdj) + " + " + updateText, Toast.LENGTH_SHORT).show();

                    }
                    ingredients += Liquid + ": ";
                    ingredients += String.valueOf(Math.round((percentage + liquidAdj) * 100));
                    ingredients += "%\n";
                }

            }

            final TextView garnishmentsTxt = (TextView) findViewById(R.id.txtGarnishments);
            final TextView ingredientsTxt = (TextView) findViewById(R.id.txtIngredients);
            garnishmentsTxt.setText("Garnishments: " + "\n" + garnishments);
            ingredientsTxt.setText("Ingredients: " + "\n" + ingredients);
        }
    }
}