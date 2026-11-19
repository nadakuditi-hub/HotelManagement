import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";

export default function ManageBookings() {
  const [bookings, setBookings] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [error, setError] = useState("");

  const load = async () => {
    try { setBookings((await api.get("/admin/bookings")).data); } catch (err) { setError(errMsg(err)); }
  };
  useEffect(() => { load(); }, []);

  const move = async (id, status) => {
    setError("");
    try { await api.put(`/admin/bookings/${id}/status`, { status }); load(); } catch (err) { setError(errMsg(err)); }
  };

  const cancel = async (id) => {
    if (!confirm("Cancel this booking?")) return;
    try { await api.put(`/bookings/${id}/cancel`); load(); } catch (err) { setError(errMsg(err)); }
  };

  const shown = filter === "ALL" ? bookings : bookings.filter((b) => b.status === filter);

  return (
    <>
      <h1>All bookings</h1>
      {error && <div className="alert error">{error}</div>}
      <div className="row" style={{ marginBottom: 12 }}>
        {["ALL", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"].map((s) => (
          <button key={s} className={`btn small ${filter === s ? "" : "ghost"}`} onClick={() => setFilter(s)}>
            {s.replace("_", " ")}
          </button>
        ))}
      </div>
      <div className="table-wrap card">
        <table>
          <thead><tr><th>#</th><th>Guest</th><th>Room</th><th>Dates</th><th>Total</th><th>Paid</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {shown.map((b) => (
              <tr key={b.id}>
                <td>{b.id}</td>
                <td>{b.customerName}<div className="muted small-text">{b.customerEmail}</div></td>
                <td>{b.roomNumber}</td>
                <td>{b.checkIn} → {b.checkOut}</td>
                <td>₹{Number(b.totalAmount).toLocaleString("en-IN")}</td>
                <td>{b.paid ? "✅" : "—"}</td>
                <td><span className={`badge ${b.status.toLowerCase()}`}>{b.status.replace("_", " ")}</span></td>
                <td className="row">
                  {b.status === "CONFIRMED" && <button className="btn small" onClick={() => move(b.id, "CHECKED_IN")}>Check in</button>}
                  {b.status === "CHECKED_IN" && <button className="btn small" onClick={() => move(b.id, "CHECKED_OUT")}>Check out</button>}
                  {b.status === "CONFIRMED" && <button className="btn small danger" onClick={() => cancel(b.id)}>Cancel</button>}
                </td>
              </tr>
            ))}
            {shown.length === 0 && <tr><td colSpan="8" className="muted">No bookings.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
