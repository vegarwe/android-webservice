package no.raiom.webservice;

import android.bluetooth.BluetoothGattServerCallback;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpPutAsyncTask  extends AsyncTask<Void, Void, Void> {
    private final GattServer gattserver;
    HttpResponse response;

    private final String url = "http://raiom.no:8111";
    private String headers    = "Content-Type:application/json";
    private String body;

    public HttpPutAsyncTask(GattServer gattserver, String body) {
        this.gattserver = gattserver;
        this.body       = body;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.i("Fjase", "HttpPutAsyncTask.onPostExecute");
        gattserver.onPostExecute(response.getStatusLine().getStatusCode());
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.i("Fjase", "HttpPutAsyncTask.doInBackground " + url + " "  + headers + " " + body);

        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        for (String pairs : body.split("&")) {
            String[] pair = pairs.split("=");
            postData.add(new BasicNameValuePair(pair[0], pair[1]));
        }

        HttpPost post = new HttpPost(url);
        HttpClient conn = new DefaultHttpClient();
        try {
            post.setEntity(new UrlEncodedFormEntity(postData, "utf-8"));
            response = conn.execute(post);
            Log.i("Fjase", "getStatusLine " + response.getStatusLine());
        } catch (IOException ioe) {
            Log.e("Fjase", "wtf!");
        }
        return null;
    }
}
