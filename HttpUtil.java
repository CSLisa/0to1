package com.taixin.wxminiprogram.commons;

import com.google.gson.Gson;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@UtilityClass
public class HttpUtil {
    private static final String APP_ID = "wx6d6c8667ef5991e1";//公众号appid
    private static final String APP_SERCET = "e78a65055f231a273b7a158db4b142b0";//公众号秘钥
    private static final String GRANT_TYPE = "client_credential";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";//微信token地址


    public static void main(String[] args) {
        HashMap params = new HashMap();
        params.put("appid", APP_ID);
        params.put("secret", APP_SERCET);
        params.put("grant_type", GRANT_TYPE);
//        nt:", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        try {
            Object o = HttpUtil.doGet(TOKEN_URL, params, null,false);
            System.out.println(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO:http或https提交数据(支持返回xml解析)
     *
     * @param url            请求Url
     * @param param          请求参数
     * @param header         请求头部
     * @param isFile         是否是文件
     * @param defaultCharset 请求的编码 默认 utf-8
     * @return T
     * @throws
     * @author lw
     * @date 2019/01/29 13:15
     */
    public static <T> T doGet(String url, Map param, Map header, Boolean isFile, String... defaultCharset) throws Exception {
        return submitData(url, null, param, header, true, 0, isFile, defaultCharset);
    }

    /**
     * 微信支付
     *
     * @param url            路径
     * @param API_KEY        微信支付使用的
     * @param param          请求参数
     * @param type           请求类型 1.json,2.xml,3.form表单
     * @param defaultCharset 请求的编码
     * @return
     */
    public static String doPost(String url, String API_KEY, Map param, int type, String... defaultCharset) throws Exception {
        return submitData(url, isNotBlank(API_KEY) ? API_KEY : null, param, null, false, type, false, defaultCharset);
    }

    /**
     * TODO:http或https提交数据(支持返回xml解析)
     *
     * @param url            请求Url
     * @param param          请求参数
     * @param header         请求头部
     * @param type           请求类型 post:1.json,2.xml,3.form表单,get 为0
     * @param isFile         是否是文件
     * @param defaultCharset 请求的编码 默认 utf-8
     * @return T
     * @throws
     * @author lw
     * @date 2019/01/29 13:15
     */
    public static <T> T doPost(String url, Map param, Map header, Integer type, Boolean isFile, String... defaultCharset) throws Exception {
        return submitData(url, null, param, header, false, type, false, defaultCharset);
    }

    /**
     * http或https提交数据(支持返回xml解析)
     *
     * @param url            请求Url
     * @param data           微信API键(只用于微信支付)可以不填
     * @param param          请求参数
     * @param header         请求头部
     * @param isGet          是否get
     * @param type           请求类型 post:1.json,2.xml,3.form表单,4 put,get 为0
     * @param isFile         是否是文件
     * @param defaultCharset 请求的编码 默认 utf-8
     * @return T 是文件返回byte[]否则为字符串
     * @throws Exception
     */
    @SuppressWarnings(value = "unchecked")
    public static <T> T submitData(String url, String data, Map param, Map header, Boolean isGet, Integer type, Boolean isFile, String... defaultCharset) throws Exception {
        url = trim(url);
        CloseableHttpClient httpClient = "https".equalsIgnoreCase(StringUtils.split(url, ":")[0]) ? getHttpClinet() : HttpClients.createDefault();// 创建Httpclient对象
        if (defaultCharset == null || defaultCharset.length == 0) { // 请求字符集
            defaultCharset = new String[1];
            defaultCharset[0] = "utf-8";
        }
        InputStream inputStream = null;
        String resultString = "";
        HttpPost httpPost = null;
        HttpGet httpGet = null;
        UrlEncodedFormEntity formEntity = null;
        Map<Object, Object> finalParam = param, head = header;
        List<NameValuePair> paramList = new ArrayList<>();
        if (isGet.equals(false)) {
            httpPost = new HttpPost(url);
            // 请求参数
            StringEntity entity = type.equals(1) ? new StringEntity(new Gson().toJson(param), ContentType.APPLICATION_JSON)
                    : type.equals(2) ? new StringEntity(isNotBlank(data) ? getRequestXml(param, 0) : getRequestXml(param, 1), defaultCharset[0])
                    : null;
            if (type.equals(3)) {
                if (isNotEmpty(param)) {
                    param.keySet().forEach(item -> {
                        paramList.add(new BasicNameValuePair(String.valueOf(item), String.valueOf(finalParam.get(item))));
                    });
                    formEntity = new UrlEncodedFormEntity(paramList, defaultCharset[0]);
                }
            }
            httpPost.setEntity(entity != null ? entity : formEntity);
        } else {
            URIBuilder builder = new URIBuilder(url);
            // 请求参数
            if (param != null) {
                param.keySet().forEach(item -> {
                    builder.addParameter(String.valueOf(item), String.valueOf(finalParam.get(item)));
                });
            }
            URI uri = builder.build();
//            URL urls = new URL(url);
//            BufferedReader in = new BufferedReader(new InputStreamReader(urls.openStream(), "UTF-8"));
//
//            String content = "";
//            String line;
//            while ((line = in.readLine()) != null) {
//                content += line + "\n";
//            }
//            in.close();
//            System.out.println("000"+content);
//            Pattern pattern = Pattern.compile(".*ts");
//            Matcher ma = pattern.matcher(content);

//            List<String> list = new ArrayList<String>();
//
//            while (ma.find()) {
//                list.add(ma.group());
//            }
            httpGet = new HttpGet(uri);

        }
        // 请求头
        if (isNotEmpty(header) && httpPost != null) {
            for (Map.Entry<Object, Object> entry : head.entrySet()) {
                httpPost.setHeader(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        } else if (isNotEmpty(header) && httpGet != null) {
            for (Map.Entry<Object, Object> entry : head.entrySet()) {
                httpGet.setHeader(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        CloseableHttpResponse response = httpClient.execute(httpPost != null ? httpPost : httpGet);// 1 json,2 xml,3 from
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            if (isFile) {
                inputStream = response.getEntity().getContent();
            } else {
                resultString = EntityUtils.toString(response.getEntity(), defaultCharset[0]);
            }
        }
        HttpClientUtils.closeQuietly(httpClient);
        HttpClientUtils.closeQuietly(response);
//        System.out.println(EntityUtils.toString(response.getEntity(), defaultCharset[0]));
        return isNotBlank(resultString) ? (T) resultString : (T) inputStream;
    }

    /**
     * TODO: 获取https连接
     *
     * @param
     * @return org.apache.http.impl.client.CloseableHttpClient
     * @throws
     * @author yourname
     * @date 2019/01/29 12:33
     */
    private static CloseableHttpClient getHttpClinet() throws Exception {
        Map map = setHttpParam();
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory((LayeredConnectionSocketFactory) map.get("sslConnectionSocketFactory"))
                .setConnectionManager((HttpClientConnectionManager) map.get("poolingHttpClientConnectionManager"))
                .setConnectionManagerShared(true)
                .build();
        return httpClient;
    }

    /**
     * TODO: https请求配置
     *
     * @param
     * @return java.util.Map
     * @throws Exception
     * @author yourname
     * @date 2019/01/29 12:31
     */
    private static Map setHttpParam() throws Exception {
        Map map = new HashMap();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
//                    信任所有站点 直接返回true
                return true;
            }
        });
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);

        RegistryBuilder<ConnectionSocketFactory> connectionSocketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        connectionSocketFactoryRegistryBuilder.register("http", new PlainConnectionSocketFactory());
        connectionSocketFactoryRegistryBuilder.register("https", sslConnectionSocketFactory);
        Registry<ConnectionSocketFactory> registryBuilder = connectionSocketFactoryRegistryBuilder.build();

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registryBuilder);
        poolingHttpClientConnectionManager.setMaxTotal(200);
        map.put("sslConnectionSocketFactory", sslConnectionSocketFactory);
        map.put("poolingHttpClientConnectionManager", poolingHttpClientConnectionManager);
        return map;
    }

    /**
     * 获取微信请求XML
     *
     * @param parameters 请求参数
     * @param type       XML生成类型默认微信请求XML，1：则为普通xml生成
     * @return
     * @author chenp
     */
    public static String getRequestXml(Map parameters, int type) {
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        Map<Object, Object> params = parameters;
        for (Map.Entry<Object, Object> entry : params.entrySet()) {
            String k = String.valueOf(entry.getKey()), v = String.valueOf(entry.getValue());
            if (type == 1) {
                sb.append("<").append(k).append(">").append(v).append("</").append(k).append(">");
            } else {
                if (k.equalsIgnoreCase("attach") || k.equalsIgnoreCase("body") || k.equalsIgnoreCase("sign") || k.equalsIgnoreCase("return_code") || k.equalsIgnoreCase("return_msg")) {
                    sb.append("<" + k + ">" + "<![CDATA[" + v + "]]></" + k + ">");
                } else {
                    sb.append("<" + k + ">" + v + "</" + k + ">");
                }
            }
        }
        sb.append("</xml>");
        return sb.toString();
    }

    /**
     * XML格式字符串转换为Map
     *
     * @param strXML XML字符串
     * @return XML数据转换后的Map
     * @throws Exception
     */
    public static Map xmlToMap(String strXML) throws Exception {
        Map<String, String> data = new HashMap();
        @Cleanup InputStream stream = new ByteArrayInputStream(strXML.getBytes(StandardCharsets.UTF_8));
        Document doc = newDocumentBuilder().parse(stream);
        doc.getDocumentElement().normalize();
        for (int idx = 0; idx < doc.getDocumentElement().getChildNodes().getLength(); ++idx) {
            if (doc.getDocumentElement().getChildNodes().item(idx).getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) doc.getDocumentElement().getChildNodes().item(idx);
                data.put(element.getNodeName(), element.getTextContent());
            }
        }
        return data;
    }

    private DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);
        return documentBuilderFactory.newDocumentBuilder();
    }

}
