package org.example.homedatazip.global.batch.tradeRent.reader;

import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeRent.api.MolitRentClient;
import org.example.homedatazip.tradeRent.dto.MolitRentApiItemResponse;
import org.springframework.batch.item.*;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;


public class tradeRentReader implements ItemStreamReader<MolitRentApiItemResponse> {

    private static final String CTX_SGG_IDX = "trBackfill.sggIdx";
    private static final String CTX_YMD_IDX = "trBackfill.ymdIdx";
    private static final String CTX_PAGE_NO = "trBackfill.pageNo";
    private static final String CTX_CURSOR = "trBackfill.cursor";

    private final MolitRentClient client;
    private final List<String> sggCds;
    private final List<String> dealYmds;

    private int sggIdx;
    private int ymdIdx;
    private int pageNo;
    private int maxPage;
    private int cursorInPage;

    private Iterator<MolitRentApiItemResponse> iter;

    public tradeRentReader(MolitRentClient client, List<String> sggCds, List<String> dealYmds) {
        this.client = Objects.requireNonNull(client);
        this.sggCds = Objects.requireNonNull(sggCds);
        this.dealYmds = Objects.requireNonNull(dealYmds);
        this.sggIdx = 0;
        this.ymdIdx = 0;
        this.pageNo = 1;
        this.maxPage = Integer.MAX_VALUE;
        this.cursorInPage = 0;
    }


    @Override
    public MolitRentApiItemResponse read() {
        while (true) {
            if (iter != null && iter.hasNext()) {
                cursorInPage++;
                return iter.next();
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

        ApiResponse<MolitRentApiItemResponse> res = client.fetch(sgg, ymd, pageNo);
        if (res == null || res.body() == null || res.body().items() == null) {
            iter = List.<MolitRentApiItemResponse>of().iterator();
            return;
        }

        Integer totalCount = res.body().totalCount();
        Integer numOfRows = res.body().numOfRows();
        maxPage = computeMaxPage(totalCount, numOfRows);

        List<MolitRentApiItemResponse> items = res.body().items().safeItem();
        Iterator<MolitRentApiItemResponse> it = items.iterator();

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

