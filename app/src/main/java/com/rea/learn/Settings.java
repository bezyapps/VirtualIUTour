package com.rea.learn;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

public class Settings extends AppCompatActivity {

    AutoCompleteTextView editTextIP;
    NumberPicker numberPickerFrame;
    public static final String SKIP_RATE = "skipRate";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String[] IPs = {"http://192.168.11.252:8080/StrutsMavenProject/image.json", "http://110.93.196.115:8080/StrutsMavenProject/image.json","http://192.168.1.178:8080/image.json", "http://192.168.:8080/image.json"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initUI();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Settings.this,MainActivity.class);
                i.putExtra(SKIP_RATE,numberPickerFrame.getValue());
                i.putExtra(IP_ADDRESS,editTextIP.getText().toString());
                Log.e("REA_IP", editTextIP.getText().toString());
                startActivity(i);
            }
        });
    }

    private void initUI()
    {
        editTextIP = (AutoCompleteTextView) findViewById(R.id.editTextIP);
        numberPickerFrame = (NumberPicker) findViewById(R.id.numberPickerFrame);
        editTextIP.setSelection(editTextIP.getText().length());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, IPs);
        editTextIP.setAdapter(adapter);
        numberPickerFrame.setMaxValue(20);
        numberPickerFrame.setMinValue(1);
        numberPickerFrame.setValue(2);
    }

}
