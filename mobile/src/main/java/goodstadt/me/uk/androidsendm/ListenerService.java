package goodstadt.me.uk.androidsendm;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by johng on 27/10/15.
 */


public class ListenerService extends WearableListenerService {

    String TAG = "SendMessage";
    private static final String START_ACTIVITY_PATH = "/app-activity-path";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(START_ACTIVITY_PATH))
        {
            final String message = new String(messageEvent.getData());
            Log.e(TAG, "MOBILE Message path received on watch is: " + messageEvent.getPath());
            Log.e(TAG, "MOBILE Message received on watch is: " + message);

            try
            {
                JSONObject mainObject = new JSONObject(message);
                String counter = mainObject.getString("counter");
                String request = mainObject.getString("request");

                Log.e(TAG, "");
            }
            catch (JSONException ex)
            {
                Log.e(TAG, ex.toString());
            }


            // Broadcast message to wearable activity for display
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

        }
        else {
            Log.e(TAG, "MOBILE Message path received FAILED path watch is: " + messageEvent.getPath());
            super.onMessageReceived(messageEvent);
        }
    }
}

