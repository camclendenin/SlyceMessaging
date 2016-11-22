package it.snipsnap.slyce_messaging_example;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by sambains on 05/09/2016.
 */

public class HomeActivity extends AppCompatActivity {

    private Button openChat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        openChat = (Button) findViewById(R.id.open_chat);
        openChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.startActivity(HomeActivity.this,
                        new Intent(HomeActivity.this, MainActivity.class), null);
            }
        });
    }
}
