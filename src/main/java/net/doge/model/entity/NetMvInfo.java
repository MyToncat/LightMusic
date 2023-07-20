package net.doge.model.entity;

import net.doge.constant.player.Format;
import net.doge.constant.model.MvInfoType;
import net.doge.constant.system.NetMusicSource;
import lombok.Data;
import net.doge.util.system.FileUtil;
import net.doge.util.common.StringUtil;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @Author yzx
 * @Description MV
 * @Date 2020/12/7
 */
@Data
public class NetMvInfo {
    // MV 来源
    private int source = NetMusicSource.NET_CLOUD;
    // 类型 (网易云分成 MV 视频 Mlog)
    private int type;
    // 视频格式
    private String format = Format.MP4;
    // MV id
    private String id;
    // MV bvid (哔哩哔哩)
    private String bvid;
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
    // url
    private String url;
    // 播放量
    private Long playCount;
    // 时长
    private Double duration;
    // 发布时间
    private String pubTime;

    // 缩略图加载后的回调函数
    private Runnable invokeLater;

    public boolean isRealMV() {
        return type == MvInfoType.MV && source != NetMusicSource.HK && source != NetMusicSource.BI;
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
        if (invokeLater != null) {
            invokeLater.run();
            // 调用后丢弃
            invokeLater = null;
        }
    }

    /**
     * 判断 MV 信息是否完整
     *
     * @return
     */
    public boolean isIntegrated() {
        return url != null;
    }

    public void setFormat(String format) {
        this.format = StringUtil.notEmpty(format) ? format : Format.MP4;
    }

    public boolean isFlv() {
        return Format.FLV.equals(format);
    }

    public boolean isMp4() {
        return Format.MP4.equals(format);
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof NetMvInfo) {
            NetMvInfo netMvInfo = (NetMvInfo) o;
            return hashCode() == netMvInfo.hashCode();
        }
        return false;
    }

    // 必须重写 hashCode 和 equals 方法才能在 Set 判断是否重复！
    @Override
    public int hashCode() {
        return Objects.hash(source, id, bvid);
    }

    public String toFileName() {
        return FileUtil.filterFileName(String.format("%s - %s.%s", toSimpleString(), id, format));
    }

    public String toSimpleFileName() {
        return FileUtil.filterFileName(String.format("%s.%s", toSimpleString(), format));
    }

    public String toString() {
        return NetMusicSource.NAMES[source] + " - " + toSimpleString()
                + (playCount == null ? "" : "\n\n" + StringUtil.formatNumber(playCount));
    }

    public String toSimpleString() {
        return StringUtil.shorten(name + " - " + artist, 230);
    }
}