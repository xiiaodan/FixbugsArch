package com.fixbugs.android.library.logger;

import android.text.TextUtils;
import android.util.Log;
import com.dianping.logan.SendLogRunnable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

class LoggerReport extends SendLogRunnable {

    private String mUploadUrl;

    LoggerReport(String uploadUrl) {
        mUploadUrl = uploadUrl;
    }

    LoggerReport(String uploadUrl, ReportListener listener) {
        mUploadUrl = uploadUrl;
    }

    @Override
    public void sendLog(File logFile) {
        boolean success = doSendFileByAction(logFile);
        Log.d("上传日志测试", "日志上传测试结果：" + success);
    }

    private HashMap<String, String> getActionHeader() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Content-Type", "binary/octet-stream"); //二进制上传
        map.put("client", "android");
        return map;
    }

    private boolean doSendFileByAction(File logFile) {
        boolean isSuccess = false;
        try {
            FileInputStream fileStream = new FileInputStream(logFile);
            byte[] backData = doPostRequest(mUploadUrl, fileStream, getActionHeader());
            isSuccess = handleSendLogBackData(backData);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return isSuccess;
    }

    private byte[] doPostRequest(String url, InputStream inputData, Map<String, String> headerMap) {
        byte[] data = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        HttpURLConnection c = null;
        ByteArrayOutputStream back;
        byte[] Buffer = new byte[2048];
        try {
            java.net.URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            Set<Map.Entry<String, String>> entrySet = headerMap.entrySet();
            for (Map.Entry<String, String> tempEntry : entrySet) {
                c.addRequestProperty(tempEntry.getKey(), tempEntry.getValue());
            }
            c.setReadTimeout(15000);
            c.setConnectTimeout(15000);
            c.setDoInput(true);
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            outputStream = c.getOutputStream();
            int i;
            while ((i = inputData.read(Buffer)) != -1) {
                outputStream.write(Buffer, 0, i);
            }
            outputStream.flush();
            int res = c.getResponseCode();
            if (res == 200) {
                back = new ByteArrayOutputStream();
                inputStream = c.getInputStream();
                while ((i = inputStream.read(Buffer)) != -1) {
                    back.write(Buffer, 0, i);
                }
                data = back.toByteArray();
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputData != null) {
                try {
                    inputData.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (c != null) {
                c.disconnect();
            }
        }
        return data;
    }

    /**
     * 处理上传日志接口返回的数据
     */
    private boolean handleSendLogBackData(byte[] backData) throws JSONException {
        boolean isSuccess = false;
        if (backData != null) {
            String data = new String(backData);
            if (!TextUtils.isEmpty(data)) {
                JSONObject jsonObj = new JSONObject(data);
                if (jsonObj.optBoolean("success", false)) {
                    isSuccess = true;
                }
            }
        }
        return isSuccess;
    }

    public interface ReportListener {

        void onResult(boolean isSuccess);
    }
}
