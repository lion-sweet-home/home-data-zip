/**
 * 사용자(User) 관련 API
 */
import { get, post, put, patch } from "./api";

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

/**
 * 마이페이지 정보 조회
 * @param {number} userId - 사용자 ID
 * @returns {Promise<{email: string, Nickname: string}>}
 */
export async function getMyPageInfo(userId) {
  return get(`/users/${userId}/my-page`);
}

/**
 * 마이페이지 수정 (닉네임 변경, 비밀번호 확인 필요)
 * @param {number} userId - 사용자 ID
 * @param {string} nickname - 새 닉네임
 * @param {string} password - 현재 비밀번호 (본인 확인용)
 * @returns {Promise<{email: string, Nickname: string}>}
 */
export async function editMyPage(userId, nickname, password) {
  return patch(`/users/${userId}/my-page`, { nickname, password });
}

/**
 * 비밀번호 변경
 * @param {string} currentPassword - 현재 비밀번호
 * @param {string} newPassword - 새 비밀번호
 * @param {string} confirmPassword - 새 비밀번호 확인
 * @returns {Promise<void>}
 */
export async function changePassword(currentPassword, newPassword, confirmPassword) {
  return patch('/users/change-password', {
    currentPassword,
    newPassword,
    confirmPassword,
  });
}

/**
 * 비밀번호 찾기 (인증코드 발송)
 * @param {string} email - 가입된 이메일
 * @returns {Promise<void>}
 */
export async function findPassword(email) {
  return post('/users/find-password', { email });
}

/**
 * 비밀번호 재설정
 * @param {string} email - 이메일
 * @param {string} newPassword - 새 비밀번호
 * @param {string} confirmPassword - 새 비밀번호 확인
 * @returns {Promise<void>}
 */
export async function resetPassword(email, newPassword, confirmPassword) {
  return patch('/users/reset-password', { email, newPassword, confirmPassword });
}

export default {
    getMyProfile,
    getNotificationSetting,
    updateNotificationSetting,
    getMyPageInfo,
    editMyPage,
    changePassword,
    findPassword,
    resetPassword,
};
