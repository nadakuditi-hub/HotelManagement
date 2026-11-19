import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import { errMsg } from "../api";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await register(form);
      navigate("/");
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card form narrow" onSubmit={submit}>
      <h2>Create account</h2>
      {error && <div className="alert error">{error}</div>}
      <label>Full name<input required value={form.fullName} onChange={set("fullName")} /></label>
      <label>Email<input type="email" required value={form.email} onChange={set("email")} /></label>
      <label>Phone<input value={form.phone} onChange={set("phone")} /></label>
      <label>Password<input type="password" required minLength={8} maxLength={64} autoComplete="new-password" value={form.password} onChange={set("password")} /></label>
      <span className="muted small-text">8 to 64 characters, with at least one letter and one number.</span>
      <button className="btn" disabled={busy}>{busy ? "Creating..." : "Sign up"}</button>
      <p className="muted">Already registered? <Link to="/login">Login</Link></p>
    </form>
  );
}
