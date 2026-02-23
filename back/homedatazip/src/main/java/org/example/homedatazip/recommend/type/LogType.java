package org.example.homedatazip.recommend.type;

public enum LogType {

    SEARCH(1),   // 지도 검색
    SUMMARY(3),  // 아파트 요약 정보 클릭
    DETAIL(5),
    PYEONG_CLICK(10); // 아파트 상세 정보 조회

    private final int score;
    LogType(int score) { this.score = score; }
    public int getScore() { return score; }
}
