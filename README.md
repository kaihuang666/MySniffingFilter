# MySniffingFilter
MySniffingFilter 使用教程
#### 1、webview引入
            @Override
            //为了headers能够正确写入，建议使用Request的拦截
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                try {
		                //播放地址
                    String url = webResourceRequest.getUrl().toString();
                    //请求头
                    Map<String, String> headers = webResourceRequest.getRequestHeaders();
                    //过滤器过滤
                    mFilter.addHeaders(headers);
                    //得到视频本体
                    SniffingVideo video =  mFilter.onFilter(webView, url);
                    if (video!=null){
                        Log.d("sniffed video:", video.getUrl());
                        boolean hasReferer = headers.containsKey("Referer");
                        boolean hasOrigin = headers.containsKey("Origin");
                        HttpReferer referer = HttpReferer.getInstance(video.getUrl(), video.getUrl());
                        Map<String, String> maps = referer.getMap();
                        if ((hasReferer || hasOrigin) && maps.size() == 0){
                            //一般保留Referer和Origin就能保证视频播放
                            if (hasReferer && hasOrigin) {
                                maps = new HashMap<>(2);
                                maps.put("Referer", headers.get("Referer"));
                                maps.put("Origin", headers.get("Origin"));
                            }else {
                                maps = new HashMap<>(1);
                                if (hasReferer)
                                    maps.put("Referer", headers.get("Referer"));
                                else
                                    maps.put("Origin", headers.get("Origin"));
                            }
                            video.addHeaders(maps);
                        }
                        //这里可以设置一个嗅探图标显示sniff.setImageResource(R.drawable.video_sniffed);
                        //添加到嗅探列表或直接播放自己修改
                        add(video);
                        //如果需要禁止视频的加载，就返回空请求（建议）
                        return new WebResourceResponse(null, null, null);
                    }
                    //信息不完整，只知道是视频，不知道类型
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                return super.shouldInterceptRequest(webView, webResourceRequest);
            }
#### 2、功能说明
- （1）嗅探bilibilivideo的flv文件，请求头需要自行加上（Referer=B站官方链接即可）
- （2）嗅探包含vkey的mp4及m3u8文件，一些解析接口常用
- （3）嗅探video/mp4、video/mpeg等二进制流媒体
- （4）嗅探video/mpeg-url类的HLS流媒体
- （5）嗅探伪装成image的m3u8清单文件
- （6）嗅探filename为mp4的服务器请求文件
- （7）嗅探一些为了防盗链伪装contentType为**fuck的清单文件
- 综合：基本上市面上可以播放的文件都可以嗅探到，建议使用TBS（X5内核）嗅探并播放，也可以使用ijkPlayer播放（m3u8缓存效果差，需要关闭缓存）。其中判断Content-type使用Jsoup的Response完成，完全等同于HttpUrlConnection的使用，可以自己替换Util类
#### 3、扩展
作为videoApp的一部分，可以用来后台嗅探播放也可以用来当作浏览器嗅探工具

