package com.codecollection.bdautopay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import org.json.JSONObject;

import java.util.logging.LogRecord;

public class bdautopay extends WebView {

    private static final String TAG = "BDPayService";
    private PaymentListener paymentListener;
    private PaymentExtension payService;
    private ProgressBar loadingProgressBar; // Default loading indicator
    private WebView webView;
    private CardView loadingCardView;
    private TextView loadingTextView;
    private Handler handler;

    public bdautopay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Initialize RelativeLayout
        RelativeLayout relativeLayout = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        addView(relativeLayout, layoutParams);

        // Initialize WebView
        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setVisibility(View.GONE); // Initially hide the WebView

        // Set WebView LayoutParams
        RelativeLayout.LayoutParams webViewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        relativeLayout.addView(webView, webViewLayoutParams);

        // Create a CardView to hold ProgressBar and Loading text
        loadingCardView = new CardView(context);
        loadingCardView.setCardElevation(8);
        loadingCardView.setRadius(16);
        loadingCardView.setContentPadding(32, 32, 32, 32);
        loadingCardView.setVisibility(View.VISIBLE); // Show the loading card initially

        // Create a LinearLayout to hold the ProgressBar and TextView vertically
        LinearLayout loadingLayout = new LinearLayout(context);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Create the circular ProgressBar
        loadingProgressBar = new ProgressBar(context);
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setVisibility(View.VISIBLE);

        // Create the loading TextView
        loadingTextView = new TextView(context);
        loadingTextView.setText("Loading...");
        loadingTextView.setGravity(Gravity.CENTER);
        loadingTextView.setTextSize(16);

        // Add ProgressBar and TextView to the LinearLayout
        loadingLayout.addView(loadingProgressBar);
        loadingLayout.addView(loadingTextView);

        // Add loading layout to the CardView
        loadingCardView.addView(loadingLayout);

        // Set CardView LayoutParams
        RelativeLayout.LayoutParams cardViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        cardViewParams.addRule(RelativeLayout.CENTER_IN_PARENT); // Center the CardView

        // Add the CardView to the RelativeLayout
        relativeLayout.addView(loadingCardView, cardViewParams);

        handler = new Handler();
    }

    public void initialize(Activity context, String apiKey) {
        payService = new PaymentExtension(context, apiKey);
    }

    public void hide() {
        loadingCardView.setVisibility(GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void startPayment(String name, String email, String amount) {
        // Show loading indicator when starting the payment process
        loadingCardView.setVisibility(VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);

                if (url.contains("bdautopay.com/success")) {
                    handlePaymentSuccess(url);
                    return true;
                } else if (url.contains("bdautopay.com/cancel")) {
                    handlePaymentFailure(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Page started loading: " + url);
                loadingCardView.setVisibility(View.VISIBLE); // Show ProgressBar when page starts loading
                webView.setVisibility(View.GONE); // Hide WebView while the page is loading
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished loading: " + url);
                handler.postDelayed(() -> hide(), 1000);
                webView.setVisibility(View.VISIBLE); // Show WebView when page is fully loaded
            }
        });

        // Call the createPayment method from BDAutoPay to initiate the payment
        payService.createPayment(name, email, amount, new PaymentExtension.PaymentCallback() {
            @Override
            public void onSuccess(String paymentUrl) {
                Log.d(TAG, "Received Payment URL: " + paymentUrl);
                if (paymentUrl != null && !paymentUrl.isEmpty()) {
                    webView.loadUrl(paymentUrl);  // Load the payment page in WebView
                } else {
                    Log.e(TAG, "Payment URL is empty.");
                }
            }

            @Override
            public void onVerifySuccess(String status, JSONObject response) {
                Log.d(TAG, "Verification Response: " + response.toString());
                if (paymentListener != null) {
                    paymentListener.onPaymentVerified(status, response);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Payment Error: " + errorMessage);
                if (paymentListener != null) {
                    paymentListener.onPaymentError(errorMessage);
                }
            }
        });
    }

    private void handlePaymentSuccess(String url) {
        Log.d(TAG, "Payment was successful!");
        String transactionId = getQueryParam(url, "transactionId");
        if (transactionId != null) {
            payService.verifyPayment(transactionId, new PaymentExtension.PaymentCallback() {
                @Override
                public void onSuccess(String paymentUrl) {
                    // Not required here
                }

                @Override
                public void onVerifySuccess(String status, JSONObject response) {
                    if (paymentListener != null) {
                        paymentListener.onPaymentVerified(status, response);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (paymentListener != null) {
                        paymentListener.onPaymentError(errorMessage);
                    }
                }
            });
        } else {
            Log.e(TAG, "Transaction ID is missing in the URL");
        }
    }

    private void handlePaymentFailure(String url) {
        Log.d(TAG, "Payment was cancelled: " + url);
        if (paymentListener != null) {
            paymentListener.onPaymentCancelled(url);
        }
    }

    // Method to handle user-initiated payment cancellation
    public void cancelPayment() {
        Log.d(TAG, "User cancelled the payment");
        if (paymentListener != null) {
            paymentListener.onPaymentCancelled("User cancelled the payment");
        }
    }

    // Extract query parameters from URL
    private String getQueryParam(String url, String param) {
        Uri uri = Uri.parse(url);
        return uri.getQueryParameter(param);
    }

    // Set payment listener
    public void setPaymentListener(PaymentListener listener) {
        this.paymentListener = listener;
    }

    // Interface for payment callbacks
    public interface PaymentListener {
        void onPaymentVerified(String status, JSONObject response);

        void onPaymentCancelled(String url);

        void onPaymentError(String errorMessage);
    }
}