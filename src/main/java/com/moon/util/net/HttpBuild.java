package com.moon.util.net;

import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * 更新:
 * Date  : 2019/01/15 17:02
 * <p> 新增请求完成以后的资源关闭
 * Date  : 2019/01/08 11:48
 * <p> 完成build构造器
 * <p>
 * Author : moon
 * Date  : 2019/01/01 06:21
 * Description :
 * Class for httpclient 4.5.x 之前写的httpUtil调用过于麻烦,基于链式调用(build模式)重写的httpclient工具类,支持SSL
 * <p>
 * TODO:
 * 1.默认值参数的修改
 * 2.附带证书的https请求
 */
public class HttpBuild {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBuild.class);

    private CloseableHttpClient httpClient;
    private final Map<String, String> header = new HashMap<>();
    private final Map<String, String> params = new HashMap<>();
    private String url;
    private boolean methodGet = true; //默认为Get方法
    private boolean closeHttpClient = true; //请求完成后是否关闭实例,默认关闭
    private String paramStr;

    private static final String DEFAULT_CHARSET = "UTF-8"; //默认请求编码
    private static final int DEFAULT_SOCKET_TIMEOUT = 5000; //默认等待响应时间
    private static final int DEFAULT_RETRY_TIMES = 0; //默认执行重试次数
    private static final int DEFAULT_REQUEST_TIMEOUT = 1000; //从connect manager获取connection超时时间


    /**
     * 返回结果封装静态内部类
     */
    @Data
    public static class HttpResult implements Serializable {
        private Integer code;
        private String data;

        public HttpResult(Integer code, String data) {
            this.code = code;
            this.data = data;
        }
    }

    /*******************************************************************************************************************
     *                                             Build构造                                                           *
     *****************************************************************************************************************/

    /**
     * 设置httpclient
     *
     * @param httpClient
     * @return
     */
    public HttpBuild client(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * 设置请求完成后是否关闭实例
     */
    public HttpBuild closeHttpClient(boolean flag) {
        this.closeHttpClient = flag;
        return this;
    }

    /**
     * 设置请求参数
     *
     * @param param
     * @return
     */
    public HttpBuild paramStr(String param) {
        this.paramStr = param;
        return this;
    }

    /**
     * 设置header头信息
     *
     * @param key
     * @param value
     * @return
     */
    public HttpBuild header(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public HttpBuild headers(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            this.header.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Get请求
     *
     * @param url
     * @return
     */
    public HttpBuild get(String url) {
        this.methodGet = true;
        this.url = url;
        return this;
    }

    /**
     * post请求
     *
     * @param url
     * @return
     */
    public HttpBuild post(String url) {
        this.methodGet = false;
        this.url = url;
        return this;
    }

    /**
     * 设置请求参数
     *
     * @return
     */
    public HttpBuild setParam(String key, String value) {
        this.params.put(key, value);
        return this;
    }


    /**
     * 设置请求参数
     *
     * @param params
     * @return
     */
    public HttpBuild setParams(Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            this.params.put(entry.getKey(), entry.getValue());
        }
        return this;
    }


    /*******************************************************************************************************************
     *                                            执行http请求                                                           *
     *****************************************************************************************************************/

    /**
     * 执行请求
     * try {
     *      HttpResult execute = new HttpBuild().closeHttpClient(false)
     *                               .setParam("name", "lisi")
     *                               .header("token", "a21sk389fsdc8b3n12=")
     *                               .post("http://www.xxx.com/test")
     *                               .execute();
     *          if (execute.getCode()==200){
     *              String data = execute.getData();
     *              System.out.println(data);
     *          }
     *      } catch (Exception e) {
     *          e.printStackTrace();
     *      }
     *
     * @return
     */
    public HttpResult execute() throws Exception {
        if (this.url == null || "".equals(this.url)) {
            LOG.info("当前传入URL无法成功解析:" + this.url);
            return null;
        }
        CloseableHttpResponse httpResponse = null;
        try {
            if (methodGet) { //get
                httpResponse = execeteGetResponse();
                return getResult(httpResponse, DEFAULT_CHARSET);
            } else { //post
                httpResponse = executePostResponse();
                return getResult(httpResponse, DEFAULT_CHARSET);
            }
        } finally {
            closeResource(httpResponse);
        }
    }


    /**
     * 获取Get响应
     *
     * @throws Exception
     */
    private CloseableHttpResponse execeteGetResponse() throws Exception {
        if (this.httpClient == null)
            this.httpClient = closeableHttpClient();
        URIBuilder uriBuilder = new URIBuilder(this.url);
        if (this.params != null && !params.isEmpty()) {
            List<NameValuePair> list = mapToParams(this.params);
            uriBuilder.addParameters(list);
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        //设置头信息
        if (this.header != null && !this.header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                httpGet.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return this.httpClient.execute(httpGet);
    }

    /**
     * 获取Post响应
     *
     * @throws Exception
     */
    private CloseableHttpResponse executePostResponse() throws Exception {
        if (this.httpClient == null)
            this.httpClient = closeableHttpClient();
        HttpPost httpPost = new HttpPost(this.url);
        HttpEntity entity = null;
        if (this.params != null && !this.params.isEmpty())
            entity = getEntity(this.params, DEFAULT_CHARSET);
        if (this.paramStr != null && !"".equals(this.paramStr))
            entity = getEntity(this.paramStr, DEFAULT_CHARSET);
        if (entity != null)
            httpPost.setEntity(entity);
        return this.httpClient.execute(httpPost);
    }


    /*******************************************************************************************************************
     *                                             工具方法                                                           *
     *****************************************************************************************************************/


    /**
     * 资源关闭
     *
     * @param response
     */
    private void closeResource(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.closeHttpClient && this.httpClient != null) {
            try {
                this.httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 根据参数获取请求的Entity
     *
     * @param param
     * @param charSet
     * @return
     * @throws Exception
     */
    private static HttpEntity getEntity(Object param, String charSet) throws Exception {
        if (param == null)
            return null;
        if (Map.class.isInstance(param)) { //当前是map数据
            @SuppressWarnings("unchecked")
            Map<String, String> paramsMap = (Map<String, String>) param;
            List<NameValuePair> pairList = mapToParams(paramsMap);
            UrlEncodedFormEntity httpEntity = new UrlEncodedFormEntity(pairList, charSet);
            httpEntity.setContentType(ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            return httpEntity;
        } else if (String.class.isInstance(param)) {
            String str = (String) param;
            StringEntity httpEntity = new StringEntity(str, charSet);
            if (str.startsWith("{")) //json数据
                httpEntity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            else if (str.startsWith("<"))  //xml数据
                httpEntity.setContentType(ContentType.APPLICATION_XML.getMimeType());
            else
                httpEntity.setContentType(ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            return httpEntity;
        } else {
            LOG.info("当前传入参数属于不能识别信息,无法生成org.apache.http.HttpEntity");
            return null;
        }
    }


    /**
     * Map --> List<NameValuePair>
     *
     * @param paramsMap
     * @return
     */
    private static List<NameValuePair> mapToParams(Map<String, String> paramsMap) {
        List<NameValuePair> list = new ArrayList<>();
        if (paramsMap == null || paramsMap.isEmpty())
            return list;
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    /**
     * 从结果中获取出String数据
     *
     * @param response http结果对象
     * @param charSet  编码
     * @return
     * @throws Exception
     */
    private static HttpResult getResult(CloseableHttpResponse response, String charSet) throws Exception {
        String result;
        if (response == null)
            return null;
        if (charSet == null || "".equals(charSet))
            charSet = DEFAULT_CHARSET;
        HttpEntity entity = response.getEntity();
        if (entity == null)
            return null;
        result = EntityUtils.toString(entity, charSet);
        EntityUtils.consume(entity);    //关闭资源
        return new HttpResult(response.getStatusLine().getStatusCode(), result);
    }


    /***********************************************************************************************************************
     *                                                开启HTTPS验证                                                        *
     *********************************************************************************************************************/

    private static SSLConnectionSocketFactory socketFactory;

    private static TrustManager manager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    private static void enableSSL() {
        try {
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, new TrustManager[]{manager}, null);
            socketFactory = new SSLConnectionSocketFactory(tls, NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 为httpClients设置重试信息
     *
     * @param httpClientBuilder
     * @param retryTimes
     */
    private static void setRetyHandler(HttpClientBuilder httpClientBuilder, final int retryTimes) {
        HttpRequestRetryHandler myHandler = (exception, executionCount, context) -> {
            if (executionCount >= retryTimes)
                return false;// Do not retry if over max retry count
            if (exception instanceof InterruptedIOException)
                return false; //TimeOut
            if (exception instanceof UnknownHostException)
                return false; //Unknown host
            if (exception instanceof ConnectTimeoutException)
                return false; //Connection refused
            if (exception instanceof SSLException)
                return false; //SSL handshake exception
            HttpClientContext httpClientContext = HttpClientContext.adapt(context);
            HttpRequest request = httpClientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent)
                return true;//如果请求是认为是幂等的,那么就重试 Retry if the request is considered idempotent
            return false;
        };
        httpClientBuilder.setRetryHandler(myHandler);
    }

    private static CloseableHttpClient closeableHttpClient() {
        return createHttpClient(DEFAULT_RETRY_TIMES, DEFAULT_SOCKET_TIMEOUT);
    }

    public static CloseableHttpClient createHttpClient(int retryTimes, int socketTimeout) {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(DEFAULT_SOCKET_TIMEOUT); //设置超时时间,单位毫秒
        builder.setConnectionRequestTimeout(DEFAULT_REQUEST_TIMEOUT); //设置从connect manager获取connection超时时间。单位毫秒
        if (socketTimeout >= 0)
            builder.setSocketTimeout(socketTimeout);
        RequestConfig requestConfig = builder.setCookieSpec(CookieSpecs.STANDARD_STRICT)
                .setExpectContinueEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                .build();
        //开启https支持
        enableSSL();
        //创建可用Scheme
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", socketFactory).build();
        //创建connectionManager，添加connection配置信息
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if (retryTimes > 0)
            setRetyHandler(clientBuilder, retryTimes);
        CloseableHttpClient httpClient = clientBuilder.setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        return httpClient;
    }

}
