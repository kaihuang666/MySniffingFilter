package com.kai.video.view;

import android.util.Log;
import android.view.View;

import com.kai.video.sniffing.SniffingFilter;
import com.kai.video.sniffing.SniffingVideo;
import com.kai.video.sniffing.Util;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MySniffingFilter implements SniffingFilter {
    public static boolean canLog = true;
    private Map<String, String > headers = new HashMap<>(0);

    public static void setCanLog(boolean canLog) {
        MySniffingFilter.canLog = canLog;
    }

    private void log(String message){
        if (canLog)
            Log.i("SniffingFilter", message);
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers = headers;
    }


    @Override
    public SniffingVideo onFilter(View webView, String url) {
        SniffingVideo video = null;
        try {
            log("url = " + url);
            if ((url.contains(".flv")) && !url.contains(".js") && !url.contains("flv.min.js")){
                video = new SniffingVideo(url, "video/x-flv", 0, "flv");
            }
            if (url.endsWith(".mp4") && Util.getContentLength(url) > 1024 * 1024 *2){
                video = new SniffingVideo(url, "video/mp4", 0, "mp4");
            }
            //vkey请求的mp4和m3u8（防止vkey失效）及常见的index.m3u8后缀文件
            else if ((url.contains("vkey") && (url.contains(".mp4?") )|| (url.contains(".m3u8?") && url.contains("vkey") && !url.contains("https://ycache.parwix.com:4433/")) || (url.endsWith("index.m3u8")) && !url.contains("?url="))){
                video = new SniffingVideo(url, url.contains("m3u8")?"video/x-mpegurl":"video/mpeg", 0, url.contains("m3u8")?"m3u8":"mp4");
            }
            //一些服务器请求类型的mp4，因为大体积需要正则匹配
            else if (url.contains("filename") && url.contains(".mp4")){
                String decode = URLDecoder.decode(url, "utf-8");
                Pattern pattern = Pattern.compile("filename.*=.*\\.mp4");
                Matcher matcher = pattern.matcher(decode);
                if (matcher.find())
                    video = new SniffingVideo(url, "video/mp4", 0, "mp4");
            }
            else{
                Object[] content = Util.getContent(url, headers);
                String s = content[1].toString();
                url = content[0].toString();
                log(url + "|" + s);
                if (s.equals("filtered")){
                    //do nothing
                }else if (s.equals("video/x-flv") || s.equals("video/flv")){
                    video = new SniffingVideo(url, s, 0, "flv");
                }
                //常规mp4
                else if (s.equals("video/mp4")){
                    video = new SniffingVideo(url, s, 0 ,"mp4");
                }
                //avi
                else if (s.equals("video/avi")){
                    video = new SniffingVideo(url, s, 0 ,"avi");
                }
                //未知contentType的二进制流
                else if (s.equals("application/octet-stream")) {
                    String e = getElement(url);
                    //如果后缀为.mp4
                    if (e.equals(".mp4"))
                        video = new SniffingVideo(url, s, 0, "mp4");
                    //如果没有后缀则需要判断大小，一般小文件就忽略即可
                    else if (Util.getContentLength(url) >= 1024 * 1024 *2)
                        video = new SniffingVideo(url, s, 1, "mp4");
                    else if (url.contains(".m3u8"))
                        video = new SniffingVideo(url, s, 0, "m3u8");
                }
                //MPEG格式的媒体文件，包括mpeg-url(HLS流)和mpeg-mp4类
                else if((s.toLowerCase().contains("video") || s.toLowerCase().contains("mpeg"))){
                    video = new SniffingVideo(url, s, 0, url.contains("m3u8")?"m3u8":"mp4");
                }
                //直接将m3u8链接打印出来的html
                else if (s.contains("text/html") && url.replaceAll("\\?.*", "").contains(".m3u8")){
                    video = new SniffingVideo(url, s, 0, "m3u8");
                }
                //常用伪装格式，图像格式通常为fuck的image
                else if ((s.contains("image") && s.contains("fuck")) || (s.contains("image") && url.contains(".m3u8"))  || (s.isEmpty() && url.contains(".mp4")) || s.equals("image/fuck/you") || (url.contains("renrenmi") && url.endsWith(".m3u8"))){
                    video = new SniffingVideo(url, s, 0, url.contains("m3u8")?"m3u8":"mp4");
                }


            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return video;
    }

    private String getElement(String url){
        url = url.replaceAll("\\?.*", "");
        int last = url.lastIndexOf("/");
        if (last < url.length()){
            url = url.substring(last);
        }
        if (url.contains(".")){
            return url.substring(url.lastIndexOf("."));
        }
        return "none";
    }
}
