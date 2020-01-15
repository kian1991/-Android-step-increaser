package com.natgo.stepincreaser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {




    public static final String TAG = "StepCounter";
    public static int stepCountDelta;
    // private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    // public static GoogleApiClient mGoogleApiClient;

    // TIME AND CAL VARIABLES
    public static int year;
    public static int month;
    public static int dayOfMonth;

    public static int fromHour = 0;
    public static int fromMinute = 0;

    public static int toHour = 23;
    public static int toMinute = 59;









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*/ OLD BUT GOLD GAPICLient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();*/


        // get Calendar
        // init date
        Date today = new Date();
        year = today.getYear();
        month = today.getDate();
        dayOfMonth = today.getDay();
        CalendarView calendarView = (CalendarView) findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int yr, int mon, int dOM) {
                year = yr;
                month = mon;
                dayOfMonth = dOM;
            }
        });
        // set initial Date
        calendarView.setDate(today.getTime());

        // get buttons
        Button connect = (Button) findViewById(R.id.login);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("APP", "Well you clicked the fucking button");
                // Fitness API
                writeData();
            }
        });

        final Button from = (Button) findViewById(R.id.from);
        from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        fromHour = hourOfDay;
                        fromMinute = minute;
                    }
                }, 0, 0, true).show();
            }
        });

        Button to = (Button) findViewById(R.id.to);
        to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        toHour = hourOfDay;
                        toMinute = minute;
                    }
                }, 23, 59, true).show();
            }
        });

       setDeltaSteps();


    } // onCreate

    private void setDeltaSteps(){
        // inital stepdelta set -> Random number
        stepCountDelta = getRandomNumberInRange(7000, 15900);
        TextView stepView = (TextView) findViewById(R.id.editText);
        stepView.setText("" + stepCountDelta);
    }


    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
    private void readData() {

    }

    private void toastIt(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Write Steps to API whoooooop
     *
     */
    private void writeData(){

        // Handle Textinput
        TextView stepCountView = findViewById(R.id.editText);
        if(stepCountView.getText().toString() == Integer.toString(stepCountDelta)) {
            // Different so set stepCountDelta
            try {
                stepCountDelta = Integer.parseInt(stepCountView.getText().toString());
            }catch (Exception ex) {
                toastIt("Just numbers please...");
            }
        }


        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth, toHour, toMinute);
        Log.i(TAG, "ENDTime: " + cal.getTime().toString());
        long endTime = cal.getTimeInMillis();
        cal.set(year, month, dayOfMonth, fromHour, fromMinute);
        Log.i(TAG, "STARTTime: " + cal.getTime().toString());

        long startTime = cal.getTimeInMillis();

        // create Data source
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setStreamName("Mi Fit" + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build();

        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint =
                DataPoint.builder(dataSource)
                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                        .setField(Field.FIELD_STEPS, stepCountDelta)
                        .build();
        DataSet dataSet = DataSet.builder(dataSource).add(dataPoint).build();


        DataUpdateRequest request = new DataUpdateRequest.Builder()
                .setDataSet(dataSet)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Task<Void> response = Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)).updateData(request);

        // Complete
        response.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

            }
        });

        // Success
        response.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "SUCCESS");
                toastIt("Success! Generating new number...");
                setDeltaSteps();
            }
        });

        // Fail
        response.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, e.toString());
            }
        });

    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}
