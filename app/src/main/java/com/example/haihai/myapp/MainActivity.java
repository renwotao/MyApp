package com.example.haihai.myapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;

import org.eclipse.californium.examples.SecureClient;

public class MainActivity extends AppCompatActivity {
    private static final String SENT_SMS_ACTION = "com.paad.smssnippets.SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "com.paad.smssnippets.DELIVERED_SMS_ACTION";
    private static final String SMS_BINARY_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";
    private static final short SMS_PORT = 8091;

    private static final String URI_TIP = "URI cannot be empty!";
    private static final String PHONENUMBER_TIP = "Phone number cannot be empty!";

    private EditText txtUri = null;
    private EditText txtPhone = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态注册短信广播接收器，可使用静态注册方式即在AndroidManifest.xml中注明
        registerReceiver(sendReceiver, new IntentFilter(SENT_SMS_ACTION));
        registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_SMS_ACTION));

        IntentFilter filter = new IntentFilter(SMS_BINARY_RECEIVED);
        filter.addDataAuthority("localhost", "8091");
        filter.addDataScheme("sms");
        registerReceiver(binarySMSReceiver, filter);

        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(onBtnClickListener);
        this.txtUri = (EditText)findViewById(R.id.url_text);
        this.txtPhone = (EditText)findViewById(R.id.phone_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(sendReceiver);
        unregisterReceiver(deliveredReceiver);
        unregisterReceiver(binarySMSReceiver);
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
                new Thread(new SendSeqReqOverSMSThread(uri,phoneNum)).start();
            } else  if (uri.startsWith("coaps://")) {
                new Thread(new SendSecReqThread(uri)).start();
            }
        }
    };

    private class SendSeqReqOverSMSThread implements Runnable {
        private String uri;
        private String phoneNum;
        public SendSeqReqOverSMSThread(String uri, String phoneNum) {
            this.uri = uri;
            this.phoneNum = phoneNum;
        }
        public void run() {
            sendBinarySms(phoneNum, "hello".getBytes());
        }

    }

    private  void sendBinarySms(String phonenumber, byte[] message) {
        SmsManager manager = SmsManager.getDefault();

        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent(SENT_SMS_ACTION), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_SMS_ACTION), 0);

        manager.sendDataMessage(phonenumber, null, SMS_PORT, message, piSend, piDelivered);
    }

    private BroadcastReceiver binarySMSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle bundle = intent.getExtras();
            String format = intent.getStringExtra("format");
            SmsMessage[] msgs;

            if(null != bundle) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (null != pdus) {
                    msgs = new SmsMessage[pdus.length];
                    byte[] data;

                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);

                        byte[] pdu = msgs[i].getPdu();
                        data = msgs[i].getUserData();
                        Toast.makeText(context, new String(data), Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
    };


    private BroadcastReceiver sendReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String resultText = "UNKNOWN";

            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    resultText = "Transmission successful"; break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    resultText = "Transmission failed"; break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    resultText = "Transmission failed: Radio is off"; break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    resultText = "Transmission failed: No PDU specified"; break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    resultText = "Transmission failed: No service"; break;
            }

            Toast.makeText(context, resultText, Toast.LENGTH_SHORT).show();
        }

    };

    private BroadcastReceiver deliveredReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String info = "Delivery information: ";

            switch(getResultCode())
            {
                case Activity.RESULT_OK: info += "delivered"; break;
                case Activity.RESULT_CANCELED: info += "not delivered"; break;
            }

            Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
        }
    };

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
