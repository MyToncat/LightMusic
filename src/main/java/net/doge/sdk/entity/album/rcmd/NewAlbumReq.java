package net.doge.sdk.entity.album.rcmd;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import net.doge.constant.async.GlobalExecutors;
import net.doge.constant.system.NetMusicSource;
import net.doge.model.entity.NetAlbumInfo;
import net.doge.sdk.common.CommonResult;
import net.doge.sdk.common.SdkCommon;
import net.doge.sdk.common.Tags;
import net.doge.sdk.util.SdkUtil;
import net.doge.util.collection.ListUtil;
import net.doge.util.common.JsonUtil;
import net.doge.util.common.RegexUtil;
import net.doge.util.common.StringUtil;
import net.doge.util.common.TimeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class NewAlbumReq {
    // 新碟上架 API
    private final String NEW_ALBUM_API = SdkCommon.PREFIX + "/top/album?area=%s";
    // 新碟上架(热门) API
//    private final String HOT_ALBUM_API = SdkCommon.PREFIX + "/top/album?type=hot&area=%s";
    // 全部新碟 API
    private final String ALL_NEW_ALBUM_API = SdkCommon.PREFIX + "/album/new?area=%s&offset=%s&limit=%s";
    // 最新专辑 API
    private final String NEWEST_ALBUM_API = SdkCommon.PREFIX + "/album/newest";
    // 数字新碟上架 API
    private final String NEWEST_DI_ALBUM_API = SdkCommon.PREFIX + "/album/list?limit=200";
    // 数字专辑语种风格馆 API
    private final String LANG_DI_ALBUM_API = SdkCommon.PREFIX + "/album/list/style?area=%s&limit=50";
    // 曲风专辑 API
    private final String STYLE_ALBUM_API = SdkCommon.PREFIX + "/style/album?tagId=%s&cursor=%s&size=%s";
    // 新碟推荐 API (QQ)
    private final String NEW_ALBUM_QQ_API
            = SdkCommon.PREFIX_QQ + "/new/album?type=%s&num=100";
    // 新碟推荐 API (咪咕)
    private final String NEW_ALBUM_MG_API
            = SdkCommon.PREFIX_MG + "/new/albums?pageNo=%s&pageSize=%s";
    // 新专辑榜 API (咪咕)
    private final String NEW_ALBUM_RANKING_MG_API
            = "https://app.c.nf.migu.cn/MIGUM2.0/v1.0/content/querycontentbyId.do?columnId=23218151";
    // 首页最新专辑 API (千千)
    private final String INDEX_NEW_ALBUM_QI_API
            = "https://music.91q.com/v1/index?appid=16073360&pageSize=12&timestamp=%s&type=song";
    // 秀动发行 API (千千)
    private final String XD_ALBUM_QI_API
            = "https://music.91q.com/v1//album/xdpublish?appid=16073360&module_name=秀动发行&moreApi=v1%%2Falbum%%2Fxdpublish" +
            "&pageNo=%s&pageSize=%s&timestamp=%s&type=showstart";
    // 新专辑推荐 API (千千)
    private final String NEW_ALBUM_QI_API
            = "https://music.91q.com/v1/album/list?appid=16073360&pageNo=%s&pageSize=%s&timestamp=%s";
    // 热门推荐专辑 API (堆糖)
    private final String REC_ALBUM_DT_API
            = "https://www.duitang.com/napi/index/hot/?include_fields=top_comments,is_root,source_link,item,buyable,root_id,status,like_count,sender,album" +
            "&start=%s&limit=%s&_=%s";
    // 分类专辑 API (堆糖)
    private final String CAT_ALBUM_DT_API
            = "https://www.duitang.com/napi/blog/list/by_filter_id/?include_fields=top_comments,is_root,source_link,item,buyable,root_id,status,like_count,sender,album,reply_count" +
            "&filter_id=%s&start=%s&limit=%s&_=%s";
    // Top 250 专辑 API (豆瓣)
    private final String TOP_ALBUM_DB_API
            = "https://music.douban.com/top250?start=%s";
    // 分类专辑 API (豆瓣)
    private final String CAT_ALBUM_DB_API
            = "https://music.douban.com/tag/%s?start=%s&type=T";

    // 歌曲封面信息 API (QQ)
    private final String SINGLE_SONG_IMG_QQ_API = "https://y.gtimg.cn/music/photo_new/T002R500x500M000%s.jpg";
    
    /**
     * 获取新碟上架
     */
    public CommonResult<NetAlbumInfo> getNewAlbums(int src, String tag, int limit, int page) {
        AtomicInteger total = new AtomicInteger();
        List<NetAlbumInfo> albumInfos = new LinkedList<>();

        final String defaultTag = "默认";
        String[] s = Tags.newAlbumTag.get(tag);

        // 网易云(程序分页)
        // 新蹀上架
        Callable<CommonResult<NetAlbumInfo>> getNewAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[0])) {
                String albumInfoBody = HttpRequest.get(String.format(NEW_ALBUM_API, s[0]))
                        .execute()
                        .body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                JSONArray albumArray = albumInfoJson.getJSONArray("weekData");
                JSONArray monthData = albumInfoJson.getJSONArray("monthData");
                if (JsonUtil.isEmpty(albumArray)) albumArray = monthData;
                else albumArray.addAll(monthData);
                t = albumArray.size();
                for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                    JSONObject albumJson = albumArray.getJSONObject(i);

                    String albumId = albumJson.getString("id");
                    String albumName = albumJson.getString("name");
                    String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.NET_CLOUD);
                    String artistId = albumJson.getJSONArray("artists").getJSONObject(0).getString("id");
                    String publishTime = TimeUtil.msToDate(albumJson.getLong("publishTime"));
                    Integer songNum = albumJson.getIntValue("size");
                    String coverImgThumbUrl = albumJson.getString("picUrl");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setArtistId(artistId);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    albumInfo.setPublishTime(publishTime);
                    albumInfo.setSongNum(songNum);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };
        // 新碟上架(热门)
//        Callable<CommonResult<NetAlbumInfo>> getHotAlbums = () -> {
//            LinkedList<NetAlbumInfo> res = new LinkedList<>();
//            Integer t = 0;
//
//            if (StringUtils.notEmpty(s[0])) {
//                String albumInfoBody = HttpRequest.get(String.format(HOT_ALBUM_API, s[0]))
//                        .execute()
//                        .body();
//                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
//                JSONArray albumArray = albumInfoJson.getJSONArray("weekData");
//                JSONArray monthData = albumInfoJson.getJSONArray("monthData");
//                if (JsonUtil.isEmpty(albumArray)) albumArray = monthData;
//                else albumArray.addAll(monthData);
//                t = albumArray.size();
//                for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
//                    JSONObject albumJson = albumArray.getJSONObject(i);
//
//                    String albumId = albumJson.getString("id");
//                    String albumName = albumJson.getString("name");
//                    String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.NET_CLOUD);
//                    String publishTime = TimeUtils.msToDate(albumJson.getLong("publishTime"));
//                    Integer songNum = albumJson.getIntValue("size");
//                    String coverImgThumbUrl = albumJson.getString("picUrl");
//
//                    NetAlbumInfo albumInfo = new NetAlbumInfo();
//                    albumInfo.setId(albumId);
//                    albumInfo.setName(albumName);
//                    albumInfo.setArtist(artist);
//                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
//                    albumInfo.setPublishTime(publishTime);
//                    albumInfo.setSongNum(songNum);
//                    GlobalExecutors.imageExecutor.execute(() -> {
//                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
//                        albumInfo.setCoverImgThumb(coverImgThumb);
//                    });
//
//                    res.add(albumInfo);
//                }
//            }
//            return new CommonResult<>(res, t);
//        };
        // 全部新碟(接口分页，与上面两个分开处理)
        Callable<CommonResult<NetAlbumInfo>> getAllNewAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[0])) {
                String albumInfoBody = HttpRequest.get(String.format(ALL_NEW_ALBUM_API, s[0], (page - 1) * limit, limit))
                        .execute()
                        .body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                t = albumInfoJson.getIntValue("total");
                JSONArray albumArray = albumInfoJson.getJSONArray("albums");
                for (int i = 0, len = albumArray.size(); i < len; i++) {
                    JSONObject albumJson = albumArray.getJSONObject(i);

                    String albumId = albumJson.getString("id");
                    String albumName = albumJson.getString("name");
                    String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.NET_CLOUD);
                    String artistId = albumJson.getJSONArray("artists").getJSONObject(0).getString("id");
                    String publishTime = TimeUtil.msToDate(albumJson.getLong("publishTime"));
                    Integer songNum = albumJson.getIntValue("size");
                    String coverImgThumbUrl = albumJson.getString("picUrl");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setArtistId(artistId);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    albumInfo.setPublishTime(publishTime);
                    albumInfo.setSongNum(songNum);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };
        // 最新专辑
        Callable<CommonResult<NetAlbumInfo>> getNewestAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(String.format(NEWEST_ALBUM_API))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONArray albumArray = albumInfoJson.getJSONArray("albums");
            t = albumArray.size();
            for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("id");
                String albumName = albumJson.getString("name");
                String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.NET_CLOUD);
                String artistId = albumJson.getJSONArray("artists").getJSONObject(0).getString("id");
                String publishTime = TimeUtil.msToDate(albumJson.getLong("publishTime"));
                Integer songNum = albumJson.getIntValue("size");
                String coverImgThumbUrl = albumJson.getString("picUrl");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });

                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 数字新碟上架
        Callable<CommonResult<NetAlbumInfo>> getNewestDiAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(String.format(NEWEST_DI_ALBUM_API))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONArray albumArray = albumInfoJson.getJSONArray("products");
            t = albumArray.size();
            for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("albumId");
                String albumName = albumJson.getString("albumName");
                String artist = albumJson.getString("artistName");
                String publishTime = TimeUtil.msToDate(albumJson.getLong("pubTime"));
                String coverImgThumbUrl = albumJson.getString("coverUrl");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });

                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 数字专辑语种风格馆
        Callable<CommonResult<NetAlbumInfo>> getLangDiAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[1])) {
                String albumInfoBody = HttpRequest.get(String.format(LANG_DI_ALBUM_API, s[1]))
                        .execute()
                        .body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                JSONArray albumArray = albumInfoJson.getJSONArray("albumProducts");
                t = albumArray.size();
                for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                    JSONObject albumJson = albumArray.getJSONObject(i);

                    String albumId = albumJson.getString("albumId");
                    String albumName = albumJson.getString("albumName");
                    String artist = albumJson.getString("artistName");
                    String coverImgThumbUrl = albumJson.getString("coverUrl");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };
        // 曲风专辑
        Callable<CommonResult<NetAlbumInfo>> getStyleAlbums = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[2])) {
                String albumInfoBody = HttpRequest.get(String.format(STYLE_ALBUM_API, s[2], (page - 1) * limit, limit))
                        .execute()
                        .body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                JSONObject data = albumInfoJson.getJSONObject("data");
                JSONArray albumArray = data.getJSONArray("albums");
                t = data.getJSONObject("page").getIntValue("total");
                for (int i = 0, len = albumArray.size(); i < len; i++) {
                    JSONObject albumJson = albumArray.getJSONObject(i);

                    String albumId = albumJson.getString("id");
                    String albumName = albumJson.getString("name");
                    String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.NET_CLOUD);
                    String artistId = albumJson.getJSONArray("artists").getJSONObject(0).getString("id");
                    String publishTime = TimeUtil.msToDate(albumJson.getLong("publishTime"));
                    Integer songNum = albumJson.getIntValue("size");
                    String coverImgThumbUrl = albumJson.getString("picUrl");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setArtistId(artistId);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    albumInfo.setPublishTime(publishTime);
                    albumInfo.setSongNum(songNum);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };

        // QQ(程序分页)
        Callable<CommonResult<NetAlbumInfo>> getNewAlbumsQq = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[3])) {
                String albumInfoBody = HttpRequest.get(String.format(NEW_ALBUM_QQ_API, s[3]))
                        .execute()
                        .body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                JSONArray albumArray = albumInfoJson.getJSONObject("data").getJSONArray("list");
                t = albumArray.size();
                for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                    JSONObject albumJson = albumArray.getJSONObject(i);

                    String albumId = albumJson.getString("mid");
                    String albumName = albumJson.getString("name");
                    String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.QQ);
                    String artistId = albumJson.getJSONArray("singers").getJSONObject(0).getString("mid");
                    String publishTime = albumJson.getString("release_time");
//            Integer songNum = albumJson.getJSONObject("ex").getIntValue("track_nums");
                    String coverImgThumbUrl = String.format(SINGLE_SONG_IMG_QQ_API, albumId);

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setSource(NetMusicSource.QQ);
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setArtistId(artistId);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    albumInfo.setPublishTime(publishTime);
//            albumInfo.setSongNum(songNum);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };

        // 咪咕
        // 新碟推荐(接口分页)
        Callable<CommonResult<NetAlbumInfo>> getNewAlbumsMg = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(String.format(NEW_ALBUM_MG_API, page, limit))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONObject("data");
            t = data.getIntValue("total");
            JSONArray albumArray = data.getJSONArray("list");
            for (int i = 0, len = albumArray.size(); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("id");
                String albumName = albumJson.getString("name");
                String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.MG);
                String artistId = albumJson.getJSONArray("artists").getJSONObject(0).getString("id");
                String publishTime = albumJson.getString("publishTime");
                Integer songNum = albumJson.getIntValue("songCount");
                String coverImgThumbUrl = albumJson.getString("picUrl");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.MG);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });

                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 新专辑榜(程序分页)
        Callable<CommonResult<NetAlbumInfo>> getNewAlbumsRankingMg = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(String.format(NEW_ALBUM_RANKING_MG_API))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONObject("columnInfo");
            t = data.getIntValue("contentsCount");
            JSONArray albumArray = data.getJSONArray("contents");
            for (int i = (page - 1) * limit, len = Math.min(albumArray.size(), page * limit); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i).getJSONObject("objectInfo");

                String albumId = albumJson.getString("albumId");
                String albumName = albumJson.getString("title");
                String artist = albumJson.getString("singer").replace("|", "、");
                String artistId = albumJson.getString("singerId").split("\\|")[0];
                String publishTime = albumJson.getString("publishTime");
                Integer songNum = albumJson.getIntValue("totalCount");
                String coverImgThumbUrl = albumJson.getJSONArray("imgItems").getJSONObject(0).getString("img");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.MG);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });

                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };

        // 千千
        // 首页新专辑
        Callable<CommonResult<NetAlbumInfo>> getIndexNewAlbumsQi = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(SdkCommon.buildQianUrl(String.format(INDEX_NEW_ALBUM_QI_API, System.currentTimeMillis())))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONArray("data").getJSONObject(3);
            JSONArray albumArray = data.getJSONArray("result");
            t = albumArray.size();
            for (int i = 0, len = albumArray.size(); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("albumAssetCode");
                String albumName = albumJson.getString("title");
                String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.QI);
                JSONArray artistArray = albumJson.getJSONArray("artist");
                String artistId = JsonUtil.notEmpty(artistArray) ? artistArray.getJSONObject(0).getString("artistCode") : "";
                String coverImgThumbUrl = albumJson.getString("pic");
                String releaseDate = albumJson.getString("releaseDate");
                if (StringUtil.isEmpty(releaseDate)) releaseDate = albumJson.getString("pushTime");
                String publishTime = releaseDate.split("T")[0];
                Integer songNum = albumJson.getJSONArray("trackList").size();

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.QI);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });
                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 秀动发行
        Callable<CommonResult<NetAlbumInfo>> getXDAlbumsQi = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(SdkCommon.buildQianUrl(String.format(XD_ALBUM_QI_API, page, limit, System.currentTimeMillis())))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONObject("data");
            t = data.getIntValue("total");
            JSONArray albumArray = data.getJSONArray("result");
            for (int i = 0, len = albumArray.size(); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("albumAssetCode");
                String albumName = albumJson.getString("title");
                String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.QI);
                JSONArray artistArray = albumJson.getJSONArray("artist");
                String artistId = JsonUtil.notEmpty(artistArray) ? artistArray.getJSONObject(0).getString("artistCode") : "";
                String coverImgThumbUrl = albumJson.getString("pic");
                String publishTime = albumJson.getString("releaseDate").split("T")[0];
                Integer songNum = albumJson.getIntValue("trackCount");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.QI);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });
                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 新专辑推荐
        Callable<CommonResult<NetAlbumInfo>> getNewAlbumsQi = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            String albumInfoBody = HttpRequest.get(SdkCommon.buildQianUrl(String.format(NEW_ALBUM_QI_API, page, limit, System.currentTimeMillis())))
                    .execute()
                    .body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONObject("data");
            t = data.getIntValue("total");
            JSONArray albumArray = data.getJSONArray("result");
            for (int i = 0, len = albumArray.size(); i < len; i++) {
                JSONObject albumJson = albumArray.getJSONObject(i);

                String albumId = albumJson.getString("albumAssetCode");
                String albumName = albumJson.getString("title");
                String artist = SdkUtil.parseArtists(albumJson, NetMusicSource.QI);
                JSONArray artistArray = albumJson.getJSONArray("artist");
                String artistId = JsonUtil.notEmpty(artistArray) ? artistArray.getJSONObject(0).getString("artistCode") : "";
                String coverImgThumbUrl = albumJson.getString("pic");
                String publishTime = albumJson.getString("releaseDate").split("T")[0];
                Integer songNum = albumJson.getIntValue("trackCount");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.QI);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });
                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };

        // 堆糖
        // 推荐专辑
        Callable<CommonResult<NetAlbumInfo>> getRecAlbumsDt = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            HttpResponse resp = HttpRequest.get(String.format(REC_ALBUM_DT_API, (page - 1) * limit, limit, System.currentTimeMillis())).execute();
            String albumInfoBody = resp.body();
            JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
            JSONObject data = albumInfoJson.getJSONObject("data");
            t = data.getIntValue("total");
            JSONArray albumArray = data.getJSONArray("object_list");
            for (int i = 0, len = albumArray.size(); i < len; i++) {
                JSONObject mainJson = albumArray.getJSONObject(i);
                JSONObject albumJson = mainJson.getJSONObject("album");

                String albumId = albumJson.getString("id");
                String albumName = albumJson.getString("name");
                String artist = mainJson.getJSONObject("sender").getString("username");
                String artistId = mainJson.getJSONObject("sender").getString("id");
//                String publishTime = TimeUtils.msToDate(mainJson.getLong("add_datetime_ts") * 1000);
                String coverImgThumbUrl = albumJson.getJSONArray("covers").getString(0);
                Integer songNum = albumJson.getIntValue("count");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.DT);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setArtistId(artistId);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
//                albumInfo.setPublishTime(publishTime);
                albumInfo.setSongNum(songNum);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });
                res.add(albumInfo);
            }

            return new CommonResult<>(res, t);
        };
        // 分类专辑
        Callable<CommonResult<NetAlbumInfo>> getCatAlbumsDt = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[5])) {
                HttpResponse resp = HttpRequest.get(String.format(CAT_ALBUM_DT_API, s[5], (page - 1) * limit, limit, System.currentTimeMillis())).execute();
                String albumInfoBody = resp.body();
                JSONObject albumInfoJson = JSONObject.parseObject(albumInfoBody);
                JSONObject data = albumInfoJson.getJSONObject("data");
                t = data.getIntValue("total");
                JSONArray albumArray = data.getJSONArray("object_list");
                for (int i = 0, len = albumArray.size(); i < len; i++) {
                    JSONObject mainJson = albumArray.getJSONObject(i);
                    JSONObject albumJson = mainJson.getJSONObject("album");

                    String albumId = albumJson.getString("id");
                    String albumName = albumJson.getString("name");
                    String artist = mainJson.getJSONObject("sender").getString("username");
                    String artistId = mainJson.getJSONObject("sender").getString("id");
                    String publishTime = TimeUtil.msToDate(mainJson.getLong("add_datetime_ts") * 1000);
                    String coverImgThumbUrl = albumJson.getJSONArray("covers").getString(0);
                    Integer songNum = albumJson.getIntValue("count");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setSource(NetMusicSource.DT);
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setArtistId(artistId);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    albumInfo.setPublishTime(publishTime);
                    albumInfo.setSongNum(songNum);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });
                    res.add(albumInfo);
                }
            }

            return new CommonResult<>(res, t);
        };

        // 豆瓣
        // Top 250
        Callable<CommonResult<NetAlbumInfo>> getTopAlbumsDb = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;
            final int rn = 25;

            String radioInfoBody = HttpRequest.get(String.format(TOP_ALBUM_DB_API, (page - 1) * rn))
                    .execute()
                    .body();
            Document doc = Jsoup.parse(radioInfoBody);
            Elements as = doc.select("tr.item");
            t -= 250 / rn * 5;
            for (int i = 0, len = as.size(); i < len; i++) {
                Element album = as.get(i);
                Elements a = album.select("div.pl2 a");
                Elements pl = album.select("div.pl2 p.pl");
                Elements img = album.select("td img");

                String albumId = RegexUtil.getGroup1("/subject/(\\d+)/", a.attr("href"));
                String albumName = a.text().trim();
                String[] sp = pl.text().split(" / ");
                String artist = sp[0];
                String pubTime = sp[1];
                String coverImgThumbUrl = img.attr("src");

                NetAlbumInfo albumInfo = new NetAlbumInfo();
                albumInfo.setSource(NetMusicSource.DB);
                albumInfo.setId(albumId);
                albumInfo.setName(albumName);
                albumInfo.setArtist(artist);
                albumInfo.setPublishTime(pubTime);
                albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                GlobalExecutors.imageExecutor.execute(() -> {
                    BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                    albumInfo.setCoverImgThumb(coverImgThumb);
                });

                res.add(albumInfo);
            }
            return new CommonResult<>(res, t);
        };
        // 分类专辑
        Callable<CommonResult<NetAlbumInfo>> getCatAlbumsDb = () -> {
            LinkedList<NetAlbumInfo> res = new LinkedList<>();
            Integer t = 0;

            if (StringUtil.notEmpty(s[4])) {
                String radioInfoBody = HttpRequest.get(String.format(CAT_ALBUM_DB_API, s[4], (page - 1) * limit))
                        .execute()
                        .body();
                Document doc = Jsoup.parse(radioInfoBody);
                Elements as = doc.select("tr.item");
                Element te = doc.select("div.paginator > a").last();
                String ts = te == null ? "" : te.text();
                t = StringUtil.notEmpty(ts) ? Integer.parseInt(ts) * limit : limit;
                for (int i = 0, len = as.size(); i < len; i++) {
                    Element album = as.get(i);
                    Elements a = album.select("div.pl2 a");
                    Elements pl = album.select("div.pl2 p.pl");
                    Elements img = album.select("td img");

                    String albumId = RegexUtil.getGroup1("/subject/(\\d+)/", a.attr("href"));
                    String albumName = a.text().trim();
                    String[] sp = pl.text().split(" / ");
                    String artist = sp[0];
                    String pubTime = sp[1];
                    String coverImgThumbUrl = img.attr("src");

                    NetAlbumInfo albumInfo = new NetAlbumInfo();
                    albumInfo.setSource(NetMusicSource.DB);
                    albumInfo.setId(albumId);
                    albumInfo.setName(albumName);
                    albumInfo.setArtist(artist);
                    albumInfo.setPublishTime(pubTime);
                    albumInfo.setCoverImgThumbUrl(coverImgThumbUrl);
                    GlobalExecutors.imageExecutor.execute(() -> {
                        BufferedImage coverImgThumb = SdkUtil.extractCover(coverImgThumbUrl);
                        albumInfo.setCoverImgThumb(coverImgThumb);
                    });

                    res.add(albumInfo);
                }
            }
            return new CommonResult<>(res, t);
        };

        List<Future<CommonResult<NetAlbumInfo>>> taskList = new LinkedList<>();

        boolean dt = defaultTag.equals(tag);

        if (dt) {
            if (src == NetMusicSource.NET_CLOUD || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewestAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewestDiAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getAllNewAlbums));
            }

            if (src == NetMusicSource.QQ || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbumsQq));
            }

            if (src == NetMusicSource.MG || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbumsMg));
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbumsRankingMg));
            }

            if (src == NetMusicSource.QI || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getIndexNewAlbumsQi));
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbumsQi));
                taskList.add(GlobalExecutors.requestExecutor.submit(getXDAlbumsQi));
            }

            if (src == NetMusicSource.DT || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getRecAlbumsDt));
            }

            if (src == NetMusicSource.DB || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getTopAlbumsDb));
            }
        } else {
            if (src == NetMusicSource.NET_CLOUD || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getAllNewAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getLangDiAlbums));
                taskList.add(GlobalExecutors.requestExecutor.submit(getStyleAlbums));
            }

            if (src == NetMusicSource.QQ || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getNewAlbumsQq));
            }

            if (src == NetMusicSource.DT || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getCatAlbumsDt));
            }

            if (src == NetMusicSource.DB || src == NetMusicSource.ALL) {
                taskList.add(GlobalExecutors.requestExecutor.submit(getCatAlbumsDb));
            }
        }
//        taskList.add(GlobalExecutors.requestExecutor.submit(getHotAlbums));

        List<List<NetAlbumInfo>> rl = new LinkedList<>();
        taskList.forEach(task -> {
            try {
                CommonResult<NetAlbumInfo> result = task.get();
                rl.add(result.data);
                total.set(Math.max(total.get(), result.total));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        albumInfos.addAll(ListUtil.joinAll(rl));

        return new CommonResult<>(albumInfos, total.get());
    }
}