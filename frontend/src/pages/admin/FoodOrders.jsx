import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";
import { money, dateTime, label } from "../../format";

const FILTERS = ["ALL", "PLACED", "PREPARING", "DELIVERED", "CANCELLED"];

export default function FoodOrders() {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [error, setError] = useState("");

  const load = async () => {
    try { setOrders((await api.get("/admin/orders")).data); } catch (e) { setError(errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  const act = async (fn) => {
    setError("");
    try { await fn(); load(); } catch (e) { setError(errMsg(e)); }
  };
  const setStatus = (id, status) => act(() => api.put(`/admin/orders/${id}/status`, { status }));
  const markPaid = (id) => act(() => api.put(`/admin/orders/${id}/paid`));

  const shown = filter === "ALL" ? orders : orders.filter((o) => o.status === filter);

  return (
    <>
      <h1>Room-service orders</h1>
      <p className="muted">Orders are added to the room bill. Guests can't check out while an order is unpaid.</p>
      {error && <div className="alert error">{error}</div>}
      <div className="chips">
        {FILTERS.map((f) => (
          <button key={f} className={`chip ${filter === f ? "on" : ""}`} onClick={() => setFilter(f)}>{label(f)}</button>
        ))}
      </div>

      {shown.length === 0 ? <p className="muted">No orders here.</p> : (
        <div className="stack">
          {shown.map((o) => (
            <div className="card" key={o.id}>
              <div className="room-top">
                <b>#{o.id} · Room {o.roomNumber} · {o.customerName}</b>
                <span className={`badge ${o.status.toLowerCase()}`}>{label(o.status)}</span>
              </div>
              <p className="muted small-text">{dateTime(o.createdAt)}</p>
              <ul className="order-items">
                {o.items.map((l) => (<li key={l.name}><span>{l.quantity} × {l.name}</span><span>{money(l.price * l.quantity)}</span></li>))}
              </ul>
              {o.notes && <p className="small-text">Note: {o.notes}</p>}
              <div className="room-top">
                <b>{money(o.totalAmount)} <span className={`badge ${o.paid ? "paid" : "unpaid"}`}>{o.paid ? "PAID" : "UNPAID"}</span></b>
                <span className="row">
                  {o.status === "PLACED" && <button className="btn small" onClick={() => setStatus(o.id, "PREPARING")}>Start preparing</button>}
                  {o.status === "PREPARING" && <button className="btn small" onClick={() => setStatus(o.id, "DELIVERED")}>Mark delivered</button>}
                  {(o.status === "PLACED" || o.status === "PREPARING") && (
                    <button className="btn small danger" onClick={() => setStatus(o.id, "CANCELLED")}>Cancel</button>
                  )}
                  {o.status !== "CANCELLED" && !o.paid && (
                    <button className="btn small ghost" onClick={() => markPaid(o.id)}>Mark paid</button>
                  )}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
