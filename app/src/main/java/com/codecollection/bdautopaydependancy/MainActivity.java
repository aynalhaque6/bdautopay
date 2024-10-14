package com.codecollection.bdautopaydependancy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.codecollection.bdautopay.bdautopay;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private LinearLayout payLay;
    private EditText amountEt;
    private Button payBtn;
    private bdautopay pay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pay = findViewById(R.id.payment);
        payLay = findViewById(R.id.payLay);
        amountEt = findViewById(R.id.amountEt);
        payBtn = findViewById(R.id.payBtn);

        pay.initialize(this, "HvW8S2Y1q3LAiNybpbVgLNA3dyFUxT6QzGi3BpFQTZd32wDWCW");
        pay.setPaymentListener(new bdautopay.PaymentListener() {
            @Override
            public void onPaymentVerified(String status, JSONObject response) {
                // Handle payment success
                pay.setVisibility(View.GONE);
                payLay.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Payment Verified: " + status, Toast.LENGTH_LONG).show();
                Log.e("BDPayService", "Status: " + status + "        Response: " + response.toString());
            }

            @Override
            public void onPaymentCancelled(String url) {
                // Handle payment failure
                Toast.makeText(MainActivity.this, "Payment Cancelled", Toast.LENGTH_LONG).show();
                Log.e("BDPayService", "Failed: " + url);
            }

            @Override
            public void onPaymentError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Payment Error: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e("BDPayService", "Payment Error: " + errorMessage);
            }
        });

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (amountEt.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter amount", Toast.LENGTH_SHORT).show();
                } else {
                    payLay.setVisibility(View.GONE);
                    pay.setVisibility(View.VISIBLE);
                    pay.startPayment("John Doe", "john.doe@example.com", amountEt.getText().toString());

                }

            }
        });


    }
}