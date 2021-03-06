package goodstadt.me.uk.androidsendm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends WearableActivity {


    private BoxInsetLayout mContainerView;

    private TextView mClockView;

    private Button sendMessageButton;
    private Button yesButton;
    private Button noButton;

    private TextView counterLabel;
    private TextView replyLabel;

    private int counter = 1;

    String TAG = "SendMessage";
    private static final String APP_ACTIVITY_PATH = "/app-activity-path";

    private GoogleApiClient mGoogleApiClient;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final long CONNECTION_TIME_OUT_MS = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        counterLabel=(TextView)findViewById(R.id.counterLabel);
        replyLabel=(TextView)findViewById(R.id.replyLabel);

        counterLabel.setText(String.valueOf(counter));
        replyLabel.setText(" ");

        sendMessageButton =(Button)findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                replyLabel.setText("Sending... ");

                Map<String, String> map = new HashMap<String, String>();
                map.put("request", "\"Message " + counter + " received from the Watch\""); //double quote json string with spaces
                map.put("counter", String.valueOf(counter));

                packageAndSendMessage(APP_ACTIVITY_PATH, map);


            }
        });
        yesButton =(Button)findViewById(R.id.yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                replyLabel.setText("Sending Yes...");

                //Build message and send

                Map<String, String> map = new HashMap<String, String>();
                map.put("request", "Yes");
                map.put("counter", String.valueOf(counter));

                packageAndSendMessage(APP_ACTIVITY_PATH, map);



            }
        });


        noButton =(Button)findViewById(R.id.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                replyLabel.setText("Sending No...");

                //Build message and send

                Map<String, String> map = new HashMap<String, String>();
                map.put("request", "No");
                map.put("counter", String.valueOf(counter));

                packageAndSendMessage(APP_ACTIVITY_PATH, map);


            }
        });


//        initApi();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.e(TAG, "WATCH onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.e(TAG, "WATCH onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e(TAG, "WATCH onConnectionFailed: " + result);
                    }
                })

                .addApi(Wearable.API)  // Request access only to the Wearable API
                .build();


        // Register the local broadcast receiver, defined in step 3.
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));

        } else {
            mContainerView.setBackground(null);
        }
    }

    /**
     * Take values and package them and then send them
     *
     * @param path unique string to bind calls tigether
     * @param map dictionary of values to send to counterpart
     */
    private void packageAndSendMessage( final String path, final Map<String, String> map ) {
        new Thread( new Runnable() {
            @Override
            public void run() {

                try{

                    String s = map.toString();
                    try{


                        JSONObject mainObject = new JSONObject(s);
                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();//can use await(5, TimeUnit.SECONDS);
                        for (Node node : nodes.getNodes()) {


                            if(node.isNearby()){ //ignore cloud - assumes one wearable attached

                                MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient, node.getId(), path, mainObject.toString().getBytes()).await();

                                if (sendMessageResult.getStatus().isSuccess()) {
                                    Log.e(TAG, "Message: {" + s + "} sent to: " + node.getDisplayName());

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            replyLabel.setText(counter + " Received");
                                            counter++;
                                            counterLabel.setText(" " + counter);
                                        }
                                    });

                                }
                                else {
                                    // Log an error
                                    Log.e(TAG, "PHONE Failed to connect to Google Api Client with status "
                                            + sendMessageResult.getStatus());

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            replyLabel.setText(counter + " Failed");
                                        }
                                    });
                                }
                            }
                        }

                    }
                    catch (JSONException ex)
                    {
                        Log.e(TAG, ex.toString());
                    }



                }
                catch(Exception ex){
                    Log.e(TAG, ex.toString());
                }



            }
        }).start();
    }

    /**
     * Standard BroadcastReceiver called from ListenerService - with message as a JSON dictionary
     */
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");

            try
            {
                JSONObject mainObject = new JSONObject(message);
                String counter = mainObject.getString("counter");
                String request = mainObject.getString("request");

                replyLabel.setText(request);


            }
            catch (JSONException ex)
            {
                Log.e(TAG, ex.toString());
            }


        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (!mResolvingError) {
            mGoogleApiClient.connect(); //connect to watch
        }
    }

    @Override
    protected void onStop() {

        mGoogleApiClient.disconnect();//disconnect from watch
        super.onStop();
    }

}
