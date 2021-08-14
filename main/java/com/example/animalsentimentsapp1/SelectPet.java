package com.example.animalsentimentsapp1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SelectPet extends AppCompatActivity {
    private RadioGroup radioGroup;
    private RadioButton rButton1, rButton2;
    private Button next;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pet);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        rButton1 = (RadioButton) findViewById(R.id.radioButton3);
        rButton2 = (RadioButton) findViewById(R.id.radioButton4);
       // next = (Button) findViewById(R.id.button);
        email = getIntent().getStringExtra("email");
//        Toast.makeText(getApplicationContext(),SharedPrefManager.getInstance(this).getUserEmail(),Toast.LENGTH_LONG).show();
}

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButton3:
              if (checked)
              {
                  Intent intent = new Intent(this, Upload.class);
                  startActivity(intent);
              }
              break;

            case R.id.radioButton4:
                if (checked)
                {
                    Intent intent = new Intent(this, Upload2.class);
                    startActivity(intent);
                }
                break;
        }
    }
}

