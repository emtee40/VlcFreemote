package com.nico.vlcfremote;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.nico.vlcfremote.utils.HttpUtils;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void zdfs(View view) {
        final TextView tv = (TextView) findViewById(R.id.textoFeo);
        new VlcConnector("http://192.168.1.5:8080/", "qwepoi").getDirList("/home/laptus");
    }

    private static class VlcConnector {
        final static String DIR_LIST_ACTION = "requests/browse.xml?dir=";

        final String urlBase;
        final String authStr;
        final HttpClient httpClient = new DefaultHttpClient();

        VlcConnector(final String url, final String pass) {
            urlBase = url;
            authStr = "Basic " + Base64.encodeToString((":"+pass).getBytes(), Base64.DEFAULT);
        }

        void getDirList(final String path) {
            HttpGet getOp = new HttpGet(urlBase + DIR_LIST_ACTION + path);
            getOp.addHeader("Authorization", authStr);
            new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
                @Override
                public void responseReceived(final String msg) {
                    String[] interestingAttrs = {"path", "name", "type"};
                    List<String[]> dirList = HttpUtils.parseXmlList(msg, "element", interestingAttrs);
                    for (String[] s : dirList) {
                        Log.i("ASD", s[0]);
                    }
                    Log.i("ASD", msg);
                }
            }).execute();
        }
    }


}
