//package net.doge.sdk.service.music.info.impl.musicurl.track.qq;
//
//import com.alibaba.fastjson2.JSONObject;
//import net.doge.constant.core.media.AudioQuality;
//import net.doge.util.core.StringUtil;
//import net.doge.util.core.http.HttpRequest;
//import net.doge.util.core.json.JsonUtil;
//import net.doge.util.core.log.LogUtil;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class YgkingQqTrackReq {
//    private static YgkingQqTrackReq instance;
//
//    private YgkingQqTrackReq() {
//        initMap();
//    }
//
//    public static YgkingQqTrackReq getInstance() {
//        if (instance == null) instance = new YgkingQqTrackReq();
//        return instance;
//    }
//
//    // 歌曲 URL 获取 API (QQ)
//    private final String SONG_URL_QQ_API = "https://api.ygking.top/api/song/url?mid=%s&quality=%s";
//
//    private Map<String, String> qualityMap = new HashMap<>();
//
//    private void initMap() {
//        // 标准品质
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.STANDARD], "320");
//        // HQ
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.HIGH], "320");
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.SUPER], "320");
//        // 无损
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.LOSSLESS], "flac");
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.HI_RES], "flac");
//        // 至臻全景声
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.ATMOSPHERE], "atmos");
//        // 至臻母带
//        qualityMap.put(AudioQuality.KEYS[AudioQuality.MASTER], "master");
//    }
//
//    /**
//     * 获取 QQ 音乐歌曲链接
//     *
//     * @param mid     歌曲 mid
//     * @param quality 品质
//     * @return
//     */
//    public String getTrackUrl(String mid, String quality) {
//        try {
//            String songBody = HttpRequest.get(String.format(SONG_URL_QQ_API, mid, qualityMap.get(quality)))
//                    .executeAsStr();
//            JSONObject songJson = JSONObject.parseObject(songBody);
//            if (songJson.getIntValue("code") != 0) return "";
//            JSONObject data = songJson.getJSONObject("data");
//            if (JsonUtil.isEmpty(data)) return "";
//            String trackUrl = data.getString("music");
//            if (StringUtil.isEmpty(trackUrl)) return "";
//            return trackUrl;
//        } catch (Exception e) {
//            LogUtil.error(e);
//            return "";
//        }
//    }
//
//    public static void main(String[] args) {
//        YgkingQqTrackReq trackHero = getInstance();
//        System.out.println(trackHero.getTrackUrl("001CnSwn2xF1ee", AudioQuality.KEYS[AudioQuality.STANDARD]));
//        System.out.println(trackHero.getTrackUrl("001CnSwn2xF1ee", AudioQuality.KEYS[AudioQuality.HIGH]));
//        System.out.println(trackHero.getTrackUrl("0039MnYb0qxYhV", AudioQuality.KEYS[AudioQuality.LOSSLESS]));
//    }
//}
