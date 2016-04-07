package com.example.haihai.myapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.californium.examples.SecureClient;

public class MainActivity extends AppCompatActivity {
    private EditText txtUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(onBtnClickListener);

        this.txtUri = (EditText)findViewById(R.id.editText);
    }

    private OnClickListener onBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            String uri = txtUri.getText().toString();
            new Thread(new sendSecReqThread(uri)).start();
        }
    };
    private class sendSecReqThread implements Runnable {
        private String uri;
        public sendSecReqThread(String uri) {
            this.uri = uri;
        }
        @Override
        public void run() {
            SecureClient client = new SecureClient();
            client.test(this.uri);
        }
    }
}
