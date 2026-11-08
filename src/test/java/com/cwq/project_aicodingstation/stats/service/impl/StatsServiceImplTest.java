package com.cwq.project_aicodingstation.stats.service.impl;

import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.stats.dto.VisitHitRequest;
import com.cwq.project_aicodingstation.stats.entity.VisitLog;
import com.cwq.project_aicodingstation.stats.mapper.VisitDailyMapper;
import com.cwq.project_aicodingstation.stats.mapper.VisitLogMapper;
import com.cwq.project_aicodingstation.stats.support.StatsJson;
import com.cwq.project_aicodingstation.stats.support.StatsKeys;
import com.cwq.project_aicodingstation.stats.support.StatsRules;
import com.cwq.project_aicodingstation.stats.vo.StatsOverviewVO;
import com.cwq.project_aicodingstation.stats.vo.StatsTrendPointVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    private static final String CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;
    @Mock
    private SetOperations<String, String> sets;
    @Mock
    private ZSetOperations<String, String> zset;
    @Mock
    private VisitLogMapper visitLogMapper;
    @Mock
    private VisitDailyMapper visitDailyMapper;
    @Mock
    private AppMapper appMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private StatsServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(redis.opsForSet()).thenReturn(sets);
        lenient().when(redis.opsForZSet()).thenReturn(zset);
        lenient().when(request.getRemoteAddr()).thenReturn("203.0.113.8");
        lenient().when(request.getHeader("User-Agent")).thenReturn(CHROME);
        lenient().when(request.getSession(true)).thenReturn(session);
        lenient().when(session.getId()).thenReturn("session-1");
        service = new StatsServiceImpl(redis, visitLogMapper, visitDailyMapper, appMapper);
    }

    @Test
    void botIsDroppedBeforeASessionOrAWrite() {
        when(request.getHeader("User-Agent")).thenReturn("curl/8.4.0");

        assertFalse(service.record(siteView("/"), request));

        verify(request, never()).getSession(true);
        verifyNoInteractions(visitDailyMapper, visitLogMapper);
    }

    @Test
    void sameVisitorOnALaterSessionAddsPageViewsButNotAnotherUv() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(StatsRules.PAGE_VIEW_DEDUP_MINUTES), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(sets.add(anyString(), anyString())).thenReturn(1L, 0L);
        when(session.getId()).thenReturn("session-1", "session-2");

        assertTrue(service.record(siteView("/"), request));
        assertTrue(service.record(siteView("/about"), request));

        verify(visitDailyMapper).upsertHit(any(LocalDate.class), eq("SITE"), eq(0L), eq(1));
        verify(visitDailyMapper).upsertHit(any(LocalDate.class), eq("SITE"), eq(0L), eq(0));
        verify(visitDailyMapper, never()).upsertHit(any(), eq("APP"), anyLong(), any(Integer.class));
    }

    @Test
    void repeatedViewInTheSameSessionDoesNotAddAnotherPageView() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(StatsRules.PAGE_VIEW_DEDUP_MINUTES), eq(TimeUnit.MINUTES)))
                .thenReturn(true, false);
        when(sets.add(anyString(), anyString())).thenReturn(1L);

        assertTrue(service.record(siteView("/"), request));
        assertFalse(service.record(siteView("/"), request));

        verify(visitDailyMapper, times(1)).upsertHit(any(LocalDate.class), eq("SITE"), eq(0L), eq(1));
        verify(zset, times(2)).add(eq(StatsKeys.online()), anyString(), anyDouble());
    }

    @Test
    void appViewDoesNotChangeSiteCounters() {
        App app = new App();
        app.setId(99L);
        app.setAppName("演示");
        when(appMapper.selectOneById(99L)).thenReturn(app);
        when(values.setIfAbsent(anyString(), eq("1"), eq(StatsRules.PAGE_VIEW_DEDUP_MINUTES), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(sets.add(anyString(), anyString())).thenReturn(1L);

        VisitHitRequest hit = new VisitHitRequest();
        hit.setKind("VIEW");
        hit.setScope("APP");
        hit.setAppId("99");
        hit.setPath("/app/99?from=home");

        assertTrue(service.record(hit, request));

        verify(visitDailyMapper).upsertHit(any(LocalDate.class), eq("APP"), eq(99L), eq(1));
        verify(visitDailyMapper, never()).upsertHit(any(), eq("SITE"), anyLong(), any(Integer.class));
        ArgumentCaptor<VisitLog> stored = ArgumentCaptor.forClass(VisitLog.class);
        verify(visitLogMapper).insert(stored.capture());
        assertEquals("APP", stored.getValue().getScope());
        assertEquals(99L, stored.getValue().getAppId());
        assertEquals("/app/99", stored.getValue().getPath());
    }

    @Test
    void missingAppIsIgnored() {
        when(appMapper.selectOneById(5L)).thenReturn(null);

        VisitHitRequest hit = new VisitHitRequest();
        hit.setKind("VIEW");
        hit.setScope("APP");
        hit.setAppId("5");
        hit.setPath("/app/5");

        assertFalse(service.record(hit, request));

        verify(values, never()).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verifyNoInteractions(visitDailyMapper, visitLogMapper);
    }

    @Test
    void overviewUsesCachedSnapshot() throws Exception {
        StatsTrendPointVO point = new StatsTrendPointVO();
        point.setDate(LocalDate.of(2026, 11, 8));
        point.setPv(4);
        point.setUv(2);
        StatsOverviewVO sample = new StatsOverviewVO();
        sample.setTodayPv(4);
        sample.setTodayUv(2);
        sample.setOnline(1);
        sample.setDays(7);
        sample.setTrend(List.of(point));
        when(values.get(StatsKeys.overview(7))).thenReturn(StatsJson.mapper().writeValueAsString(sample));

        StatsOverviewVO got = service.overview(7);

        assertEquals(4, got.getTodayPv());
        assertEquals(1, got.getOnline());
        assertEquals(LocalDate.of(2026, 11, 8), got.getTrend().getFirst().getDate());
        verifyNoInteractions(visitDailyMapper);
        verify(zset, never()).zCard(anyString());
    }

    @Test
    void overviewRejectsUnsupportedRanges() {
        assertThrows(BusinessException.class, () -> service.overview(14));
    }

    private static VisitHitRequest siteView(String path) {
        VisitHitRequest hit = new VisitHitRequest();
        hit.setKind("VIEW");
        hit.setScope("SITE");
        hit.setPath(path);
        return hit;
    }
}
