import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";
import { label } from "../../format";

const STATE_LABEL = { CLEAN: "Clean", DIRTY: "Needs cleaning", MAINTENANCE: "Under maintenance" };

export default function Housekeeping() {
  const [rooms, setRooms] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [staff, setStaff] = useState([]);
  const [filter, setFilter] = useState("OPEN");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = async () => {
    try {
      const [r, t, s] = await Promise.all([
        api.get("/admin/housekeeping/rooms"),
        api.get("/admin/housekeeping/tasks"),
        api.get("/admin/staff"),
      ]);
      setRooms(r.data);
      setTasks(t.data);
      setStaff(s.data.filter((x) => x.active));
    } catch (e) {
      setError(errMsg(e));
    }
  };
  useEffect(() => { load(); }, []);

  const run = async (fn, msg) => {
    setError("");
    setOk("");
    try {
      await fn();
      setOk(msg);
      load();
    } catch (e) {
      setError(errMsg(e));
    }
  };

  const createTask = (roomId, type) =>
    run(() => api.post("/admin/housekeeping/tasks", { roomId, type }), `${label(type)} task created.`);
  const updateTask = (id, body, msg) => run(() => api.put(`/admin/housekeeping/tasks/${id}`, body), msg);

  const shown = tasks.filter((t) =>
    filter === "ALL" ? true : filter === "OPEN" ? t.status !== "DONE" : t.status === "DONE"
  );

  return (
    <>
      <h1>Housekeeping</h1>
      <p className="muted">A room can only be checked in when it is clean and has no open maintenance task. Checking out creates a cleaning task automatically.</p>
      {error && <div className="alert error">{error}</div>}
      {ok && <div className="alert success">{ok}</div>}

      <h2>Rooms</h2>
      <div className="table-wrap card">
        <table>
          <thead><tr><th>Room</th><th>Type</th><th>State</th><th>Open tasks</th><th>Actions</th></tr></thead>
          <tbody>
            {rooms.map((r) => (
              <tr key={r.roomId}>
                <td><b>{r.roomNumber}</b></td>
                <td>{r.roomType}</td>
                <td><span className={`badge ${r.state.toLowerCase()}`}>{STATE_LABEL[r.state] || r.state}</span></td>
                <td>{r.openTasks}</td>
                <td className="row">
                  <button className="btn small ghost" onClick={() => createTask(r.roomId, "CLEANING")}>+ Cleaning</button>
                  <button className="btn small ghost" onClick={() => createTask(r.roomId, "MAINTENANCE")}>+ Maintenance</button>
                </td>
              </tr>
            ))}
            {rooms.length === 0 && <tr><td colSpan="5" className="muted">No rooms yet.</td></tr>}
          </tbody>
        </table>
      </div>

      <h2 style={{ marginTop: 24 }}>Tasks</h2>
      <div className="chips">
        {["OPEN", "DONE", "ALL"].map((f) => (
          <button key={f} className={`chip ${filter === f ? "on" : ""}`} onClick={() => setFilter(f)}>{label(f)}</button>
        ))}
      </div>
      <div className="table-wrap card">
        <table>
          <thead><tr><th>Room</th><th>Task</th><th>Notes</th><th>Assigned to</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {shown.map((t) => (
              <tr key={t.id}>
                <td>{t.roomNumber}</td>
                <td>{label(t.type)}</td>
                <td className="muted small-text">{t.notes || "—"}</td>
                <td>
                  <select
                    value={t.staffId ?? ""}
                    disabled={t.status === "DONE"}
                    onChange={(e) =>
                      updateTask(
                        t.id,
                        e.target.value ? { staffId: Number(e.target.value) } : { unassign: true },
                        "Staff updated."
                      )
                    }
                  >
                    <option value="">Unassigned</option>
                    {staff.map((s) => (<option key={s.id} value={s.id}>{s.fullName} ({label(s.role)})</option>))}
                  </select>
                </td>
                <td><span className={`badge ${t.status.toLowerCase()}`}>{label(t.status)}</span></td>
                <td className="row">
                  {t.status === "OPEN" && (
                    <button className="btn small" onClick={() => updateTask(t.id, { status: "IN_PROGRESS" }, "Task started.")}>Start</button>
                  )}
                  {t.status !== "DONE" && (
                    <button className="btn small" onClick={() => updateTask(t.id, { status: "DONE" }, "Task marked done.")}>Done</button>
                  )}
                </td>
              </tr>
            ))}
            {shown.length === 0 && <tr><td colSpan="6" className="muted">No tasks here.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
