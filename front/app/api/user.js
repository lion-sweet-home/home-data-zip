/**
 * 사용자(User) 관련 API
 */
import { get, put } from "./api";

const USER_ENDPOINTS = {
    me: "/users/me",
    notificationSetting: "/users/notification-setting",
};

/**
 * 현재 로그인한 사용자 정보 조회
 * @returns {Promise<object>}
 */
export async function getMyProfile() {
    return get(USER_ENDPOINTS.me);
}

/**
 * 알림 수신 설정 조회
 * @returns {Promise<{notificationEnabled: boolean}>}
 */
export async function getNotificationSetting() {
    return get(USER_ENDPOINTS.notificationSetting);
}

/**
 * 알림 수신 설정 변경
 * @param {boolean} notificationEnabled
 * @returns {Promise<void>}
 */
export async function updateNotificationSetting(notificationEnabled) {
    return put(USER_ENDPOINTS.notificationSetting, {
        notificationEnabled: !!notificationEnabled,
    });
}

export default {
    getMyProfile,
    getNotificationSetting,
    updateNotificationSetting,
};