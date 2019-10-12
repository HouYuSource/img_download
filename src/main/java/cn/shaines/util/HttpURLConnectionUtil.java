package cn.shaines.util;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @description: 网络请求工具类
 * @author: houyu for.houyu@foxmail.com
 * @create: 2018-08-18 09:10:13
 */
public class HttpURLConnectionUtil {

    private static final LoggerFactory.Logger logger = LoggerFactory.getLogger(HttpURLConnectionUtil.class);

    public static boolean isEmpty(Object o) {
        if(o == null) {
            return true;
        }
        if(o instanceof String) {
            return ((String) o).isEmpty();
        } else if(o instanceof Collection) {
            return ((Collection) o).isEmpty();
        } else if(o instanceof Map) {
            return ((Map) o).isEmpty();
        } else if(o instanceof Object[]) {
            return ((Object[]) o).length == 0;
        } else {
            return false;
        }
    }

    public static boolean isNotEmpty(Object o) {
        return !isEmpty(o);
    }

    public static Request build(String site) {
        return new Request(site);
    }

    public static Request buildPost(String site) {
        return new Request(site).setMethod(Method.POST);
    }

    public static Session buildSession() {
        return new Session();
    }

    /**
     * @description 把浏览器的信息直接复制过来解析返回一个Map
     * @date 2019-09-02 11:00:51
     * @author houyu for.houyu@foxmail.com
     */
    public static Map<String, Object> parseToMap(String s) {
        String[] split = s.split("\\n");
        Map<String, Object> map = new LinkedHashMap<>(16);
        for (String keyAndValString : split) {
            String[] key_val = keyAndValString.split(": ", 2);
            map.put(key_val[0], key_val[1]);
        }
        return map;
    }


    /**
     * @description 域对象
     * @date 2019-09-02 11:00:51
     * @author houyu for.houyu@foxmail.com
     */
    public static class Session {

        private volatile String referer;                                                            // 推荐
        private volatile Map<String, Object> header = new ConcurrentHashMap<>(32);     // 请求头
        private volatile String cookie;                                                             // cookie
        private volatile Map<String, Object> extra = new ConcurrentHashMap<>(32);      // 携带参数(可使用于响应之后的操作)
        private volatile Proxy proxy;                                                               // 代理
        private volatile String charset;                                                            // 参数编码
        private volatile boolean ifEncodeUrl = false;                                               // 是否编码URL
        private volatile boolean ifCache = false;                                                   // 是否缓存
        private volatile int timeout = -1;                                                          // 连接超时(单位:毫秒)
        private volatile boolean ifStableRedirection = true;                                        // 是否稳定重定向
        private volatile boolean ifHandleHttps = true;                                              // 是否处理https
        private volatile boolean ifEnableDefaultHostnameVerifier = false;                           // 是否启用默认主机名验证程序
        private volatile HostnameVerifier hostnameVerifier;                                         // 主机名验证程序
        private volatile SSLSocketFactory sslSocketFactory;                                         // SocketFactory

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /** 保护构造 */
        protected Session() {}

        public synchronized Request build(String site) {
            return new Request(site)//
                    .setReferer(this.referer)//
                    .setHeader(new HashMap<>(this.header))//
                    .setCookie(this.cookie)//
                    .setExtra(new HashMap<>(this.extra))//
                    .setProxy(this.proxy)//
                    .setCharset(this.charset)//
                    .setIfEncodeUrl(this.ifEncodeUrl)//
                    .setIfCache(this.ifCache)//
                    .setTimeout(this.timeout)//
                    .setIfStableRedirection(this.ifStableRedirection)//
                    .setIfHandleHttps(this.ifHandleHttps)//
                    .setIfEnableDefaultHostnameVerifier(this.ifEnableDefaultHostnameVerifier)//
                    .setHostnameVerifier(this.hostnameVerifier)//
                    .setSslSocketFactory(this.sslSocketFactory)//
                    .setSession(this);
        }

        public Request buildPost(String site) {
            return this.build(site).setMethod(Method.POST);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // setter --- start

        protected Session setReferer(String referer) {
            if (isNotEmpty(referer)) {
                this.referer = referer;
            }
            return this;
        }

        protected Session setHeader(Map<String, Object> header) {
            if (isNotEmpty(header)) {
                header.forEach((k, v) -> {
                    if (isNotEmpty(k) && isNotEmpty(v)) {
                        this.header.put(k, v);
                    }
                });
                // 移除 content-length
                this.header.remove(Constant.CONTENT_LENGTH);
                this.header.remove("content-length");
                // 移除 cookie
                this.header.remove(Constant.REQUEST_COOKIE);
                this.header.remove("cookie");
                // 移除 referer
                this.header.remove(Constant.REFERER);
                this.header.remove("referer");
                // 移除 content-type
                this.header.remove(Constant.CONTENT_TYPE);
                this.header.remove("content-type");
            }
            return this;
        }

        protected Session setCookie(String cookie) {
            if (isNotEmpty(cookie)) {
                synchronized(this) {
                    this.cookie = this.cookie == null ? cookie : (this.cookie + "; " + cookie);// key1=val1; key2=val2
                    this.cookie = Request.cardingCookie(this.cookie);
                }
            }
            return this;
        }

        protected Session setExtra(Map<String, Object> extra) {
            if(isNotEmpty(extra)) {
                extra.forEach((k, v) -> {
                    if(isNotEmpty(k) && isNotEmpty(v)) {
                        this.extra.put(k, v);
                    }
                });
            }
            return this;
        }

        protected Session setProxy(Proxy proxy) {
            if (proxy != null) {
                this.proxy = proxy;
            }
            return this;
        }

        protected Session setCharset(String charset) {
            if (isNotEmpty(charset)) {
                this.charset = charset;
            }
            return this;
        }

        protected Session setIfEncodeUrl(boolean ifEncodeUrl) {
            this.ifEncodeUrl = ifEncodeUrl;
            return this;
        }

        protected Session setIfCache(boolean ifCache) {
            this.ifCache = ifCache;
            return this;
        }

        protected Session setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        protected Session setIfStableRedirection(boolean ifStableRedirection) {
            this.ifStableRedirection = ifStableRedirection;
            return this;
        }

        protected Session setIfHandleHttps(boolean ifHandleHttps) {
            this.ifHandleHttps = ifHandleHttps;
            return this;
        }

        protected Session setIfEnableDefaultHostnameVerifier(boolean ifEnableDefaultHostnameVerifier) {
            this.ifEnableDefaultHostnameVerifier = ifEnableDefaultHostnameVerifier;
            return this;
        }

        protected Session setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier != null) {
                this.hostnameVerifier = hostnameVerifier;
            }
            return this;
        }

        protected Session setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if(sslSocketFactory != null) {
                this.sslSocketFactory = sslSocketFactory;
            }
            return this;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Session{");
            sb.append("referer='").append(referer).append('\'');
            sb.append(", header=").append(header);
            sb.append(", cookie='").append(cookie).append('\'');
            sb.append(", extra=").append(extra);
            sb.append(", proxy=").append(proxy);
            sb.append(", charset='").append(charset).append('\'');
            sb.append(", ifEncodeUrl=").append(ifEncodeUrl);
            sb.append(", ifCache=").append(ifCache);
            sb.append(", timeout=").append(timeout);
            sb.append(", ifStableRedirection=").append(ifStableRedirection);
            sb.append(", ifHandleHttps=").append(ifHandleHttps);
            sb.append(", ifEnableDefaultHostnameVerifier=").append(ifEnableDefaultHostnameVerifier);
            sb.append(", hostnameVerifier=").append(hostnameVerifier);
            sb.append(", sslSocketFactory=").append(sslSocketFactory);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * @description 请求对象
     * @date 2019-08-21 11:00:49
     * @author houyu for.houyu@foxmail.com
     */
    public static class Request {

        private String site;                                            // 请求网站地址
        private Method method = Method.GET;                             // 请求方法(默认是GET)
        private Map<String, Object> header;                             // 请求头
        private Map<String, Object> param;                              // 请求参数
        private Map<String, Object> extra;                              // 携带参数(可使用于响应之后的操作)
        private Proxy proxy;                                            // 代理
        private String charset = Constant.UTF_8;                        // 参数编码(默认UTF-8)
        private boolean ifEncodeUrl = false;                            // 是否编码URL
        private boolean ifCache = false;                                // 是否缓存
        private String json;                                            // JSON文本
        private int timeout = -1;                                       // 连接超时(单位:毫秒)
        private String cookie;                                          // 携带cookie(优先) ex: key1=val1; key2=val2
        private boolean ifStableRedirection = true;                     // 是否稳定重定向
        private boolean ifHandleHttps = true;                           // 是否处理https
        private boolean ifEnableDefaultHostnameVerifier = false;        // 是否启用默认主机名验证程序
        private List<String> redirectUrlList;                           // 重定向的url列表
        private HttpURLConnection http = null;                          // HttpURLConnection对象
        private HostnameVerifier hostnameVerifier;                      // 主机名验证程序
        private SSLSocketFactory sslSocketFactory;                      // SocketFactory
        private Session session;                                        // 域对象
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private static Map<String, Object> DEFAULT_HEADER;              // 默认的请求头
        private static HostnameVerifier HOSTNAME_VERIFIER;              // 设置主机名验证程序
        private static SSLSocketFactory SSL_SOCKET_FACTORY;             // SocketFactory


        static {
            /** 初始化默认请求头 */
            DEFAULT_HEADER = new HashMap<>(8);
            DEFAULT_HEADER.put("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8");
            DEFAULT_HEADER.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36 shaines.cn");
            DEFAULT_HEADER.put("Accept-Encoding", "gzip");
            DEFAULT_HEADER.put("Accept-Language", "zh-CN,zh;q=0.8");
            // DEFAULT_HEADER.put("Content-Type", "application/x-www-form-urlencoded");
            //
            DEFAULT_HEADER = Collections.unmodifiableMap(DEFAULT_HEADER); // 设置不可以修改
            //
            /** 初始化全局主机名验证程序 */
            HOSTNAME_VERIFIER = (s, sslSession) -> true;
            /** 初始化全局主机名验证程序 */
            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    // return new X509Certificate[0];
                    return null;
                }
            };
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { x509TrustManager }, new SecureRandom());
                SSL_SOCKET_FACTORY = sslContext.getSocketFactory();
            } catch(NoSuchAlgorithmException | KeyManagementException e) {
                logger.warn("init SSLContext has exception ", e);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        protected Request(String site) {
            this.site = site;
            logger.debug("do request {}", this.site);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 执行 请求 */
        public Response execute() {
            // 初始化GET param
            this.handleGETParamWithUrl();
            // 初始化连接
            this.initConnection();
            // 发送数据包裹
            this.send();
            // 处理重定向
            boolean ifRedirect = this.handleRedirect();
            if(ifRedirect) {
                return this.execute();// 递归实现重定向
            }
            this.handleSession();
            // 返回响应
            return new Response(this.http, this.redirectUrlList, this.extra, this.session);
        }

        /** 处理 域对象 */
        private void handleSession() {
            if (this.session != null) {
                this.session
                        // .setReferer(this.getSite())//
                        .setHeader(this.header)//
                        .setCookie(this.cookie)//
                        .setExtra(this.extra)//
                        .setProxy(this.proxy)//
                        .setCharset(this.charset)//
                        .setIfEncodeUrl(this.ifEncodeUrl)//
                        .setIfCache(this.ifCache)//
                        .setTimeout(this.timeout)//
                        .setIfStableRedirection(this.ifStableRedirection)//
                        .setIfHandleHttps(this.ifHandleHttps)//
                        .setIfEnableDefaultHostnameVerifier(this.ifEnableDefaultHostnameVerifier)//
                        .setHostnameVerifier(this.hostnameVerifier)//
                        .setSslSocketFactory(this.sslSocketFactory);
            }
        }

        /** 处理重定向 */
        private boolean handleRedirect() {
            if(this.ifStableRedirection) {
                // 采用稳定重定向方式, 需要处理重定向问题
                int responseCode;
                try {
                    responseCode = this.http.getResponseCode();
                } catch(IOException var3) {
                    logger.warn("{} get response code has exception", this.site);
                    responseCode = 0;
                }
                if(responseCode == Constant.REDIRECT_CODE_301
                        || responseCode == Constant.REDIRECT_CODE_302
                        || responseCode == Constant.REDIRECT_CODE_303) {
                    this.site = this.http.getHeaderField(Constant.LOCATION);
                    this.redirectUrlList = this.redirectUrlList == null ? new ArrayList<>(8) : this.redirectUrlList;
                    this.redirectUrlList.add(this.site);
                    if(this.redirectUrlList.size() < 8) {
                        this.http.disconnect();     // 断开本次连接, 然后重新请求
                        logger.debug("{} request redirecting ", this.site);
                        return true;
                    }
                }
            } else {
                // 使用默认的重定向规则处理, 无序手动处理, 但是有可能出现重定向失败
                // do non thing
            }
            return false;
        }

        /** 发送数据 */
        private void send() {
            try {
                if(!Method.POST.equals(this.method) && !Method.PUT.equals(this.method)) {
                    this.http.connect();
                } else {
                    // POST...
                    this.handleContentTypeAndBody();
                }
            } catch(IOException e) {
                logger.warn("{} send data has exception", this.site);
                throw new RuntimeException(e);
            }
        }

        /** 处理 ContentType 和 传输内容 */
        private void handleContentTypeAndBody() throws IOException {
            if(!Method.GET.equals(this.method)) {
                // non GET
                /* handle ContentType 有可能多个content-type, 大小写不一致的问题 */
                String contentType = Objects.toString(this.header.get(Constant.CONTENT_TYPE), null);
                if(isEmpty(contentType)) {
                    contentType = Objects.toString(this.header.remove("content-type"), null);
                } else {
                    this.header.remove("content-type");
                }
                if(isEmpty(contentType)) {
                    // 没有 Content-Type
                    if(isNotEmpty(this.json)) {
                        this.addAndRefreshHead(Constant.CONTENT_TYPE, Constant.CONTENT_TYPE_WITH_JSON + this.charset);
                    } else {
                        this.addAndRefreshHead(Constant.CONTENT_TYPE, Constant.CONTENT_TYPE_WITH_FORM + this.charset);
                    }
                } else {
                    if(!contentType.contains(Constant.CHARSET)) {
                        // 存在 Content-Type ,没有 ;charset, 处理方式是添加 ;charset=this.charset
                        this.addAndRefreshHead(Constant.CONTENT_TYPE, contentType + "; " + Constant.CONTENT_TYPE_WITH_CHARSET + this.charset);
                    }
                }
                // 需要重新赋值一下, 否则导致下面有 NPE 危险
                contentType = String.valueOf(this.header.get(Constant.CONTENT_TYPE));
                /* handle body */
                boolean ifJson = contentType.contains("json");
                byte[] body;
                if(ifJson) {
                    if(isNotEmpty(this.json)) {
                        body = this.json.getBytes(this.charset);
                    } else {
                        // can handle param to json
                        body = new byte[0];
                    }
                } else {
                    //
                    String paramString = parseParamMapToString(this.param, this.charset);// 必须编码, 否则个别网站获取不到数据
                    paramString = Objects.toString(paramString, "");
                    body = paramString.getBytes(this.charset);
                }
                try(OutputStream outputStream = this.http.getOutputStream()) {
                    // 使用 try-with-resource 方式处理流, 无需手动关闭流操作
                    outputStream.write(body);
                    outputStream.flush();
                }
            }
        }

        /** 初始化连接 */
        private void initConnection() throws RuntimeException {
            URL url;
            try {
                url = new URL(this.site);
            } catch(MalformedURLException e) {
                logger.warn("{} create URL has exception", this.site);
                throw new RuntimeException("创建URL出错" + e.getMessage());
            }
            //
            try {
                this.http = this.openConnection(url, this.proxy);
                //
                if(this.timeout > 0) {
                    // 设置超时
                    this.http.setConnectTimeout(this.timeout);
                    this.http.setReadTimeout(this.timeout);
                }
                // 设置请求方法
                this.method = this.method == null ? Method.GET : this.method;
                this.http.setRequestMethod(this.method.name());
            } catch(IOException e) {
                logger.warn("{} open connection has exception", this.site);
                throw new RuntimeException("打开连接出错" + e.getMessage());
            }
            //
            this.http.setDoInput(true);
            if(!Method.GET.equals(this.method)) {
                // 非GET方法需要设置可输入
                http.setDoOutput(true);
                http.setUseCaches(false);
            }
            // 初始化填充默认请求头
            this.initAndFillDefaultHeader();
            // 设置cookie
            this.setCookie();
            // 设置请求头到连接中
            this.header.forEach((k, v) -> this.http.setRequestProperty(k, String.valueOf(v)));
            // 设置缓存
            if(ifCache) {
                this.http.setUseCaches(true);
            }
            // 设置是否自动重定向
            this.http.setInstanceFollowRedirects(!(this.ifStableRedirection));
        }

        /** 设置 Cookie到连接中 */
        private void setCookie() {
            String headerCookie = Objects.toString(this.header.remove(Constant.REQUEST_COOKIE), null);
            if (headerCookie != null) {
                this.cookie = isEmpty(this.cookie) ? headerCookie : (headerCookie + "; " + this.cookie);
            }
            headerCookie = Objects.toString(this.header.remove("cookie"), null);
            if (headerCookie != null) {
                this.cookie = isEmpty(this.cookie) ? headerCookie : (headerCookie + "; " + this.cookie);
            }
            if(isNotEmpty(this.cookie)) {
                this.cookie = cardingCookie(this.cookie);
                this.addHead(Constant.REQUEST_COOKIE, this.cookie);
            }
        }

        /** 刷新 请求头信息 */
        private void addAndRefreshHead(String key, Object value) {
            if (isNotEmpty(key) && isNotEmpty(value)) {
                this.addHead(key, value);
                http.setRequestProperty(key, String.valueOf(value));
            }
        }

        /** 初始化和设置默认请求头 */
        private void initAndFillDefaultHeader() {
            HashMap<String, Object> initMap = new HashMap<>(16);
            initMap.putAll(DEFAULT_HEADER);
            if(isNotEmpty(this.header)) {
                initMap.putAll(this.header);
            }
            this.header = initMap;
        }

        /** 打开连接 */
        private HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
            URLConnection connection;
            if(this.proxy == null) {
                connection = url.openConnection();
            } else if(isNotEmpty(proxy.getUsername())) {                            // 设置代理服务器
                java.net.Proxy javaNetProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(this.proxy.getHost(), this.proxy.getPort()));
                connection = url.openConnection(javaNetProxy);
                String authString = this.proxy.getUsername() + ":" + this.proxy.getPassword();
                String auth = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes(this.charset));
                connection.setRequestProperty(Constant.PROXY_AUTHORIZATION, auth);
                logger.debug("{} do proxy server ", this.site);
            } else if(isNotEmpty(proxy.getHost())) {                                // 设置代理主机和端口
                java.net.Proxy javaNetProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(this.proxy.getHost(), this.proxy.getPort()));
                connection = url.openConnection(javaNetProxy);
                logger.debug("{} do proxy ", this.site);
            } else {                                                                                    // 不设置代理
                connection = url.openConnection();
            }
            if(ifHandleHttps && connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                // 设置主机名验证程序
                if (this.hostnameVerifier != null) {
                    httpsConn.setHostnameVerifier(this.hostnameVerifier);
                } else if (this.ifEnableDefaultHostnameVerifier) {
                    httpsConn.setHostnameVerifier(HOSTNAME_VERIFIER);
                }
                // 设置ssl factory
                httpsConn.setSSLSocketFactory(this.sslSocketFactory == null ? SSL_SOCKET_FACTORY : this.sslSocketFactory);
            }
            return (HttpURLConnection) connection;
        }

        /** 设置 如果是GET, 则将参数写入url中 */
        private void handleGETParamWithUrl() {
            // 校验url地址
            Objects.requireNonNull(this.site, "网站地址不可以为空");
            // 校验url协议 要求url必须是http / https协议, 默认使用http协议
            if(!this.site.toLowerCase().startsWith(Constant.HTTP)) {
                this.site = Constant.HTTP + "://" + this.site;
            }
            // 处理url中的query进行url编码
            int indexOf;
            if (ifEncodeUrl && (indexOf = this.site.indexOf("?")) > -1) {
                String query = this.site.substring(indexOf);
                query = encode(query, this.charset);
                query = query.replace("%3F", "?").replace("%2F", "/").replace("%3A", ":").replace("%3D", "=").replace("%26", "&").replace("%23", "#");
                this.site = this.site.substring(0, indexOf) + query;
            }
            if(Method.GET.equals(this.method)) {
                String query = parseParamMapToString(this.param, ifEncodeUrl ? this.charset : null);
                if(query != null) {
                    this.site = this.site.contains("?") ? (this.site + "&" + query) : (this.site + "?" + query);
                }
            }
        }

        /**
         * @description Map => key1=val1&key2=val2
         * @date 2019-08-20 20:42:59
         * @author houyu for.houyu@foxmail.com
         */
        public static String parseParamMapToString(Map<String, Object> paramMap, String charset) {
            if(isNotEmpty(paramMap)) {
                StringBuilder builder = new StringBuilder(128);
                if(isEmpty(charset)) {
                    // key1=val1&key2=val2
                    paramMap.forEach((k, v) -> builder.append(k).append(Constant.EQU).append(v).append(Constant.AND_SIGN));
                } else {
                    paramMap.forEach((k, v) -> builder.append(encode(k, charset)).append(Constant.EQU).append(encode(String.valueOf(v), charset))
                            .append(Constant.AND_SIGN));
                }
                return builder.delete(builder.length() - 1, builder.length()).toString();
            }
            return null;
        }

        /** url 编码 */
        public static String encode(String text, String charset) {
            if(isNotEmpty(text) && isNotEmpty(charset)  && Charset.isSupported(charset)) {
                // 不为空 并且charset可用
                try {
                    return URLEncoder.encode(text, charset);
                } catch(UnsupportedEncodingException e) {
                    // do non thing
                }
            }
            return text;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 设置 网站地址 */
        public Request setSite(String site) {
            this.site = site;
            return this;
        }

        /** 设置 请求方法 */
        public Request setMethod(Method method) {
            if (method != null) {
                this.method = method;
            }
            return this;
        }

        /** 设置 请求头 */
        public Request setHeader(Map<String, Object> header) {
            if (isNotEmpty(header)) {
                if (isEmpty(this.header)) {
                    this.header = header;
                }else {
                    this.header.putAll(header);
                }
            }
            return this;
        }

        /** 设置 请求参数 */
        public Request setParam(Map<String, Object> param) {
            if (isNotEmpty(param)) {
                if (isEmpty(this.param)) {
                    this.param = param;
                }else {
                    this.param.putAll(param);
                }
            }
            return this;
        }

        /** 设置 携带参数 */
        public Request setExtra(Map<String, Object> extra) {
            if (isNotEmpty(extra)) {
                if (isEmpty(this.extra)) {
                    this.extra = extra;
                }else {
                    this.extra.putAll(extra);
                }
            }
            return this;
        }

        /** 设置 参数编码 */
        public Request setCharset(String charset) {
            if(isNotEmpty(charset) && Charset.isSupported(charset)) {
                // 只设置支持的编码, 否则有可能导致整体的错误
                this.charset = charset;
            }
            return this;
        }

        /** 设置 是否需要编码 */
        public Request setIfEncodeUrl(boolean ifEncodeUrl) {
            this.ifEncodeUrl = ifEncodeUrl;
            return this;
        }

        /** 设置 是否缓存 */
        public Request setIfCache(boolean ifCache) {
            this.ifCache = ifCache;
            return this;
        }

        /** 设置 内容类型 */
        public Request setContentType(String contentType) {
            if (isNotEmpty(contentType)) {
                this.addHead(Constant.CONTENT_TYPE, contentType);
                this.header.remove("content-type");
            }
            return this;
        }

        /** 设置 引荐 */
        public Request setReferer(String referer) {
            if (isNotEmpty(referer)) {
                this.addHead(Constant.REFERER, referer);
                this.header.remove("referer");
            }
            return this;
        }

        /** 设置 JSON文本 */
        public Request setJson(String json) {
            if (isNotEmpty(json)) {
                this.json = json;
            }
            return this;
        }

        /** 设置 连接超时 */
        public Request setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /** 设置 携带cookie */
        public Request setCookie(String cookie) {
            if (isNotEmpty(cookie)) {
                this.cookie = this.cookie == null ? cookie : (this.cookie + "; " + cookie);
                this.cookie = cardingCookie(this.cookie);
            }
            return this;
        }

        /** 处理 cookie, 防止出现重复的key */
        public static String cardingCookie(String cookie) {
            if (isNotEmpty(cookie)) {
                String[] split = cookie.split(Constant.COOKIE_SPLIT);
                Map<String, String> tempMap = new LinkedHashMap<>(split.length);
                for (String keyAndVal : split) {
                    String[] key_val = keyAndVal.split(Constant.EQU, 2);
                    if (key_val.length == 2) {
                        tempMap.put(key_val[0], key_val[1]);
                    }
                }
                StringBuilder builder = new StringBuilder(cookie.length());
                tempMap.forEach((k, v) -> builder.append(k).append(Constant.EQU).append(v).append(Constant.COOKIE_SPLIT));
                return builder.length() > 2 ? builder.delete(builder.length() - 2, builder.length()).toString() : "";
            }
            return "";
        }

        /** 设置 携带cookie */
        public Request setCookie(Map<String, String> cookie) {
            if (isNotEmpty(cookie)) {
                StringBuilder builder = new StringBuilder(128);
                cookie.forEach((k, v) -> builder.append(k).append(Constant.EQU).append(v).append(Constant.COOKIE_SPLIT));
                String tempString = builder.length() > 2 ? builder.delete(builder.length() - 2, builder.length()).toString() : "";
                this.setCookie(tempString);
            }
            return this;
        }

        /** 设置 是否稳定重定向 */
        public Request setIfStableRedirection(boolean ifStableRedirection) {
            this.ifStableRedirection = ifStableRedirection;
            return this;
        }

        /** 设置 是否处理https */
        public Request setIfHandleHttps(boolean ifHandleHttps) {
            this.ifHandleHttps = ifHandleHttps;
            return this;
        }

        /** 设置 是否启用默认主机名验证程序 */
        public Request setIfEnableDefaultHostnameVerifier(boolean ifEnableDefaultHostnameVerifier) {
            this.ifEnableDefaultHostnameVerifier = ifEnableDefaultHostnameVerifier;
            return this;
        }

        /** 设置 域对象 */
        protected Request setSession(Session session) {
            if (session != null) {
                this.session = session;
            }
            return this;
        }

        /** 添加 请求头 */
        public Request addHead(String key, Object value) {
            if (isNotEmpty(key) && isNotEmpty(value)) {
                this.header = this.header == null ? new HashMap<>(8) : this.header;
                this.header.put(key, value);
            }
            return this;
        }

        /** 添加 请求参数 */
        public Request addParam(String key, Object value) {
            if (isNotEmpty(key) && isNotEmpty(value)) {
                this.param = this.param == null ? new HashMap<>(16) : this.param;
                this.param.put(key, value);
            }
            return this;
        }

        /** 添加 携带参数 */
        public Request addExtra(String key, Object value) {
            if (isNotEmpty(key) && isNotEmpty(value)) {
                this.extra = this.extra == null ? new HashMap<>(16) : this.extra;
                this.extra.put(key, value);
            }
            return this;
        }

        /** 设置 代理 */
        public Request setProxy(Proxy proxy) {
            if (proxy != null) {
                this.proxy = proxy;
            }
            return this;
        }

        /** 设置 代理 */
        public Request setProxy(String host, Integer port) {
            if (isNotEmpty(host) && isNotEmpty(port)) {
                this.proxy = new Proxy(host, port);
            }
            return this;
        }

        /** 设置 代理 */
        public Request setProxy(String host, Integer port, String username, String password) {
            if (isNotEmpty(host) && isNotEmpty(port) && isNotEmpty(username) && isNotEmpty(password)) {
                this.proxy = new Proxy(host, port, username, password);
            }
            return this;
        }

        /** 设置 主机名验证程序 */
        public Request setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier != null) {
                this.hostnameVerifier = hostnameVerifier;
            }
            return this;
        }

        /** 设置 sslSocketFactory */
        public Request setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory != null) {
                this.sslSocketFactory = sslSocketFactory;
            }
            return this;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 获取 网站地址 */
        public String getSite() {
            return this.site;
        }

        /** 获取 请求方法 */
        public Method getMethod() {
            return this.method;
        }

        /** 获取 请求头 */
        public Map<String, Object> getHeader() {
            return this.header;
        }

        /** 获取 请求参数 */
        public Map<String, Object> getParam() {
            return this.param;
        }

        /** 获取 携带参数 */
        public Map<String, Object> getExtra() {
            return this.extra;
        }

        /** 获取 代理 */
        public Proxy getProxy() {
            return this.proxy;
        }

        /** 获取 参数编码 */
        public String getCharset() {
            return this.charset;
        }

        /** 获取 JSON文本 */
        public String getJson() {
            return this.json;
        }

        /** 获取 连接超时 */
        public int getTimeout() {
            return this.timeout;
        }

        /** 获取 是否稳定重定向 */
        public boolean getIfStableRedirection() {
            return this.ifStableRedirection;
        }

        /** 获取 是否处理https */
        public boolean getIfHandleHttps() {
            return ifHandleHttps;
        }

        /** 获取 是否启用默认主机名验证程序 */
        public boolean getIfEnableDefaultHostnameVerifier() {
            return ifEnableDefaultHostnameVerifier;
        }

        /** 获取 主机名验证程序 */
        public HostnameVerifier getHostnameVerifier() {
            return this.hostnameVerifier;
        }

        /** 获取 sslSocketFactory */
        public SSLSocketFactory getSslSocketFactory() {
            return this.sslSocketFactory;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Request{");
            sb.append("site='").append(site).append('\'');
            sb.append(", method=").append(method);
            sb.append(", header=").append(header);
            sb.append(", param=").append(param);
            sb.append(", extra=").append(extra);
            sb.append(", proxy=").append(proxy);
            sb.append(", charset='").append(charset).append('\'');
            sb.append(", ifEncodeUrl=").append(ifEncodeUrl);
            sb.append(", ifCache=").append(ifCache);
            sb.append(", json='").append(json).append('\'');
            sb.append(", timeout=").append(timeout);
            sb.append(", cookie='").append(cookie).append('\'');
            sb.append(", ifStableRedirection=").append(ifStableRedirection);
            sb.append(", ifHandleHttps=").append(ifHandleHttps);
            sb.append(", ifEnableDefaultHostnameVerifier=").append(ifEnableDefaultHostnameVerifier);
            sb.append(", redirectUrlList=").append(redirectUrlList);
            sb.append(", http=").append(http);
            sb.append(", hostnameVerifier=").append(hostnameVerifier);
            sb.append(", sslSocketFactory=").append(sslSocketFactory);
            sb.append(", session=").append(session);
            sb.append('}');
            return sb.toString();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * @description 响应对象
     * @date 2019-08-21 11:01:03
     * @author houyu for.houyu@foxmail.com
     */
    public static class Response {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private String site;                                // 请求url
        private Map<String, List<String>> header;           // 响应头信息
        private Map<String, Object> extra;                  // request携带参数(可使用于响应之后的操作)
        private String cookie;                              // cookie ex:key2=val2; key1=val1
        private String charset;                             // 响应编码
        private String defaultCharset;                      // 默认编码
        private byte[] body;                                // 响应体
        private List<String> redirectUrlList;               // 重定向的url列表
        private HttpURLConnection http;                     // HttpURLConnection
        private Integer code;                               // http响应状态码(HttpURLConnection.HTTP_OK)
        private Session session;                            // 域对象
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 保护构造 */
        protected Response() {}

        /** 获取html 响应的 charset */
        private static final Pattern PATTERN_FOR_CHARSET = Pattern.compile("charset\\s*=\\s*['\"]*([^\\s;'\"]*)", Pattern.CASE_INSENSITIVE);
        private static Response EMPTY_RESPONSE;

        static {
            EMPTY_RESPONSE = new Response();
            EMPTY_RESPONSE.site = Constant.EMPTY_STRING;
            EMPTY_RESPONSE.header = Collections.emptyMap();
            EMPTY_RESPONSE.extra = Collections.emptyMap();
            EMPTY_RESPONSE.cookie = Constant.EMPTY_STRING;
            EMPTY_RESPONSE.charset = Constant.UTF_8;
            EMPTY_RESPONSE.defaultCharset = Constant.UTF_8;
            EMPTY_RESPONSE.body = new byte[0];
            EMPTY_RESPONSE.redirectUrlList = Collections.emptyList();
            EMPTY_RESPONSE.http = null;
            EMPTY_RESPONSE.code = 0;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 构造 Response */
        protected Response(HttpURLConnection http, List<String> redirectUrlList, Map<String, Object> extra, Session session) {
            //
            this.http = http;
            this.redirectUrlList = redirectUrlList;
            this.extra = extra;
            this.session = session;
            //
            this.init();
        }

        /** 初始化数据 */
        private void init() {
            try {
                this.site = this.http.getURL().toString();
                this.header = this.http.getHeaderFields();
                this.code = this.http.getResponseCode();
                this.defaultCharset = detectCharset(this.http.getContentType());
                InputStream inputStream = this.code < 400 ? this.http.getInputStream() : this.http.getErrorStream();
                this.initParseInputSteam(inputStream);
                this.handleSession();
                logger.debug("{} get response success, code = {}", this.site, this.code);
            } catch(IOException e) {
                logger.warn("{} do response has exception ", this.site, e);
                throw new RuntimeException(e);
            }
        }

        /** 处理 域对象 */
        private void handleSession() {
            if (this.session != null) {
                this.session.setReferer(this.getSite());
                this.session.setCookie(this.getCookie());
            }
        }

        /** 初始化解析inputStream */
        private void initParseInputSteam(InputStream inputStream) throws IOException {
            // 获取响应头是否有Content-Encoding=gzip
            String gzip = Optional.ofNullable(this.header.get(Constant.CONTENT_ENCODING))//
                    .filter(v -> v.size() > 0)//
                    .map(v -> v.get(0))//
                    .map(String::toLowerCase)//
                    .filter(Constant.GZIP::equals).orElse(null);
            if(gzip != null) {
                inputStream = new GZIPInputStream(inputStream);
            }
            ByteArrayOutputStream outputStream = null;
            try {
                outputStream = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024 * 3];
                for(int length = 0; length > -1; length = inputStream.read(bytes)) {
                    outputStream.write(bytes, 0, length);
                    outputStream.flush();
                }
                this.body = outputStream.toByteArray();
            } catch(IOException e) {
                logger.warn("{} do response inputSteam has exception ", this.site, e);
                throw new RuntimeException(e);
            } finally {
                this.close(inputStream);
                this.close(outputStream);
                this.http.disconnect();
            }
        }

        private void close(Closeable closeable) {
            try {
                if(closeable != null) {
                    closeable.close();
                }
            } catch(IOException e) {
                // non do thing
            } finally {
                closeable = null;
            }
        }

        /*public static Response ofEmpty() {
            return Response.EMPTY_RESPONSE;
        }*/
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 获取 url链接 */
        public String getSite() {
            return this.site;
        }

        /** 获取 响应头信息 */
        public Map<String, List<String>> getHeader() {
            return this.header;
        }

        /** 获取 携带参数 */
        public Map<String, Object> getExtra() {
            return this.extra;
        }

        /** 获取 携带参数值 */
        public Object getExtraValue(String key) {
            return this.extra == null ? null : this.extra.get(key);
        }

        /** 获取 cookie */
        public String getCookie() {
            if(this.cookie == null) {
                List<String> cookieList = this.header.get(Constant.RESPONSE_COOKIE);
                if(isNotEmpty(cookieList)) {
                    StringJoiner joiner = new StringJoiner(Constant.COOKIE_SPLIT);
                    for(String cookieObj : cookieList) {
                        joiner.add(cookieObj.split(Constant.COOKIE_SPLIT)[0]);
                    }
                    this.cookie = joiner.toString();
                }
            }
            return this.cookie;
        }

        /** 获取 cookieMap */
        public Map<String, String> getCookieMap() {
            Map<String, String> cookieMap = null;
            String cookieString = this.getCookie();
            if(isNotEmpty(cookieString)) {
                String[] cookieKeyValArray = cookieString.split(Constant.COOKIE_SPLIT);
                cookieMap = new HashMap<>(cookieKeyValArray.length);
                for(String cookieKeyVal : cookieKeyValArray) {
                    String[] keyVal = cookieKeyVal.split(Constant.EQU, 2);
                    if(keyVal.length == 2) {
                        cookieMap.put(keyVal[0], keyVal[1]);
                    }
                }
            }
            return cookieMap;
        }

        /** 获取 html编码 */
        public String getCharset() {
            return isEmpty(this.charset) ? this.defaultCharset : this.charset;
        }

        /** 获取 响应 */
        public byte[] getBody() {
            return this.body;
        }

        /** 获取 响应 */
        public String getBodyString() {
            try {
                return getBodyString(isEmpty(this.charset) ? this.defaultCharset : this.charset);
            } catch(UnsupportedEncodingException e) {
                logger.warn("{} get body string has exception", this.site, e);
                return null;
            }
        }

        /** 获取 响应 */
        public String getBodyString(String charset) throws UnsupportedEncodingException {
            return new String(this.getBody(), isNotEmpty(charset) && Charset.isSupported(charset) ? charset : this.getCharset());
        }

        /** 获取 重定向的url列表 */
        public List<String> getRedirectUrlList() {
            return this.redirectUrlList;
        }

        public HttpURLConnection getHttp() {
            return this.http;
        }

        public Integer getCode() {
            return this.code;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /** 自动识别网页编码 */
        private static String detectCharset(String contentType) {
            // contentType = text/html
            // contentType = text/html;charset=ISO-8859-1
            if(isNotEmpty(contentType)) {
                int index = contentType.indexOf(Constant.CONTENT_TYPE_WITH_CHARSET);
                if(index > 0) {
                    String parseCharset = contentType.substring(index + Constant.CONTENT_TYPE_WITH_CHARSET.length()).trim();
                    if(Charset.isSupported(parseCharset)) {
                        return parseCharset;
                    }
                    Matcher matcher = PATTERN_FOR_CHARSET.matcher(contentType);
                    if(matcher.find()) {
                        parseCharset = matcher.group(1);
                        if(Charset.isSupported(parseCharset)) {
                            return parseCharset;
                        }
                    }
                }
            }
            return Constant.UTF_8;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Response{");
            sb.append("site='").append(site).append('\'');
            sb.append(", header=").append(header);
            sb.append(", extra=").append(extra);
            sb.append(", cookie='").append(cookie).append('\'');
            sb.append(", charset='").append(charset).append('\'');
            sb.append(", defaultCharset='").append(defaultCharset).append('\'');
            sb.append(", body=").append(Arrays.toString(body));
            sb.append(", redirectUrlList=").append(redirectUrlList);
            sb.append(", http=").append(http);
            sb.append(", code=").append(code);
            sb.append(", session=").append(session);
            sb.append('}');
            return sb.toString();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * @description 代理对象
     * @date 2019-08-21 11:00:30
     * @author houyu for.houyu@foxmail.com
     */
    public static class Proxy {

        private String host;
        private Integer port;
        private String username;
        private String password;

        public Proxy(String host, Integer port) {
            this.host = host;
            this.port = port;
        }

        public Proxy(String host, Integer port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

    }

    /**
     * @description 请求方法
     * @date 2019-08-21 11:00:17
     * @author houyu for.houyu@foxmail.com
     */
    public enum Method {GET, POST, PUT, DELETE}

    /**
     * @description 常量
     * @date 2019-08-21 10:59:36
     * @author houyu for.houyu@foxmail.com
     */
    public interface Constant {

        String CONTENT_LENGTH = "Content-Length";
        String CONTENT_TYPE = "Content-Type";
        String RESPONSE_COOKIE = "Set-Cookie";  // 获取响应的COOKIE
        String REQUEST_COOKIE = "Cookie";       // 设置发送的COOKIE
        String REFERER = "Referer";
        String PROXY_AUTHORIZATION = "Proxy-Authorization";
        String CONTENT_ENCODING = "Content-Encoding";
        String LOCATION = "Location";

        String CONTENT_TYPE_WITH_FORM = "application/x-www-form-urlencoded; charset=";
        String CONTENT_TYPE_WITH_JSON = "application/json; charset=";
        String GZIP = "gzip";

        int REDIRECT_CODE_301 = 301;
        int REDIRECT_CODE_302 = 302;
        int REDIRECT_CODE_303 = 303;

        String COOKIE_SPLIT = "; ";
        String EQU = "=";
        String UTF_8 = "UTF-8";
        String HTTPS = "https";
        String HTTP = "http";

        String AND_SIGN = "&";

        String CONTENT_TYPE_WITH_CHARSET = "charset=";

        String EMPTY_STRING = "";
        String CHARSET = "charset";

    }

}


