package net.doge.entity.service;

import lombok.Data;
import net.doge.constant.core.media.VideoQuality;
import net.doge.constant.core.os.Format;
import net.doge.constant.service.MvInfoType;
import net.doge.constant.service.source.NetResourceSource;
import net.doge.entity.service.base.Downloadable;
import net.doge.entity.service.base.NetResource;
import net.doge.util.core.StringUtil;
import net.doge.util.core.io.FileUtil;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Doge
 * @description MV
 * @date 2020/12/7
 */
@Data
public class NetMvInfo implements NetResource, Downloadable {
    // MV 来源
    private int source = NetResourceSource.NC;
    // 类型 (网易云分成 MV 视频 Mlog)
    private int type;
    // 播放格式
    private String playFormat = Format.MP4;
    // 下载格式
    private String downFormat = Format.MP4;
    // 播放画质
    private int playQuality;
    // 下载画质
    private int downQuality;
    // MV id
    private String id;
    // MV bvid (哔哩哔哩)
    private String bvId;
    // MV 名称
    private String name;
    // 创建者 id
    private String creatorId;
    // 艺术家
    private String artist;
    // 封面图片 url
    private String coverImgUrl;
    // 封面图片缩略图
    private BufferedImage coverImgThumb;
    // 播放 url
    private String playUrl;
    // 下载 url
    private String downUrl;
    // 播放量
    private Long playCount;
    // 时长
    private Double duration;
    // 发布时间
    private String pubTime;

    // 缩略图加载后的回调函数
    private Runnable invokeLater;

    private static final String SEPARATOR = " - ";

    public boolean isRealMV() {
        return type == MvInfoType.MV && source != NetResourceSource.HK && source != NetResourceSource.BI;
    }

    public boolean hasName() {
        return StringUtil.notEmpty(name);
    }

    public boolean hasArtist() {
        return StringUtil.notEmpty(artist);
    }

    public boolean hasDuration() {
        return duration != null && !Double.isNaN(duration) && !Double.isInfinite(duration) && duration != 0;
    }

    public boolean hasCreatorId() {
        return StringUtil.notEmpty(creatorId);
    }

    public boolean hasPubTime() {
        return StringUtil.notEmpty(pubTime);
    }

    public boolean hasPlayCount() {
        return playCount != null && playCount >= 0;
    }

    public void setCoverImgThumb(BufferedImage coverImgThumb) {
        this.coverImgThumb = coverImgThumb;
        callback();
    }

    private void callback() {
        if (invokeLater == null) return;
        invokeLater.run();
        // 调用后丢弃
        invokeLater = null;
    }

    // 判断 MV 信息是否完整
    public boolean isIntegrated() {
        return hasPlayUrl();
    }

    // 判断当前播放画质是否与设置的匹配
    public boolean isPlayQualityMatch() {
        return playQuality == VideoQuality.playQuality;
    }

    // 判断当前下载画质是否与设置的匹配
    public boolean isDownQualityMatch() {
        return downQuality == VideoQuality.downQuality;
    }

    public boolean isFlvPlayFormat() {
        return Format.FLV.equalsIgnoreCase(playFormat);
    }

    public boolean isMp4PlayFormat() {
        return Format.MP4.equalsIgnoreCase(playFormat);
    }

    public boolean isVideo() {
        return type == MvInfoType.VIDEO;
    }

    public boolean isMlog() {
        return type == MvInfoType.MLOG;
    }

    public boolean hasCoverImgThumb() {
        return coverImgThumb != null;
    }

    public boolean hasPlayUrl() {
        return StringUtil.notEmpty(playUrl);
    }

    public boolean hasDownUrl() {
        return StringUtil.notEmpty(downUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NetMvInfo) {
            NetMvInfo mvInfo = (NetMvInfo) o;
            return hashCode() == mvInfo.hashCode();
        }
        return false;
    }

    // 必须重写 hashCode 和 equals 方法才能在 Set 判断是否重复！
    @Override
    public int hashCode() {
        return Objects.hash(source, id, bvId);
    }

    // 用于播放的文件名
    public String toCacheFileName() {
        return FileUtil.filterFileName(toCacheFileBaseName() + "." + playFormat);
    }

    // 用于下载的文件名
    public String toDownloadFileName() {
        return FileUtil.filterFileName(toDownloadFileBaseName() + "." + downFormat);
    }

    public String toString() {
        return NetResourceSource.NAMES[source] + SEPARATOR + toSimpleString();
    }

    public String toSimpleString() {
        return StringUtil.shorten(name + (StringUtil.notEmpty(artist) ? SEPARATOR + artist : ""), 230);
    }

    public String toDownloadFileBaseName() {
        return toSimpleString();
    }

    public String toCacheFileBaseName() {
        return toDownloadFileBaseName() + SEPARATOR + id;
    }
}
