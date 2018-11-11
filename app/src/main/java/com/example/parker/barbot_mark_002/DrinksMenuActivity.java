package com.example.parker.barbot_mark_002;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class DrinksMenuActivity extends AppCompatActivity {

    private static final String TAG = "Drinks Menu";
    private String TABLE_NAME = "Drinks";
    private String liquidFilter = null;

    DatabaseHelper myDbHelper  = new DatabaseHelper(this);

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drink_menu_activity);
        mListView = (ListView) findViewById(R.id.List);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            liquidFilter = extras.getString("liquidFilter");
        }

        try {
            myDbHelper.openDataBase();
        }catch(SQLiteException sqle){
            throw sqle;
        }
        //Toast.makeText(DrinksMenuActivity.this,"Filter on drink: "+liquidFilter,Toast.LENGTH_SHORT).show();
        populateListView();
    }


    private void populateListView() {
        Log.d(TAG, "populateListView: Displaying data in the ListView.");

        ArrayList<String> listData = new ArrayList<>();

        try {
            //get the data and append to a list
            //Cursor data = myDbHelper.getData(TABLE_NAME);
            if (liquidFilter == null) {
                Cursor data = myDbHelper.getData(TABLE_NAME);
                while (data.moveToNext()) {
                    //get the value from the database in column 1
                    //then add it to the ArrayList
                    listData.add(data.getString(1));
                }
            } else {
                String SQLquery = "SELECT * FROM " + TABLE_NAME + " WHERE \""+ liquidFilter +"\" > '"+ 0.0 +"'";
                Cursor data = myDbHelper.getQuery(SQLquery);
                while (data.moveToNext()) {
                        //get the value from the database in column 1
                        //then add it to the ArrayList
                        listData.add(data.getString(1));
                }
            }
        } catch(Exception e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
        if(listData.size()==1){
            Intent editScreenIntent = new Intent(DrinksMenuActivity.this, DrinkLevelActivity.class);
            editScreenIntent.putExtra("name",listData.get(0).toString());
            startActivity(editScreenIntent);
        }

        //create the list adapter and set the adapter
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        mListView.setAdapter(adapter);

        //set an onItemClickListener to the ListView
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = adapterView.getItemAtPosition(i).toString();
                Log.d(TAG, "onItemClick: You Clicked on " + name);

                Cursor data = myDbHelper.getItemID(name, TABLE_NAME); //get the id associated with that name
                int itemID = -1;
                while(data.moveToNext()){
                    itemID = data.getInt(0);
                }
                if(itemID > -1){
                    Log.d(TAG, "onItemClick: The ID is: " + itemID);
                    Intent editScreenIntent = new Intent(DrinksMenuActivity.this, DrinkLevelActivity.class);
                    editScreenIntent.putExtra("name",name);
                    startActivity(editScreenIntent);
                }
                else{
                    toastMessage("No ID associated with that name");
                }
            }
        });

    }

    /**
     * customizable toast
     * @param message
     */

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    public void goToScreen1 (View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }

}
