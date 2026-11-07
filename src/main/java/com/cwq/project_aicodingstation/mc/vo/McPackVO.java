package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class McPackVO {
    private String id;
    private String name;
    private String description;
    /** 1.0.0，或「行为 1.0.0 · 资源 1.2.0」 */
    private String version;
    /** resources / behavior / addon */
    private String kind;
    private boolean hasScript;
    /** 依赖了带 beta 的脚本接口。不会自动改世界实验项。 */
    private boolean experimentHint;
    private boolean missingDependency;
    private String dependencyNote;
    private boolean hasIcon;
    private String iconUrl;
    private long sizeBytes;
    /** 资源部分超过 12MB，手机加入可能较慢。 */
    private boolean largeForMobile;
    private String authors;
    private String minEngine;
    /** 已经写入 world_*_packs.json 的存档文件夹。 */
    private List<String> enabledWorlds;
}
