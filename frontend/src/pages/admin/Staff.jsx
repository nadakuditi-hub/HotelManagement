import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";
import { label } from "../../format";

const ROLES = ["RECEPTIONIST", "HOUSEKEEPING", "MAINTENANCE", "CHEF", "SECURITY", "MANAGER"];
const empty = { fullName: "", role: "RECEPTIONIST", phone: "", email: "", active: true };

export default function Staff() {
  const [staff, setStaff] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const load = async () => {
    try { setStaff((await api.get("/admin/staff")).data); } catch (e) { setError(errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  const body = (f) => ({
    fullName: f.fullName.trim(),
    role: f.role,
    phone: f.phone.trim() || null,
    email: f.email.trim() || null,
    active: f.active,
  });

  const save = async (e) => {
    e.preventDefault();
    setError("");
    setOk("");
    try {
      if (editId) await api.put(`/admin/staff/${editId}`, body(form));
      else await api.post("/admin/staff", body(form));
      setOk(editId ? "Staff member updated." : "Staff member added.");
      setForm(empty);
      setEditId(null);
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const edit = (s) => {
    setEditId(s.id);
    setForm({ fullName: s.fullName, role: s.role, phone: s.phone || "", email: s.email || "", active: s.active });
  };

  const toggle = async (s) => {
    setError("");
    try {
      await api.put(`/admin/staff/${s.id}`, { fullName: s.fullName, role: s.role, phone: s.phone, email: s.email, active: !s.active });
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  return (
    <>
      <h1>Staff</h1>
      {error && <div className="alert error">{error}</div>}
      {ok && <div className="alert success">{ok}</div>}
      <div className="two-col">
        <form className="card form" onSubmit={save}>
          <h3>{editId ? "Edit staff member" : "Add staff member"}</h3>
          <label>Full name<input required maxLength={100} value={form.fullName} onChange={set("fullName")} /></label>
          <label>Role
            <select value={form.role} onChange={set("role")}>
              {ROLES.map((r) => (<option key={r} value={r}>{label(r)}</option>))}
            </select>
          </label>
          <label>Phone<input value={form.phone} onChange={set("phone")} placeholder="+91 98765 43210" /></label>
          <label>Email<input type="email" maxLength={120} value={form.email} onChange={set("email")} /></label>
          <label className="check-inline"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>
          <div className="row">
            <button className="btn">{editId ? "Save changes" : "Add staff"}</button>
            {editId && <button type="button" className="btn ghost" onClick={() => { setEditId(null); setForm(empty); }}>Cancel</button>}
          </div>
        </form>

        <div className="table-wrap card">
          <table>
            <thead><tr><th>Name</th><th>Role</th><th>Contact</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {staff.map((s) => (
                <tr key={s.id}>
                  <td><b>{s.fullName}</b></td>
                  <td>{label(s.role)}</td>
                  <td className="small-text">{s.phone || "—"}<div className="muted">{s.email || ""}</div></td>
                  <td><span className={`badge ${s.active ? "done" : "inactive"}`}>{s.active ? "Active" : "Inactive"}</span></td>
                  <td className="row">
                    <button className="btn small ghost" onClick={() => edit(s)}>Edit</button>
                    <button className="btn small ghost" onClick={() => toggle(s)}>{s.active ? "Deactivate" : "Activate"}</button>
                  </td>
                </tr>
              ))}
              {staff.length === 0 && <tr><td colSpan="5" className="muted">No staff yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
