package edu.northwestern.socialmedia.hestondissertationtest;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED) {
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent intent = new Intent(this, OutgoingSMSReceiver.class);
        startService(intent);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            AppDatabase db = Database.getDb(getApplicationContext());
            long messageId = incomingIntent.getLongExtra("message_id", -1);
            if (messageId != -1) {
                Message msg = db.messageDao().getById(Long.toString(messageId));

                // Capture the layout's TextView and set the string as its text
                TextView textView = findViewById(R.id.message_container);
                textView.setText(msg.getMessageText());

                TextView contactContainer = findViewById(R.id.contactContainer);
                contactContainer.setText(msg.getMessageFromName());

                TextView receivedMessageContainer = findViewById(R.id.receivedMsgContainer);
                receivedMessageContainer.setText(msg.getInResponseTo());

                TextView dateContainer = findViewById(R.id.dateContainer);
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                String reportDate = df.format(msg.getReceivedAt());
                dateContainer.setText(reportDate);
            }
        }
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {

        AppDatabase db = Database.getDb(getApplicationContext());

        RatingBar availabilityRating = (RatingBar) findViewById(R.id.availabilityRating);
        int availability = Math.round(availabilityRating.getRating());

        RatingBar urgencyRating = (RatingBar) findViewById(R.id.urgencyRating);
        int urgency = Math.round(urgencyRating.getRating());

        RatingBar friendUrgencyRating = (RatingBar) findViewById(R.id.urgencyRating2);
        int friendUrgency = Math.round(friendUrgencyRating.getRating());

        CheckBox availabilityCheck = (CheckBox) findViewById(R.id.availabilityCheckBox);
        int availabilityBinary = availabilityCheck.isChecked() ? 1 : 0;

        Intent incomingIntent = getIntent();
        int messageId = (int) incomingIntent.getLongExtra("message_id", -1);

        SurveyResult result = new SurveyResult();
        result.setAvailability(availability);
        result.setUrgency(urgency);
        result.setFriendUrgency(friendUrgency);
        result.setUnavailable(availabilityBinary);
        result.setMessageId(messageId);


        db.messageDao().makeHandled(messageId);
        Message msg = db.messageDao().getById(Integer.toString(messageId));
        WebPoster.PostMessage(this, msg);
        db.surveyResultDao().insert(result);
        WebPoster.PostSurveyResult(this, result);

        Intent intent = new Intent(this, ThankYouActivity.class);
        startActivity(intent);
    }

    public void cancelMessage(View view) {
        AppDatabase db = Database.getDb(getApplicationContext());

        Intent incomingIntent = getIntent();
        int messageId = (int) incomingIntent.getLongExtra("message_id", -1);
        db.messageDao().makeHandled(messageId);

        Intent intent = new Intent(this, ThankYouActivity.class);
        startActivity(intent);
    }
}

