/**
 * 관리자(Admin) 관련 API
 * - 대시보드 통계(수입/회원수/구독자/매물수)
 * - 회원 목록
 * - 공지사항 CRUD
 *
 * 백엔드 기준 prefix: /api/admin/**
 */

import { get, post, put, del, request } from "./api";

// -----------------------------
// Dashboard
// -----------------------------

export async function getMonthlyIncome() {
  return get("/admin/monthly-income");
}

export async function getYearlyIncome(year) {
  const params = new URLSearchParams();
  if (year) params.append("year", String(year));
  const qs = params.toString();
  return get(`/admin/yearly-income${qs ? `?${qs}` : ""}`);
}

export async function getUsersCount() {
  return get("/admin/users/count");
}

export async function getSubscribersCount() {
  return get("/admin/subscribers/count");
}

export async function getListingsCount() {
  return get("/admin/listings/count");
}

export async function getDashboardStats(year) {
  const [monthlyIncome, usersCount, subscribersCount, listingsCount, yearlyIncome] =
    await Promise.all([
      getMonthlyIncome(),
      getUsersCount(),
      getSubscribersCount(),
      getListingsCount(),
      year ? getYearlyIncome(year) : Promise.resolve(null),
    ]);

  return {
    monthlyIncome,
    usersCount,
    subscribersCount,
    listingsCount,
    yearlyIncome,
  };
}

// -----------------------------
// Users
// -----------------------------

export async function getAdminUsersList({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  params.append("page", String(page));
  params.append("size", String(size));
  return get(`/admin/users/list?${params.toString()}`);
}

/**
 * 관리자 회원 검색 (type: ALL | NICKNAME | EMAIL, keyword 필수)
 */
export async function searchAdminUsers({ type = "ALL", keyword, page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  params.append("page", String(page));
  params.append("size", String(size));
  const url = `/admin/search?${params.toString()}`;
  return request(url, { method: "GET", body: { type, keyword: keyword ?? "" } });
}

export async function deleteAdminUser(userId) {
  if (userId === undefined || userId === null) {
    throw new Error("userId가 필요합니다.");
  }
  return del(`/admin/${userId}`);
}

// -----------------------------
// Notifications (공지사항)
// -----------------------------

export async function getAdminNotifications() {
  return get("/admin/notifications");
}

export async function createAdminNotification({ title, message }) {
  return post("/admin/notifications", { title, message });
}

export async function updateAdminNotification(notificationId, { title, message }) {
  if (!notificationId) throw new Error("notificationId가 필요합니다.");
  return put(`/admin/notifications/${notificationId}`, { title, message });
}

export async function deleteAdminNotification(notificationId) {
  if (!notificationId) throw new Error("notificationId가 필요합니다.");
  return del(`/admin/notifications/${notificationId}`);
}

export default {
  getMonthlyIncome,
  getYearlyIncome,
  getUsersCount,
  getSubscribersCount,
  getListingsCount,
  getDashboardStats,
  getAdminUsersList,
  searchAdminUsers,
  deleteAdminUser,
  getAdminNotifications,
  createAdminNotification,
  updateAdminNotification,
  deleteAdminNotification,
};
