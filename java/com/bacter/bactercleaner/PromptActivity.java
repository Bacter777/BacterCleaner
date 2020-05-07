package com.bacter.bactercleaner;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class PromptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_prompt);

        Button button = findViewById(R.id.button1);
        button.setOnClickListener (view ->{
            startActivityForResult (new Intent (android.provider.Settings.ACTION_SETTINGS), 0);
            System.exit(0);
        });
    }
}
