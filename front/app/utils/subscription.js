export const DEFAULT_SUBSCRIPTION_PRICE = 9900;
export const DEFAULT_SUBSCRIPTION_PLAN_NAME = "프리미엄";

function normalizeBoolean(value) {
  if (typeof value === "boolean") return value;
  if (typeof value === "number") return value === 1;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (["true", "y", "yes", "1", "on", "active"].includes(normalized)) return true;
    if (["false", "n", "no", "0", "off", "inactive"].includes(normalized))
      return false;
  }
  return null;
}

function pickFirstValue(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null && value !== "") return value;
  }
  return null;
}

function resolvePlanName(raw) {
  if (!raw) return null;
  const name = String(raw).trim();
  if (!name) return null;
  const normalized = name.toLowerCase();
  if (["-", "none", "미구독", "없음"].includes(normalized)) return null;
  return name;
}

function resolvePrice(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value.replace(/[,원]/g, ""));
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
}

export function getSubscriptionMeta(subscription) {
  if (!subscription) {
    return {
      status: null,
      isActive: false,
      planName: null,
      price: null,
      hasBillingKey: false,
    };
  }

  const status = pickFirstValue(
    subscription.status,
    subscription.subscriptionStatus,
    subscription.subscription_status,
    subscription.state,
    subscription.subscriptionState
  );

  const isActive =
    normalizeBoolean(subscription.isActive) ??
    normalizeBoolean(subscription.active) ??
    normalizeBoolean(subscription.autoPayActive) ??
    normalizeBoolean(subscription.auto_pay_active) ??
    normalizeBoolean(subscription.autoPay) ??
    normalizeBoolean(subscription.auto_pay) ??
    normalizeBoolean(subscription.autoPayEnabled) ??
    normalizeBoolean(subscription.autopayEnabled) ??
    normalizeBoolean(subscription.subscribed) ??
    normalizeBoolean(subscription.isSubscribed) ??
    false;

  const planName = resolvePlanName(
    pickFirstValue(
      subscription.planName,
      subscription.plan?.name,
      subscription.plan?.title,
      subscription.plan,
      subscription.subscriptionPlan
    )
  );

  const price = resolvePrice(
    pickFirstValue(
      subscription.price,
      subscription.plan?.price,
      subscription.plan?.amount,
      subscription.amount,
      subscription.planAmount
    )
  );

  const hasBillingKey =
    subscription?.hasBillingKey === true ||
    Boolean(subscription?.billingKey || subscription?.billing_key);

  return {
    status,
    isActive: status === "ACTIVE" || isActive,
    planName,
    price,
    hasBillingKey,
  };
}
