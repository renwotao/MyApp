package com.example.haihai.myapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.californium.examples.SecureClient;
import org.eclipse.californium.examples.SmsSecureClient;

import java.net.InetSocketAddress;

import cn.sms.util.SmsHander;
import cn.sms.util.SmsSocket;
import cn.sms.util.SmsSocketAddress;

public class MainActivity extends AppCompatActivity {
    private static final String URI_TIP = "URI cannot be empty!";
    private static final String PHONENUMBER_TIP = "Phone number cannot be empty!";

    private EditText txtUri = null;
    private EditText txtPhone = null;
    private SmsSocket smsSocket = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        smsSocket = new SmsSocket(this, "8091");
        smsSocket.init();

        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(onBtnClickListener);
        this.txtUri = (EditText)findViewById(R.id.url_text);
        this.txtPhone = (EditText)findViewById(R.id.phone_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        smsSocket.unInit();
    }

    private OnClickListener onBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String uri = txtUri.getText().toString();
            String phoneNum = txtPhone.getText().toString();
            if (txtUri.length() == 0) {
                Toast.makeText(getBaseContext(), URI_TIP, Toast.LENGTH_SHORT).show();
                return;
            }
            if (uri.startsWith("coaps+sms://")) {
                if (txtPhone.length() == 0) {
                    Toast.makeText(getBaseContext(), PHONENUMBER_TIP, Toast.LENGTH_SHORT).show();
                    return;
                }
                uri = uri.replace("+sms", "");
                SmsSocketAddress dstSmsSocketAddress =  new SmsSocketAddress(phoneNum, (short)8091);
                new Thread(new SendSeqReqOverSMSThread(uri,smsSocket,dstSmsSocketAddress)).start();
            } else  if (uri.startsWith("coaps://")) {

                new Thread(new SendSecReqThread(uri)).start();
            }
        }
    };

    private class SendSeqReqOverSMSThread implements Runnable {
        private String uri;
        private SmsSocket smsSocket;
        private SmsSocketAddress smsSocketAddress;

        public SendSeqReqOverSMSThread(String uri, SmsSocket smsSocket, SmsSocketAddress smsSocketAddress) {
            this.uri = uri;
            this.smsSocket = smsSocket;
            this.smsSocketAddress = smsSocketAddress;
        }
        public void run() {
            /*smsSocket.sendBinary(phoneNum, port,"hello".getBytes());*/
            SmsSecureClient client = new SmsSecureClient(smsSocket, smsSocketAddress);
            client.test(uri);
        }

    }

    private class SendSecReqThread implements Runnable {
        private String uri;

        public SendSecReqThread(String uri) {
           this.uri = uri;
        }
        @Override
        public void run() {
            SecureClient client = new SecureClient();
            client.test(this.uri);

        }
    }
}
