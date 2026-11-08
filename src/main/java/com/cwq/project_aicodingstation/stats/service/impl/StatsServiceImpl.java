package com.cwq.project_aicodingstation.stats.service.impl;

import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.stats.dto.VisitHitRequest;
import com.cwq.project_aicodingstation.stats.entity.VisitDaily;
import com.cwq.project_aicodingstation.stats.entity.VisitLog;
import com.cwq.project_aicodingstation.stats.enums.VisitKind;
import com.cwq.project_aicodingstation.stats.enums.VisitScope;
import com.cwq.project_aicodingstation.stats.mapper.VisitDailyMapper;
import com.cwq.project_aicodingstation.stats.mapper.VisitLogMapper;
import com.cwq.project_aicodingstation.stats.service.StatsService;
import com.cwq.project_aicodingstation.stats.support.BotUserAgents;
import com.cwq.project_aicodingstation.stats.support.StatsJson;
import com.cwq.project_aicodingstation.stats.support.StatsKeys;
import com.cwq.project_aicodingstation.stats.support.StatsRules;
import com.cwq.project_aicodingstation.stats.support.VisitHashes;
import com.cwq.project_aicodingstation.stats.vo.StatsAppRankVO;
import com.cwq.project_aicodingstation.stats.vo.StatsOverviewVO;
import com.cwq.project_aicodingstation.stats.vo.StatsTrendPointVO;
import com.cwq.project_aicodingstation.user.security.ClientIpResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 浏览量写入 MySQL 汇总，独立访客和在线人数放在 Redis。
 * 管理端概览缓存 60 秒，命中时只读这一份快照。
 */
@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    private final StringRedisTemplate redis;
    private final VisitLogMapper visitLogMapper;
    private final VisitDailyMapper visitDailyMapper;
    private final AppMapper appMapper;

    public StatsServiceImpl(StringRedisTemplate redis,
                            VisitLogMapper visitLogMapper,
                            VisitDailyMapper visitDailyMapper,
                            AppMapper appMapper) {
        this.redis = redis;
        this.visitLogMapper = visitLogMapper;
        this.visitDailyMapper = visitDailyMapper;
        this.appMapper = appMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean record(VisitHitRequest request, HttpServletRequest httpRequest) {
        if (request == null || BotUserAgents.isBot(httpRequest.getHeader("User-Agent"))) {
            return false;
        }
        VisitKind kind = VisitKind.parse(request.getKind());
        if (kind == null) {
            return false;
        }
        VisitScope scope = null;
        long appId = 0L;
        if (kind == VisitKind.VIEW) {
            scope = VisitScope.parse(request.getScope());
            if (scope == null) {
                return false;
            }
            if (scope == VisitScope.APP) {
                Long parsed = parseAppId(request.getAppId());
                if (parsed == null || appMapper.selectOneById(parsed) == null) {
                    return false;
                }
                appId = parsed;
            }
        }

        String sessionHash = VisitHashes.sha256(httpRequest.getSession(true).getId());
        if (kind == VisitKind.HEARTBEAT) {
            touchOnline(sessionHash);
            return false;
        }
        if (scope == null) {
            return false;
        }

        String scopeName = scope.name();
        String dedupKey = StatsKeys.pageViewDedup(sessionHash, scopeName, appId);
        Boolean fresh = redis.opsForValue().setIfAbsent(
                dedupKey, "1", StatsRules.PAGE_VIEW_DEDUP_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(fresh)) {
            touchOnline(sessionHash);
            return false;
        }

        String visitorHash = VisitHashes.sha256(visitorMaterial(httpRequest));
        String uvKey = StatsKeys.uv(LocalDate.now(StatsRules.ZONE), scopeName, appId);
        boolean reservedUv = false;
        try {
            Long added = redis.opsForSet().add(uvKey, visitorHash);
            reservedUv = added != null && added == 1L;
            redis.expire(uvKey, StatsRules.UV_TTL_HOURS, TimeUnit.HOURS);
            writeHit(scopeName, appId, sanitizePath(request.getPath()), visitorHash, sessionHash, reservedUv ? 1 : 0);
        } catch (RuntimeException ex) {
            redis.delete(dedupKey);
            if (reservedUv) {
                redis.opsForSet().remove(uvKey, visitorHash);
            }
            throw ex;
        }
        touchOnline(sessionHash);
        return true;
    }

    @Override
    public StatsOverviewVO overview(int days) {
        if (days != 7 && days != 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "天数只支持 7 或 30");
        }
        String cacheKey = StatsKeys.overview(days);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            try {
                return StatsJson.mapper().readValue(cached, StatsOverviewVO.class);
            } catch (JsonProcessingException ex) {
                log.warn("统计缓存无法解析，改为重算");
            }
        }
        StatsOverviewVO overview = buildOverview(days);
        try {
            redis.opsForValue().set(
                    cacheKey,
                    StatsJson.mapper().writeValueAsString(overview),
                    StatsRules.OVERVIEW_CACHE_SECONDS,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            log.warn("统计缓存写入失败");
        }
        return overview;
    }

    @Override
    public Map<Long, Long> appPageViews(Collection<Long> appIds) {
        if (appIds == null || appIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = appIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> views = new HashMap<>();
        for (VisitDaily row : visitDailyMapper.sumAppPageViews(ids)) {
            if (row.getAppId() != null) {
                views.put(row.getAppId(), row.getPv() == null ? 0L : row.getPv());
            }
        }
        return views;
    }

    @Override
    public int purgeExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now(StatsRules.ZONE).minusDays(StatsRules.LOG_RETENTION_DAYS);
        return visitLogMapper.deleteByQuery(QueryWrapper.create().where("visited_at < ?", cutoff));
    }

    private void writeHit(String scope, long appId, String path, String visitorHash, String sessionHash, int uvDelta) {
        LocalDateTime now = LocalDateTime.now(StatsRules.ZONE);
        LocalDate today = now.toLocalDate();
        VisitLog row = VisitLog.builder()
                .visitedAt(now)
                .statDate(today)
                .scope(scope)
                .appId(VisitScope.SITE.name().equals(scope) ? null : appId)
                .path(path)
                .visitorHash(visitorHash)
                .sessionHash(sessionHash)
                .build();
        visitLogMapper.insert(row);
        visitDailyMapper.upsertHit(today, scope, appId, uvDelta);
    }

    private StatsOverviewVO buildOverview(int days) {
        LocalDate today = LocalDate.now(StatsRules.ZONE);
        LocalDate from = today.minusDays(days - 1L);
        List<VisitDaily> rows = visitDailyMapper.listBetween(from, today);

        Map<LocalDate, long[]> siteByDate = new HashMap<>();
        Map<Long, long[]> appTotals = new HashMap<>();
        for (VisitDaily row : rows) {
            if (row.getStatDate() == null || row.getScope() == null) {
                continue;
            }
            long pv = row.getPv() == null ? 0L : row.getPv();
            long uv = row.getUv() == null ? 0L : row.getUv();
            long rowAppId = row.getAppId() == null ? 0L : row.getAppId();
            if (VisitScope.SITE.name().equals(row.getScope()) && rowAppId == 0L) {
                siteByDate.merge(row.getStatDate(), new long[]{pv, uv}, StatsServiceImpl::addPair);
            } else if (VisitScope.APP.name().equals(row.getScope()) && rowAppId > 0L) {
                appTotals.merge(rowAppId, new long[]{pv, uv}, StatsServiceImpl::addPair);
            }
        }

        StatsOverviewVO overview = new StatsOverviewVO();
        overview.setDays(days);
        overview.setOnline(countOnline());
        List<StatsTrendPointVO> trend = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            long[] pair = siteByDate.getOrDefault(date, new long[]{0L, 0L});
            StatsTrendPointVO point = new StatsTrendPointVO();
            point.setDate(date);
            point.setPv(pair[0]);
            point.setUv(pair[1]);
            trend.add(point);
            if (date.equals(today)) {
                overview.setTodayPv(pair[0]);
                overview.setTodayUv(pair[1]);
            }
        }
        overview.setTrend(trend);
        overview.setRanking(rankApps(appTotals));
        return overview;
    }

    private List<StatsAppRankVO> rankApps(Map<Long, long[]> appTotals) {
        List<Long> ids = appTotals.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, long[]>>comparingLong(entry -> entry.getValue()[0])
                        .reversed()
                        .thenComparing(Comparator.<Map.Entry<Long, long[]>>comparingLong(entry -> entry.getValue()[1]).reversed()))
                .map(Map.Entry::getKey)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (App app : appMapper.selectListByIds(ids)) {
            if (app.getId() != null) {
                names.put(app.getId(), app.getAppName());
            }
        }
        List<StatsAppRankVO> ranking = new ArrayList<>();
        for (Long id : ids) {
            long[] pair = appTotals.get(id);
            StatsAppRankVO item = new StatsAppRankVO();
            item.setAppId(Long.toString(id));
            String name = names.get(id);
            item.setAppName(name == null || name.isBlank() ? "已删除的应用" : name);
            item.setPv(pair[0]);
            item.setUv(pair[1]);
            ranking.add(item);
        }
        return ranking;
    }

    private long countOnline() {
        long cutoff = System.currentTimeMillis() - StatsRules.ONLINE_WINDOW_MILLIS;
        redis.opsForZSet().removeRangeByScore(StatsKeys.online(), 0, cutoff);
        Long count = redis.opsForZSet().zCard(StatsKeys.online());
        return count == null ? 0L : count;
    }

    private void touchOnline(String sessionHash) {
        redis.opsForZSet().add(StatsKeys.online(), sessionHash, System.currentTimeMillis());
    }

    private static long[] addPair(long[] left, long[] right) {
        return new long[]{left[0] + right[0], left[1] + right[1]};
    }

    private static String visitorMaterial(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }
        userAgent = userAgent.trim();
        if (userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 512);
        }
        return ip + "\n" + userAgent;
    }

    static Long parseAppId(String raw) {
        if (raw == null || !raw.matches("\\d{1,19}")) {
            return null;
        }
        try {
            long id = Long.parseLong(raw);
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String sanitizePath(String raw) {
        String path = raw == null ? "/" : raw.trim();
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path.length() > 256 ? path.substring(0, 256) : path;
    }
}
