package com.codecollection.bdautopay;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PaymentExtension {

    private static final String TAG = "BDPayService";
    private static final String PAYMENT_CREATE_URL = "https://pay.bdautopay.com/api/payment/create";
    private static final String PAYMENT_VERIFY_URL = "https://pay.bdautopay.com/api/payment/verify";
    private final String API_KEY;
    private final RequestQueue requestQueue;

    public PaymentExtension(Context context, String apiKey) {
        this.API_KEY = apiKey;
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void createPayment(String name, String email, String amount, final PaymentCallback callback) {
        JSONObject data = new JSONObject();
        try {
            Random r = new Random();
            int id = r.nextInt(80 - 65) + 65;

            data.put("cus_name", name);
            data.put("cus_email", email);
            data.put("amount", amount);
            data.put("webhook_url", "https://bdautopay.com/?wc-api=wc_bdautopay_gateway&order_id=" + id);
            data.put("success_url", "https://bdautopay.com/success?response=success_response_data");
            data.put("cancel_url", "https://bdautopay.com/cancel?response=cancel_response_data");

        } catch (Exception e) {
            Log.e(TAG, "Error creating payment data", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, PAYMENT_CREATE_URL, data,
                response -> {
                    try {
                        String paymentUrl = response.getString("payment_url");
                        callback.onSuccess(paymentUrl);
                    } catch (Exception e) {
                        callback.onError("Error parsing payment URL: " + e.getMessage());
                    }
                },
                error -> callback.onError("Error: " + error.getMessage())) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("API-KEY", API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void verifyPayment(String transactionId, final PaymentCallback callback) {
        JSONObject data = new JSONObject();
        try {
            data.put("transaction_id", transactionId);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON data for verification: " + e.getMessage());
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, PAYMENT_VERIFY_URL, data,
                response -> {
                    try {
                        String status = response.getString("status");
                        callback.onVerifySuccess(status, response);
                    } catch (Exception e) {
                        callback.onError("Error parsing verification response: " + e.getMessage());
                    }
                },
                error -> callback.onError("Error verifying payment: " + error.getMessage())) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("API-KEY", API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public interface PaymentCallback {
        void onSuccess(String paymentUrl);

        void onVerifySuccess(String status, JSONObject response);

        void onError(String errorMessage);
    }
}