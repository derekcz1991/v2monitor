package com.derek.v2monitor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.derek.v2monitor.model.BookInfo;
import com.derek.v2monitor.model.BookingResult;
import com.derek.v2monitor.utils.FileUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    private ConcurrentLinkedQueue<SingleTask> taskQueue;
    private ArrayList<SingleTask> tasks;

    private Handler looperHandler;
    private long lastPollTime;
    private long timeDelta;
    //private Handler handler = new Handler();

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

        //queue = new ArrayBlockingQueue<>(20);
        taskQueue = new ConcurrentLinkedQueue<>();
        tasks = new ArrayList<>();

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtn.setEnabled(false);
                input.setEnabled(false);

                String[] idArray = input.getText().toString().split("\n");
                for (String id : idArray) {
                    SingleTask singleTask = new SingleTask(MainActivity.this, client, commitClient, DATA_PATH, id, MainActivity.this);
                    tasks.add(singleTask);
                }

                new Thread(new LooperTask()).start();

                for (SingleTask singleTask : tasks) {
                    singleTask.execute();
                }

                looperHandler.sendEmptyMessage(1);
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logText.setText("");
            }
        });
    }

    class LooperTask implements Runnable {

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();

            looperHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            SingleTask peek = taskQueue.poll();
                            if (peek != null) {
                                lastPollTime = System.currentTimeMillis();
                                peek.queryList();
                            }
                            timeDelta = 500 - (System.currentTimeMillis() - lastPollTime);
                            if (timeDelta <= 0) {
                                looperHandler.sendEmptyMessage(1);
                            } else {
                                looperHandler.sendEmptyMessageDelayed(1, timeDelta);
                            }
                            break;
                        case 2:
                            taskQueue.offer((SingleTask) msg.obj);
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }

    private Message getTaskMessage(SingleTask singleTask) {
        Message msg = Message.obtain();
        msg.what = 2;
        msg.obj = singleTask;
        return msg;
    }

    @Override
    public void onReady(SingleTask singleTask) {
        looperHandler.sendMessage(getTaskMessage(singleTask));
    }

    @Override
    public void onDoneQuery(final SingleTask singleTask, long lastQueryTime) {
        if (lastQueryTime == 0) {
            looperHandler.sendMessage(getTaskMessage(singleTask));
        } else {
            long delta = 5000 - (System.currentTimeMillis() - lastQueryTime);
            if (delta <= 0) {
                looperHandler.sendMessage(getTaskMessage(singleTask));
            } else {
                looperHandler.sendMessageDelayed(getTaskMessage(singleTask), delta);
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
