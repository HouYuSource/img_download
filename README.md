
[TOC]

> 前言： 大概一个月前帮一个朋友写一个爬虫，这个爬虫比较有意思，抓取新浪微博的图片（某个人物的微博）【站内深度抓取】，然后就花了点时间帮他写一个java爬虫，然后打包成为一个类似绿色版的软件给他，双击即可运行的那种。然后我不太敢把那个源码放出来，所以我就写了另外一个更加有意思的爬虫~ 你想要什么图片，都可以下载，想要多少张都可以设置【奸笑】,这个小程序是一个小于1M的文件夹，超级轻量~超级好用

# img_download

## 1.0 看看效果吧

![在这里插入图片描述](https://shaines.cn/view/image?src=https://img-blog.csdnimg.cn/20191012234351696.png)

![在这里插入图片描述](https://shaines.cn/view/image?src=https://img-blog.csdnimg.cn/20191012234547974.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0ppbmdsb25nU291cmNl,size_16,color_FFFFFF,t_70)

---

## 2.0 了解一下 "图片下载器软件" 目录结构
链接：https://pan.baidu.com/s/1_R0WYQDLMiokyNTpWEXNxw 
提取码：8id5

你从百度云下载下来的应该是一个 img_download.rar 压缩包，然后解压出来即可使用，如下![在这里插入图片描述](https://shaines.cn/view/image?src=https://img-blog.csdnimg.cn/20191013000127611.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0ppbmdsb25nU291cmNl,size_16,color_FFFFFF,t_70)
你只需要双击运行 start.bat 即可下载图片 【默认下载 唐嫣 100张图片】

## 3.0 如何使用？

```csharp
1.0 图片下载器是基于百度搜索引擎实现
2.0 图片下载器获取图片链接使用单线程，下载图片使用多线程，正常情况下，1分钟能下载下载大概3000+图片【亲测】
3.0 图片下载使用方法：
	3.1 从百度网盘上下载最新的绿色版
	3.2 解压出来后再 ./conf目录下修改对应的参数即可 如: keyword totalCount (这两个关键参数)
	3.3 接着双击start.bat文件即可开始下载图片
```

## 4.0 源码剖析

> 核心代码如下

```java
import cn.shaines.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author houyu
 */
public class BaiDuImgSpider2 {

    private static final LoggerFactory.Logger logger = LoggerFactory.getLogger(BaiDuImgSpider2.class);

    private ThreadPoolUtil threadPoolUtil;
    // private BloomFilter<CharSequence> bloomFilter;
    private List<String> list;
    private Conf conf;
    private AtomicInteger index = new AtomicInteger();


    public void init() {
        threadPoolUtil = ThreadPoolUtil.get();
        // bloomFilter =  BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000, 0.0001);
        PropertiesUtil propertiesUtil = PropertiesUtil.builder(System.getProperty("user.dir") + "/conf/img.baidu.properties").build();
        Conf conf = new Conf();
        conf.setKeyword(propertiesUtil.getPropertyOrDefault("keyword", "唐嫣"));
        conf.setResultPath(propertiesUtil.getPropertyOrDefault("resultPath", System.getProperty("user.dir") + "/download/" + conf.getKeyword()));
        conf.setTotalCount(propertiesUtil.getPropertyOrDefault("totalCount", 1000));
        conf.setSleepTime(propertiesUtil.getPropertyOrDefault("sleepTime", 1000L));
        list = new ArrayList<>(conf.getTotalCount());
        //
        this.conf = conf;
    }

    public void run() throws Exception {
        // init -- start
        // 创建目录, 存在则不创建.
        // new File(resultPath).mkdirs();
        Files.createDirectories(Paths.get(this.conf.getResultPath()));
        // init --  end
        //
        String contextUrl = "https://image.baidu.com/";
        String baseUrl = "https://image.baidu.com/search/acjson?tn=resultjson_com&ipn=rj&ct=201326592&is=&fp=result&queryWord=${keyword}&cl=2&lm=-1&ie=utf-8&oe=utf-8&adpicid=&st=-1&z=&ic=&hd=&latest=&copyright=&word=${keyword}&s=&se=&tab=&width=&height=&face=0&istype=2&qc=&nc=1&fr=&expermode=&force=&cg=star&pn=${startIndex}&rn=30&gsm=&1570893297936=";
        //
        HttpURLConnectionUtil.Session session = HttpURLConnectionUtil.buildSession();
        //
        for(int i = 0; i < this.conf.getTotalCount(); i += 10) {
            if(i % 100 == 0) {
                // 查找10页, 那就换一个cookie
                session.build(contextUrl).execute().getBody();
            }
            // 随机睡眠0 ~ 指定的睡眠时间之内的时间
            Thread.sleep(ThreadLocalRandom.current().nextLong(this.conf.getSleepTime()));
            //
            String url = baseUrl.replace("${keyword}", this.conf.getKeyword()).replace("${startIndex}", String.valueOf(i));
            String bodyString = session.build(url).setIfEncodeUrl(true).execute().getBodyString();
            Object urlObject = JSONPath.eval(JSON.parseObject(bodyString), "$..data..hoverURL");
            if(urlObject instanceof List) {
                for(String imgUrl : (List<String>) urlObject) {
                    // if(bloomFilter.mightContain(imgUrl)) {
                    if(list.contains(imgUrl)) {
                        continue;
                    }
                    saveToFile(session, this.conf.getResultPath(), imgUrl);
                    // bloomFilter.put(imgUrl);
                    list.add(imgUrl);
                }
            }
        }
    }


    private void saveToFile(HttpURLConnectionUtil.Session session, String resultPath, String imgUrl) {
        threadPoolUtil.submit(() -> {
            byte[] body = session.build(imgUrl).execute().getBody();
            String fileType = FileUtil.getVagueImgFileType(FileUtil.getFileType(imgUrl));
            fileType = fileType == null ? "png" : fileType;
            try {
                String format = "%0"+ String.valueOf(this.conf.getTotalCount()).length() +"d";
                String prefix = String.format(format, index.incrementAndGet());
                Files.write(Paths.get(resultPath + "/" + prefix + "_" + UUID.randomUUID().toString().replace("-", "") + "." + fileType), body);
            } catch(IOException e) {
                logger.warn("保存图片失败", e);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        BaiDuImgSpider2 baiDuImgSpider = new BaiDuImgSpider2();
        baiDuImgSpider.init();
        baiDuImgSpider.run();
    }

    private class Conf {
        String keyword;
        int totalCount;
        String resultPath;
        Long sleepTime;

        public Conf() {
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getResultPath() {
            return resultPath;
        }

        public void setResultPath(String resultPath) {
            this.resultPath = resultPath;
        }

        public Long getSleepTime() {
            return sleepTime;
        }

        public void setSleepTime(Long sleepTime) {
            this.sleepTime = sleepTime;
        }
    }
}

```

## 5.0 项目地址

github: https://github.com/HouYuSource/img_download.git

## 6.0 写在最后的话
> 这个爬虫是抓取百度图片的，也就是说百度图片 【http://image.baidu.com】有的图片，理论上都是能download下来的

![在这里插入图片描述](https://shaines.cn/view/image?src=https://img-blog.csdnimg.cn/20191013123931243.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0ppbmdsb25nU291cmNl,size_16,color_FFFFFF,t_70)

> 请不要广泛使用，避免产生其他问题~

> 义务性贴出声明，避免揽事

```csharp
====== 声 明 ======
1.0 禁止使用商业用途。
2.0 严禁倒卖。
3.0 使用本程序产生的一切法律问题由使用者承担，与软件和制作人无任何关系。
4.0 软件的用途用于学习与交流,请使用完毕后24小时内自觉删除!
```

---

如果使用过程中有任何问题欢迎反馈: for.houyu@foxmail.com
