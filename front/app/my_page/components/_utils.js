export function decodeAccessTokenPayload() {
  if (typeof window === 'undefined') return null;
  const token = localStorage.getItem('accessToken');
  if (!token) return null;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload ?? null;
  } catch {
    return null;
  }
}

export function normalizeRoles(roles) {
  if (!roles) return [];
  if (Array.isArray(roles)) {
    if (roles.length > 0 && typeof roles[0] === 'string') return roles;
    if (roles.length > 0 && roles[0]?.roleType) return roles.map((r) => r.roleType);
    if (roles.length > 0 && roles[0]?.role?.roleType) return roles.map((r) => r.role.roleType);
  }
  return [];
}

export function formatDate(dateLike) {
  if (!dateLike) return '-';
  const d = new Date(dateLike);
  if (Number.isNaN(d.getTime())) return '-';
  return d.toISOString().slice(0, 10);
}

export function getLocalJSON(key, fallback) {
  if (typeof window === 'undefined') return fallback;
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function setLocalJSON(key, value) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(key, JSON.stringify(value));
}

