package com.nico.vlcfremote.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HttpUtils {

    public static abstract class XmlMogrifier<T> {
        private final Class<T> clazz;
        T object;

        XmlMogrifier(Class<T> clazz) {
            this.clazz = clazz;
        }

        void reset() {
            try {
                object = clazz.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                object = null;
            }
        }

        T getParsedObject() { return object; }

        abstract void parseValue(T object, final String key, final String value);
    }

    public static class CantCreateXmlParser extends Throwable {
        @Override
        public String getMessage() { return "Can't create an XML parser"; }
    }

    public static class CantParseXmlResponse extends Throwable {
        @Override
        public String getMessage() { return "The XML response was invalid"; }
    }

    private static XmlPullParserFactory xmlParserFactory = null;
    private static XmlPullParser createXmlParserFor(final String msg) throws CantCreateXmlParser {
        final XmlPullParser xpp;
        try {
            if (xmlParserFactory == null) xmlParserFactory = XmlPullParserFactory.newInstance();

            xpp = xmlParserFactory.newPullParser();
            if (xpp == null) throw new CantCreateXmlParser();

            xpp.setInput( new StringReader(msg) );
            return xpp;

        } catch (XmlPullParserException e) {
            throw new CantCreateXmlParser();
        }
    }

    public static <T> List<T> parseXmlList(final String xmlMsg, final String interestingTag, XmlMogrifier<T> objDeserializer)
                throws CantCreateXmlParser, CantParseXmlResponse
    {
        Log.e("ASD", xmlMsg);

        List<T> foundObjects = new ArrayList<>();
        final XmlPullParser xpp = createXmlParserFor(xmlMsg);

        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG && (xpp.getName().equals(interestingTag))) {
                    objDeserializer.reset();

                    for (int i=0; i < xpp.getAttributeCount(); ++i) {
                        objDeserializer.parseValue(objDeserializer.getParsedObject(), xpp.getAttributeName(i), xpp.getAttributeValue(i));
                    }

                    foundObjects.add(objDeserializer.getParsedObject());
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new CantParseXmlResponse();
        }

        return foundObjects;
    }

    public static <T> T parseXmlObject(final String xmlMsg, XmlMogrifier<T> objDeserializer)
            throws CantCreateXmlParser, CantParseXmlResponse
    {
        Log.e("ASD", xmlMsg);

        final XmlPullParser xpp = createXmlParserFor(xmlMsg);
        objDeserializer.reset();

        try {
            int eventType = xpp.getEventType();
            String currentTag = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = xpp.getName();
                        break;

                    case XmlPullParser.TEXT:
                        if (currentTag != null) {
                            objDeserializer.parseValue(objDeserializer.getParsedObject(), currentTag, xpp.getText());
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        currentTag = null;
                        break;
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new CantParseXmlResponse();
        }

        return objDeserializer.getParsedObject();
    }


    public static interface HttpResponseCallback {
        void onHttpResponseReceived(int httpStatusCode, final String msg);
        void onHttpConnectionFailure();
    }

    private static class HttpQueryResult {
        public String message;
        public int httpStatusCode;
        public boolean connectionFailure;
    }

    public static class AsyncRequester extends AsyncTask<Void, Void, HttpQueryResult> {
        private final HttpClient httpClient;
        private final HttpGet op;
        private final HttpResponseCallback callback;

        public AsyncRequester(HttpClient httpClient, HttpGet op, HttpResponseCallback callback) {
            this.httpClient = httpClient;
            this.op = op;
            this.callback = callback;
        }

        @Override
        protected HttpQueryResult doInBackground(Void... params) {
            HttpQueryResult res = new HttpQueryResult();
            try {
                HttpResponse resp = httpClient.execute(op);
                if (resp != null) {
                    res.httpStatusCode = resp.getStatusLine().getStatusCode();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    resp.getEntity().writeTo(out);
                    res.message = out.toString();
                    out.close();
                } else {
                    res.connectionFailure = true;
                }
            } catch (IOException e) {
                res.connectionFailure = true;
            }

            return res;
        }

        @Override
        protected void onPostExecute(final HttpQueryResult res) {
            if (res.connectionFailure) {
                callback.onHttpConnectionFailure();
            } else {
                callback.onHttpResponseReceived(res.httpStatusCode, res.message);
            }
        }
    }
}
