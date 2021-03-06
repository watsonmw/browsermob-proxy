package org.browsermob.proxy.http;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.browsermob.proxy.util.Base64;
import org.browsermob.proxy.util.ClonedInputStream;
import org.browsermob.proxy.util.Log;
import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BrowserMobHttpRequest {
    private static final Log LOG = new Log();

    private HttpRequestBase method;
    private BrowserMobHttpClient client;
    private int expectedStatusCode;
    private String verificationText;
    private List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    private StringEntity stringEntity;
    private ByteArrayEntity byteArrayEntity;
    private InputStreamEntity inputStreamEntity;
    private MultipartEntity multipartEntity;
    private OutputStream outputStream;
    private RequestCallback requestCallback;
    private boolean collectAdditionalInfo;
    private ByteArrayOutputStream copy;
    private String expectedLocation;
    private boolean multiPart;

    protected BrowserMobHttpRequest(HttpRequestBase method, BrowserMobHttpClient client, int expectedStatusCode, boolean collectAdditionalInfo) {
        this.method = method;
        this.client = client;
        this.expectedStatusCode = expectedStatusCode;
        this.collectAdditionalInfo = collectAdditionalInfo;
    }

    public String getExpectedLocation() {
        return expectedLocation;
    }

    public void setExpectedLocation(String location) {
        this.expectedLocation = location;
    }

    public void addRequestHeader(String key, String value) {
        method.addHeader(key, value);
    }

    public void addRequestHeaders(NativeObject headersNO) {
        if (headersNO != null) {
            for (Object o : headersNO.getAllIds()) {
                Object val = headersNO.get(o.toString(), null);
                if (val instanceof NativeArray) {
                    NativeArray arr = (NativeArray) val;
                    for (int i = 0; i < arr.getLength(); i++) {
                        method.addHeader(o.toString(), arr.get(i, null).toString());
                    }
                } else {
                    method.addHeader(o.toString(), val.toString());
                }
            }
        }
    }

    public void addRequestParameter(String key, String value) {
        nvps.add(new BasicNameValuePair(key, value));
    }

    public void setRequestBody(String body, String contentType, String charSet) {
        try {
            stringEntity = new StringEntity(body, charSet);
        } catch (UnsupportedEncodingException e) {
            try {
                stringEntity = new StringEntity(body, null);
            } catch (UnsupportedEncodingException e1) {
                // this won't happen
            }
        }

        stringEntity.setContentType(contentType);
    }

    public void setRequestBody(String body) {
        setRequestBody(body, null, "UTF-8");
    }

    public void setRequestBodyAsBase64EncodedBytes(String bodyBase64Encoded) {
        byteArrayEntity = new ByteArrayEntity(Base64.base64ToByteArray(bodyBase64Encoded));
    }

    public void addRequestParameters(NativeObject paramsNO) {
        if (paramsNO != null) {
            for (Object o : paramsNO.getAllIds()) {
                Object val = paramsNO.get(o.toString(), null);
                if (val instanceof NativeArray) {
                    NativeArray arr = (NativeArray) val;
                    for (int i = 0; i < arr.getLength(); i++) {
                        addRequestParameter(o.toString(), arr.get(i, null).toString());
                    }
                } else {
                    addRequestParameter(o.toString(), val.toString());
                }
            }
        }
    }

    public void setRequestInputStream(InputStream is, long length) {
        if (collectAdditionalInfo) {
            ClonedInputStream cis = new ClonedInputStream(is);
            is = cis;
            copy = cis.getOutput();
        }

        inputStreamEntity = new InputStreamEntity(is, length);
    }


    public String getVerificationText() {
        return verificationText;
    }

    public void setVerificationText(String verificationText) {
        this.verificationText = verificationText;
    }

    public HttpRequestBase getMethod() {
        return method;
    }

    public void makeMultiPart() {
        if (!multiPart) {
            multiPart = true;
            multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        }
    }

    public BrowserMobHttpResponse execute() {
        // deal with PUT/POST requests
        if (method instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase enclodingRequest = (HttpEntityEnclosingRequestBase) method;

            if (!nvps.isEmpty()) {
                try {
                    if (!multiPart) {
                        enclodingRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                    } else {
                        for (NameValuePair nvp : nvps) {
                            multipartEntity.addPart(nvp.getName(), new StringBody(nvp.getValue()));
                        }
                        enclodingRequest.setEntity(multipartEntity);
                    }
                } catch (UnsupportedEncodingException e) {
                    LOG.severe("Could not find UTF-8 encoding, something is really wrong", e);
                }
            } else if (multipartEntity != null) {
                enclodingRequest.setEntity(multipartEntity);
            } else if (byteArrayEntity != null) {
                enclodingRequest.setEntity(byteArrayEntity);
            } else if (stringEntity != null) {
                enclodingRequest.setEntity(stringEntity);
            } else if (inputStreamEntity != null) {
                enclodingRequest.setEntity(inputStreamEntity);
            }
        }

        return client.execute(this);
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public RequestCallback getRequestCallback() {
        return requestCallback;
    }

    public void setRequestCallback(RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
    }

    public ByteArrayOutputStream getCopy() {
        return copy;
    }
}
