import { useState } from "react";
import api, { errMsg } from "../api";
import { useAuth } from "../AuthContext.jsx";
import { PASSWORD_RULE, PASSWORD_HINT } from "../format";

export default function Account() {
  const { user } = useAuth();
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setOk("");
    if (!PASSWORD_RULE.test(form.newPassword)) return setError("New password: " + PASSWORD_HINT);
    if (form.newPassword !== form.confirm) return setError("The two new passwords do not match.");
    setBusy(true);
    try {
      await api.put("/account/password", { currentPassword: form.currentPassword, newPassword: form.newPassword });
      setOk("Password changed successfully.");
      setForm({ currentPassword: "", newPassword: "", confirm: "" });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h1>My account</h1>
      <div className="card profile">
        <span className="avatar big">{user.fullName.trim()[0]?.toUpperCase()}</span>
        <div>
          <b>{user.fullName}</b>
          <div className="muted">{user.email}</div>
          <span className={`badge ${user.role === "ADMIN" ? "deluxe" : "single"}`}>{user.role}</span>
        </div>
      </div>

      <form className="card form narrow left" onSubmit={submit}>
        <h3>Change password</h3>
        {error && <div className="alert error">{error}</div>}
        {ok && <div className="alert success">{ok}</div>}
        <label>Current password<input type="password" required autoComplete="current-password" value={form.currentPassword} onChange={set("currentPassword")} /></label>
        <label>New password<input type="password" required autoComplete="new-password" value={form.newPassword} onChange={set("newPassword")} /></label>
        <span className="muted small-text">{PASSWORD_HINT}</span>
        <label>Confirm new password<input type="password" required autoComplete="new-password" value={form.confirm} onChange={set("confirm")} /></label>
        <button className="btn" disabled={busy}>{busy ? "Saving..." : "Change password"}</button>
      </form>
    </>
  );
}
