package cn.shaines;


import cn.shaines.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author houyu
 * @createTime 2019/9/27 10:31
 */
public class BaiDuImgSpider {

    private static final LoggerFactory.Logger logger = LoggerFactory.getLogger(BaiDuImgSpider.class);

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
        /*
        使用手机模式
        tn: wisejsonala
        ie: utf-8
        fromsf: 1
        word: 唐嫣
        pn: 20 开始位置
        rn: 10 大小
        gsm: 0
        searchtype: 1
        prefresh: undefined
        from: link
        type: 1
        tagname: 推荐
         */
        String baseUrl = "http://m.baidu.com/sf/vsearch/image/search/wisesearchresult?tn=wisejsonala&ie=utf-8&fromsf=1&word=${keyword}&pn=${startIndex}&rn=10&gsm=&searchtype=1&prefresh=undefined&from=link&type=1&tagname=%E6%8E%A8%E8%8D%90";
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
            Object urlObject = JSONPath.eval(JSON.parseObject(bodyString), "$..linkData..thumbnailUrl");
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
        BaiDuImgSpider baiDuImgSpider = new BaiDuImgSpider();
        baiDuImgSpider.init();
        baiDuImgSpider.run();
    }

    private class Conf {
        String keyword;
        int totalCount; // 一页10条
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
