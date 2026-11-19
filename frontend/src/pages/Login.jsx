import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import { errMsg } from "../api";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const u = await login(form.email, form.password);
      navigate(u.role === "ADMIN" ? "/admin" : "/");
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card form narrow" onSubmit={submit}>
      <h2>Login</h2>
      {error && <div className="alert error">{error}</div>}
      <label>Email
        <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </label>
      <label>Password
        <input type="password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
      </label>
      <button className="btn" disabled={busy}>{busy ? "Signing in..." : "Login"}</button>
      <p className="muted">No account? <Link to="/register">Sign up</Link></p>
      <p className="muted small-text">Demo admin: admin@hotel.com / admin123</p>
    </form>
  );
}
