package com.derek.v2monitor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.derek.v2monitor.model.BookInfo;
import com.derek.v2monitor.model.BookingResult;
import com.derek.v2monitor.model.UserInfo;
import com.derek.v2monitor.utils.DealStrSubUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by derek on 2018/3/19.
 */

public class SingleTask implements okhttp3.Callback {
    private String TAG = this.getClass().getSimpleName();
    private final String DEFAULT_LANGUAGE = "eng";

    private final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private final String URL_GET_TYPE = "http://183.6.175.51:8000/xb/xbywyy/selectType.jsp";
    private final String URL_SEARCH = "http://183.6.175.51:8000/xb/xbywyy/queryXbywzhBySerialNumber.do";
    private final String URL_GET_CODE = "http://183.6.175.51:8000/xb/validcode/createValidateCode.do";
    private final String URL_POST_INFO = "http://183.6.175.51:8000/xb/xbywyy/queryXbywyy.do";
    private final String URL_BOOKING_LIST = "http://183.6.175.51:8000/xb/xbywyy/bookingList.jsp";
    private final String URL_GET_LAST_CODE = "http://183.6.175.51:8000//xb/login/yyvalidateCode.do?i=1";
    private final String URL_BOOKING = "http://183.6.175.51:8000/xb/xbywyy/wsyy.do";

    private String userName = "derekcz";
    private String userPwd = "2015Wgzl@";
    private String userToken = "derekcz";

    private Activity activity;
    private OkHttpClient client;
    private OkHttpClient commitClient;
    private TessBaseAPI tessBaseAPI;
    private Callback callback;

    private int step;

    private String id;
    private String cookie;
    private UserInfo userInfo;
    private String imageCode;
    private BookInfo bookInfo;

    private long lastQueryTime;

    private Call cookieCall;
    private Call queryInfoCall;
    private Call getImgCodeCall;
    private Call postInoCall;
    private Call queryListCall;


    interface Callback {
        void onReady(SingleTask singleTask);

        void onDoneQuery(SingleTask singleTask, long lastQueryTime);

        void onFindBooking(List<BookInfo> list);

        void onBooking(String id, BookingResult bookingResult);
    }

    public SingleTask(Activity activity, OkHttpClient client, OkHttpClient commitClient, String dataPath, String id, Callback callback) {
        this.activity = activity;
        this.client = client;
        this.commitClient = commitClient;
        //this.tessBaseAPI = tessBaseAPI;
        this.id = id;
        this.callback = callback;

        this.tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(dataPath, DEFAULT_LANGUAGE);
    }

    public void execute() {
        handleStep(1);
    }

    private void handleStep(int step) {
        switch (step) {
            case 1:
                getCookie();
                break;
            case 2:
                queryInfo();
                break;
            case 3:
                getImageCode();
                break;
            case 4:
                postInfo();
                break;
            case 5:
                callback.onReady(this);
                break;
            case 6:
                realCommit(bookInfo, false);
                break;
        }
    }

    // step 1: 获取cookie
    private void getCookie() {
        step = 1;
        Logger.d(TAG, getLogMsg("Step 1: 获取cookie"));
        Request request = new Request.Builder()
            .url(URL_GET_TYPE)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .tag(new Tag(hashCode(), 1))
            .build();
        cookieCall = client.newCall(request);
        cookieCall.enqueue(this);
    }

    // step 2: 查询信息
    private void queryInfo() {
        step = 2;
        Logger.d(TAG, getLogMsg("Step 2: 查询信息"));
        RequestBody body = RequestBody.create(FORM, "sblsh=" + id);
        Request request = new Request.Builder()
            .url(URL_SEARCH)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Cookie", cookie)
            .post(body)
            .tag(new Tag(hashCode(), 2))
            .build();
        queryInfoCall = client.newCall(request);
        queryInfoCall.enqueue(this);
    }

    // step 3: 获取验证码
    private void getImageCode() {
        step = 3;
        Logger.d(TAG, getLogMsg("Step 3: 获取验证码"));
        Request request = new Request.Builder()
            .url(URL_GET_CODE)
            .addHeader("Accept", " image/webp,image/apng,image/*,*/*;q=0.8")
            .addHeader("Cookie", cookie)
            .tag(new Tag(hashCode(), 3))
            .build();
        getImgCodeCall = client.newCall(request);
        getImgCodeCall.enqueue(this);
    }

    // step 4: 提交信息
    private void postInfo() {
        step = 4;
        Logger.d(TAG, getLogMsg("Step 4: 提交信息"));
        StringBuilder sb = new StringBuilder();
        if (TextUtils.isEmpty(userInfo.getSfzmhm())) {
            return;
        }
        if (userInfo.getSfzmhm().length() == 18) {
            sb.append("zjmc=A").append("&");
        } else {
            sb.append("zjmc=B").append("&");
        }
        sb.append("zjhm=").append(userInfo.getSfzmhm()).append("&");
        sb.append("xm=").append(userInfo.getGsmc()).append("&");
        sb.append("sjhm=13750022597&");
        sb.append("sh=").append(userInfo.getSydjhm()).append("&");
        sb.append("ywlx=0201&ywlb=02010101&startRec=1&endRec=&");
        sb.append("captcha=").append(imageCode);

        RequestBody body = RequestBody.create(FORM, sb.toString());
        Request request = new Request.Builder()
            .url(URL_POST_INFO)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Cookie", cookie)
            .post(body)
            .tag(new Tag(hashCode(), 4))
            .build();
        postInoCall = client.newCall(request);
        postInoCall.enqueue(this);
    }

    // step 5: 搜索list
    void queryList() {
        step = 5;
        lastQueryTime = System.currentTimeMillis();
        Logger.d(TAG, getLogMsg("Step 5: 搜索BookingList"));
        Request request = new Request.Builder()
            .url(URL_BOOKING_LIST)
            .addHeader("Cookie", cookie)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .tag(new Tag(hashCode(), 5))
            .build();
        queryListCall = client.newCall(request);
        queryListCall.enqueue(this);
    }

    // step 6: 获取图片验证码
    void commit(BookInfo bookInfo) {
        realCommit(bookInfo, true);
    }

    private void realCommit(BookInfo bookInfo, boolean needCheck) {
        if (needCheck && step >= 6) {
            return;
        }
        step = 6;
        if (cookieCall != null && !cookieCall.isCanceled()) {
            cookieCall.cancel();
        }
        if (queryInfoCall != null && !queryInfoCall.isCanceled()) {
            queryInfoCall.cancel();
        }
        if (getImgCodeCall != null && !getImgCodeCall.isCanceled()) {
            getImgCodeCall.cancel();
        }
        if (postInoCall != null && !postInoCall.isCanceled()) {
            postInoCall.cancel();
        }
        if (queryListCall != null && !queryListCall.isCanceled()) {
            queryListCall.cancel();
        }

        Logger.d(TAG, getLogMsg("Step 6: 获取图片验证码"));
        this.bookInfo = bookInfo;
        Request request = new Request.Builder()
            .url(URL_GET_LAST_CODE)
            .addHeader("Accept", " image/webp,image/apng,image/*,*/*;q=0.8")
            .addHeader("Cookie", cookie)
            .build();
        commitClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e(TAG, getLogMsg("提交-获取图片验证码 ==>> "), e);
                callback.onDoneQuery(SingleTask.this, 0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                byte[] imageBytes = getStreamBytes(response.body().byteStream());
                if (imageBytes != null) {
                    recognizeCode(imageBytes);
                } else {
                    callback.onDoneQuery(SingleTask.this, 0);
                }
            }
        });
    }

    // step 7: 识别图片验证码
    private void recognizeCode(byte[] file) {
        step = 7;
        Logger.d(TAG, getLogMsg("Step 7: 识别图片验证码"));
        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody body = RequestBody.create(MediaType.parse("image/*"), file);
        requestBody.addFormDataPart("upload", "xxx.jpg", body);

        requestBody.addFormDataPart("user_name", userName);
        requestBody.addFormDataPart("user_pw", userPwd);
        requestBody.addFormDataPart("zztool_token", userToken);
        requestBody.addFormDataPart("yzm_minlen", String.valueOf(6));
        requestBody.addFormDataPart("yzm_maxlen", String.valueOf(6));
        requestBody.addFormDataPart("yzmtype_mark", String.valueOf(0));

        final Request request = new Request.Builder()
            .url("http://v1-http-api.jsdama.com/api.php?mod=php&act=upload")
            .post(requestBody.build())
            .build();
        commitClient.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e(TAG, getLogMsg("识别图片验证码失败 ==>> "), e);
                callback.onDoneQuery(SingleTask.this, 0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Logger.d(TAG, getLogMsg("识别图片验证码 ==>> " + result));
                JSONObject jsonObject = JSONObject.parseObject(result);
                if ("true".equals(jsonObject.getString("result"))) {
                    final String code = jsonObject.getJSONObject("data").getString("val").toLowerCase();
                    finalCommit(code);
                } else {
                    callback.onDoneQuery(SingleTask.this, 0);
                }
            }
        });
    }

    // step 8:
    private void finalCommit(String code) {
        step = 8;
        Logger.d(TAG, getLogMsg("Step 8: 提交预约"));
        final RequestBody body = RequestBody.create(FORM,
            "peid=" + bookInfo.getId()
                + "&bookingDate=" + bookInfo.getDate()
                + "&bookingTime=" + bookInfo.getTime().replace(":", "%3A")
                + "&yyvalidateCode=" + code);
        Request request = new Request.Builder()
            .url(URL_BOOKING)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Cookie", cookie)
            .post(body)
            .build();
        commitClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e(TAG, getLogMsg("finalCommit ==>> "), e);
                callback.onDoneQuery(SingleTask.this, 0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                BookingResult bookingResult = JSON.parseObject(response.body().string(), BookingResult.class);
                Logger.d(TAG, getLogMsg("预约结果 ==>> " + (bookingResult == null ? "null" : bookingResult.toString())));
                if (bookingResult == null || bookingResult.getDetail().contains("验证码不正确！")) {
                    handleStep(6);
                } else if ("success".equals(bookingResult.getType())) {
                    callback.onBooking(id, bookingResult);
                } else {
                    callback.onDoneQuery(SingleTask.this, 0);
                }
            }
        });
    }

    @Override
    public void onFailure(Call call, IOException e) {
        if (isMyRequest(call.request().tag())) {
            switch (((Tag) call.request().tag()).step) {
                case 1:
                    Logger.e(TAG, getLogMsg("get cookie ==>> "), e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    handleStep(1);
                    break;
                case 2:
                    Logger.e(TAG, getLogMsg("查询流水号信息 ==>> "), e);
                    handleStep(2);
                    break;
                case 3:
                    Logger.e(TAG, getLogMsg("获取验证码 ==>> "), e);
                    handleStep(3);
                    break;
                case 4:
                    Logger.e(TAG, getLogMsg("提交信息 ==>> "), e);
                    handleStep(3);
                    break;
                case 5:
                    Logger.e(TAG, getLogMsg("搜索list ==>> "), e);
                    callback.onDoneQuery(this, lastQueryTime);
                    break;
            }
        }
    }

    @Override
    public void onResponse(Call call, final Response response) throws IOException {
        if (isMyRequest(call.request().tag())) {
            switch (((Tag) call.request().tag()).step) {
                case 1:
                    cookie = response.header("Set-Cookie").split(";")[0];
                    Logger.d(TAG, getLogMsg("cookie ==>> " + cookie));
                    handleStep(2);
                    break;
                case 2: {
                    String result = response.body().string();
                    userInfo = JSON.parseObject(result, UserInfo.class);
                    Logger.d(TAG, getLogMsg("流水号信息 ==>> " + userInfo));
                    if (userInfo.isValidate()) {
                        handleStep(3);
                    } else {
                        handleStep(2);
                    }
                    break;
                }
                case 3:
                    Logger.d(TAG, getLogMsg("获取验证码"));
                    Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    tessBaseAPI.setImage(bitmap);
                    imageCode = tessBaseAPI.getUTF8Text().replace(" ", "");
                    if (!TextUtils.isEmpty(imageCode) && imageCode.length() == 4) {
                        handleStep(4);
                    } else {
                        handleStep(3);
                    }
                    break;
                case 4: {
                    String result = response.body().string();
                    Logger.d(TAG, getLogMsg("提交信息 ==>> " + result));
                    if (result.contains("success")) {
                        handleStep(5);
                    } else {
                        handleStep(3);
                    }
                    break;
                }
                case 5:
                    String result = response.body().string();
                    if (result.contains("请不要进行非法操作")) {
                        handleStep(1);
                    } else if (result.contains("<span>预约</span>")) {
                        List<BookInfo> list = DealStrSubUtils.getTimeInfoList(result, "javascript:booking(.*?);");
                        Logger.d(TAG, getLogMsg("发现可预约 ==>> ") + list.size());
                        if (list.size() > 0) {
                            callback.onFindBooking(list);
                        } else {
                            callback.onDoneQuery(this, lastQueryTime);
                        }
                        return;
                    } else {
                        callback.onDoneQuery(this, lastQueryTime);
                    }
                    break;
            }
        }
    }

    private boolean isMyRequest(Object tag) {
        return tag instanceof Tag && ((Tag) tag).hashCode == hashCode();
    }

    String getLogMsg(String msg) {
        return "[" + id + "]: " + msg;
    }

    private byte[] getStreamBytes(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] b = baos.toByteArray();
            is.close();
            baos.close();
            return b;
        } catch (IOException e) {
            Log.e(TAG, "getStreamBytes ==>>", e);
        }
        return null;
    }

    static class Tag {
        int hashCode;
        int step;

        Tag(int hashCode, int step) {
            this.hashCode = hashCode;
            this.step = step;
        }
    }
}
