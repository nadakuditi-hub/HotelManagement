// Small display helpers shared by all pages.

export const money = (n) =>
  "₹" + Number(n ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });

export const dateStr = (d) =>
  d ? new Date(d).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "";

export const dateTime = (d) =>
  d ? new Date(d).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }) : "";

// CHECKED_IN -> "CHECKED IN"
export const label = (s) => (s ? String(s).replace(/_/g, " ") : "");

// Empty input -> null, otherwise a number (for optional number fields sent to the API)
export const numOrNull = (v) => (v === "" || v == null ? null : Number(v));

export const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;
export const PASSWORD_HINT = "8 to 64 characters, with at least one letter and one number";
