/**
 * 유저(User) 관련 API
 */

import { get, put } from './api';

/**
 * 알림 수신 설정 조회
 * @returns {Promise<{notificationEnabled: boolean}>}
 */
export async function getNotificationSetting() {
  return get('/users/notification-setting');
}

/**
 * 알림 수신 설정 변경
 * @param {boolean} notificationEnabled
 * @returns {Promise<void>}
 */
export async function updateNotificationSetting(notificationEnabled) {
  return put('/users/notification-setting', { notificationEnabled: !!notificationEnabled });
}

export default {
  getNotificationSetting,
  updateNotificationSetting,
};

