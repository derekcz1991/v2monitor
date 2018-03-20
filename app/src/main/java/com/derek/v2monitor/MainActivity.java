package com.derek.v2monitor;

import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.derek.v2monitor.model.BookInfo;
import com.derek.v2monitor.model.BookingResult;
import com.derek.v2monitor.utils.DealStrSubUtils;
import com.derek.v2monitor.utils.FileUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements SingleTask.Callback {
    private String TAG = this.getClass().getSimpleName();

    private final String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private final String tessdata = DATA_PATH + File.separator + "tessdata";
    private final String DEFAULT_LANGUAGE = "eng";
    private final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    private final String LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME;

    private OkHttpClient client;
    private OkHttpClient commitClient;
    private TessBaseAPI tessBaseAPI;

    private Button startBtn;
    private Button clearBtn;
    private EditText input;
    private TextView logText;

    private ArrayBlockingQueue<SingleTask> queue;
    private ArrayList<SingleTask> tasks;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.start_btn);
        clearBtn = findViewById(R.id.clear_btn);
        input = findViewById(R.id.input);
        logText = findViewById(R.id.logText);
        logText.setMovementMethod(new ScrollingMovementMethod());

        client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        commitClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        FileUtils.copyToSD(this, LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(DATA_PATH, DEFAULT_LANGUAGE);

        Logger.setCallback(new Logger.Callback() {
            @Override
            public void onLog(final String log) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logText.append(log);
                        logText.append("\n");
                    }
                });
            }
        });

        queue = new ArrayBlockingQueue<>(20);
        tasks = new ArrayList<>();

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtn.setEnabled(false);
                input.setEnabled(false);

                String[] idArray = input.getText().toString().split("\n");
                for (String id : idArray) {
                    SingleTask singleTask = new SingleTask(client, commitClient, tessBaseAPI, id, MainActivity.this);
                    tasks.add(singleTask);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                printThread("take");
                                queue.take().queryList();
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Logger.e(TAG, "queue error ==>> ", e);
                            }
                        }
                    }
                }).start();
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logText.setText("");
            }
        });
    }

    @Override
    public void onReady(SingleTask singleTask) {
        printThread("onReady");
        queue.offer(singleTask);
    }

    @Override
    public void onDoneQuery(final SingleTask singleTask, long lastQueryTime) {
        if (lastQueryTime == 0) {
            printThread("onDoneQuery 1");
            queue.offer(singleTask);
        } else {
            long delta = 5000 - (System.currentTimeMillis() - lastQueryTime);
            if (delta <= 0) {
                printThread("onDoneQuery 2");
                queue.offer(singleTask);
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        printThread("onDoneQuery 3");
                        queue.offer(singleTask);
                    }
                }, delta);
            }
        }

    }

    @Override
    public void onFindBooking(List<BookInfo> list) {
        for (SingleTask task : tasks) {
            task.commit(list.get(0));
        }
    }

    @Override
    public void onBooking(String id, BookingResult bookingResult) {
        //Logger.d(TAG, "Booking result ==>> " + "[" + id + "]: " + bookingResult.toString());
    }

    private void printThread(String method) {
        Log.d("MyThread", method + ": " + Thread.currentThread().getName());
    }
}
