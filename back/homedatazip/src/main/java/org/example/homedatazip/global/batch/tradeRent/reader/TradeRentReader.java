package org.example.homedatazip.global.batch.tradeRent.reader;

import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeRent.api.RentApiClient;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.springframework.batch.item.*;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class TradeRentReader implements ItemStreamReader<RentApiItem> {

    private static final String CTX_SGG_IDX = "trBackfill.sggIdx";
    private static final String CTX_YMD_IDX = "trBackfill.ymdIdx";
    private static final String CTX_PAGE_NO = "trBackfill.pageNo";
    private static final String CTX_CURSOR = "trBackfill.cursor";

    private static final Set<String> ALLOWED_SIDO_PREFIX = Set.of("11", "28", "41");

    private final RentApiClient client;
    private final List<String> sggCds;
    private final List<String> dealYmds;

    private int sggIdx;
    private int ymdIdx;
    private int pageNo;
    private int maxPage;
    private int cursorInPage;

    private Iterator<RentApiItem> iter;

    public TradeRentReader(RentApiClient client, List<String> sggCds, List<String> dealYmds) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.sggCds = Objects.requireNonNull(sggCds, "sggCds must not be null");
        this.dealYmds = Objects.requireNonNull(dealYmds, "dealYmds must not be null");
        this.sggIdx = 0;
        this.ymdIdx = 0;
        this.pageNo = 1;
        this.maxPage = Integer.MAX_VALUE;
        this.cursorInPage = 0;
    }


    @Override
    public RentApiItem read() {
        while (true) {
            if(hasMore() && !isAllowedSidoBySggCd(currentSggCd())){
                advanceSggOnly();
                continue;
            }
            if (iter != null && iter.hasNext()) {
                RentApiItem next = iter.next();
                cursorInPage++;

                String itemSggCd = tryGetSggCd(next);
                if(itemSggCd == null || isAllowedSidoBySggCd(itemSggCd)){
                    return next;
                }
                continue;
            }

            if (!hasMore()) return null;

            if (pageNo > maxPage) {
                advance();
                continue;
            }

            loadPage();

            if (iter == null || !iter.hasNext()) {
                pageNo++;
            }
        }
    }

    private boolean hasMore() {
        return sggIdx < sggCds.size() && ymdIdx < dealYmds.size();
    }

    private String currentSggCd() {
        if (sggIdx < 0 || sggIdx >= sggCds.size()) return null;
        return sggCds.get(sggIdx);
    }
    private static boolean isAllowedSidoBySggCd(String sggCd) {
        if (sggCd == null) return false;
        String t = sggCd.trim();
        if (t.length() < 2) return false;
        return ALLOWED_SIDO_PREFIX.contains(t.substring(0, 2));
    }

    private static String tryGetSggCd(RentApiItem item) {
         return item.getSggCd();
    }

    private void advanceSggOnly() {
        iter = null;
        cursorInPage = 0;
        pageNo = 1;
        maxPage = Integer.MAX_VALUE;

        ymdIdx = 0;
        sggIdx++;
    }

    private void advance() {
        iter = null;
        cursorInPage = 0;
        pageNo = 1;
        maxPage = Integer.MAX_VALUE;

        ymdIdx++;
        if (ymdIdx >= dealYmds.size()) {
            ymdIdx = 0;
            sggIdx++;
        }
    }

    private void loadPage() {
        String sgg = sggCds.get(sggIdx);
        String ymd = dealYmds.get(ymdIdx);

        ApiResponse<RentApiItem> res = client.fetch(sgg, ymd, pageNo);
        if (res == null || res.body() == null || res.body().items() == null) {
            iter = List.<RentApiItem>of().iterator();
            return;
        }

        Integer totalCount = res.body().totalCount();
        Integer numOfRows = res.body().numOfRows();
        maxPage = computeMaxPage(totalCount, numOfRows);

        List<RentApiItem> items = res.body().items().safeItem();
        Iterator<RentApiItem> it = items.iterator();

        int skip = cursorInPage;
        cursorInPage = 0;
        for (int i = 0; i < skip && it.hasNext(); i++) it.next();

        iter = it;
    }

    private static int computeMaxPage(Integer totalCount, Integer numOfRows) {
        if (totalCount == null || numOfRows == null || numOfRows <= 0) return Integer.MAX_VALUE;
        if (totalCount <= 0) return 0;
        return (int) Math.ceil(totalCount / (double) numOfRows);
    }

    @Override
    public void open(ExecutionContext executionContext) {
        this.sggIdx = executionContext.getInt(CTX_SGG_IDX, 0);
        this.ymdIdx = executionContext.getInt(CTX_YMD_IDX, 0);
        this.pageNo = executionContext.getInt(CTX_PAGE_NO, 1);
        this.cursorInPage = executionContext.getInt(CTX_CURSOR, 0);
        this.iter = null;
        this.maxPage = Integer.MAX_VALUE;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(CTX_SGG_IDX, sggIdx);
        executionContext.putInt(CTX_YMD_IDX, ymdIdx);
        executionContext.putInt(CTX_PAGE_NO, pageNo);
        executionContext.putInt(CTX_CURSOR, cursorInPage);
    }

    @Override
    public void close() {
        iter = null;
    }
}

