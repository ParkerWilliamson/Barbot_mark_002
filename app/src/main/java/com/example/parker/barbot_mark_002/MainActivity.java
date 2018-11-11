package com.example.parker.barbot_mark_002;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.content.Intent;
import android.view.View;

import android.os.AsyncTask;
import android.widget.Button;
import android.widget.EditText;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends AppCompatActivity {

    private static final String name = "Liquid Menu";
    private String TABLE_NAME = "Liquids";

    DatabaseHelper myDbHelper  = new DatabaseHelper(this);

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(name);

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
                        mWebView.setWebChromeClient(new WebChromeClient());
                        mWebView.loadUrl("http://192.168.1.177");
                    }
                });
            }
        }, 0, 1000);


        // set the recipe in the arduino when button is pressed
        Button setRecipe1Button = (Button) findViewById(R.id.setRecipe1Button);
        setRecipe1Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // get EditText by id and store it into "num"
                EditText num = (EditText) findViewById(R.id.recipeSelector);
                // Store EditText - Input in variable
                int recipeSel = Integer.parseInt(num.getText().toString());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Background_get().execute("recipe="+recipeSel);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new Background_get().execute("recipe="+recipeSel);
                }
                return true;
            }
        });

        mListView = findViewById(R.id.List);

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
        populateListView();
    }

    private void populateListView() {
        Log.d(name, "populateListView: Displaying data in the ListView.");

        ArrayList<String> listData = new ArrayList<>();

        try {
            //get the data and append to a list
            Cursor data = myDbHelper.getData(TABLE_NAME);
            while (data.moveToNext()) {
                //get the value from the database in column 1
                String liquid = data.getString(1);
                if ((!(liquid.equals("Water"))) & (!(liquid.equals("Carbonated Water")))& (!(liquid.equals("Simple Syrup")))) {
                    //then add it to the ArrayList
                    listData.add(liquid);
                }
            }
        } catch (Exception e) {
        }

        //create the list adapter and set the adapter
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        mListView.setAdapter(adapter);

        //set an onItemClickListener to the ListView
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String liquid = adapterView.getItemAtPosition(i).toString();
                Log.d(name, "onItemClick: You Clicked on " + liquid);

                Cursor data = myDbHelper.getItemID(liquid, TABLE_NAME); //get the id associated with that name
                int itemID = -1;
                while(data.moveToNext()){
                    itemID = data.getInt(0);
                }
                if(itemID > -1){
                    Log.d(name, "onItemClick: The ID is: " + itemID);
                    Intent editScreenIntent = new Intent(MainActivity.this, DrinksMenuActivity.class);
                    editScreenIntent.putExtra("liquidFilter",liquid);
                    startActivity(editScreenIntent);
                }
                else{
                    Toast.makeText(MainActivity.this,"No ID associated with that name",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    //go to screen 2 button
    public void goToScreen2 (View view){
        Intent intent = new Intent (this, DrinksMenuActivity.class);
        startActivity(intent);
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
