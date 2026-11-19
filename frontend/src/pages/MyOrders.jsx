import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api";
import { money, dateTime, label } from "../format";

export default function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    try { setOrders((await api.get("/orders/my")).data); }
    catch (e) { setError(errMsg(e)); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const cancel = async (id) => {
    if (!confirm("Cancel this order?")) return;
    setError("");
    try { await api.put(`/orders/${id}/cancel`); load(); } catch (e) { setError(errMsg(e)); }
  };

  if (loading) return <p>Loading...</p>;

  return (
    <>
      <h1>My food orders</h1>
      {error && <div className="alert error">{error}</div>}
      {orders.length === 0 ? (
        <div className="empty">
          <div className="empty-icon">🍽️</div>
          <p>No orders yet. You can order once you are checked in.</p>
          <Link className="btn ghost" to="/menu">See the menu</Link>
        </div>
      ) : (
        <div className="stack">
          {orders.map((o) => (
            <div className="card" key={o.id}>
              <div className="room-top">
                <b>Order #{o.id} · Room {o.roomNumber}</b>
                <span className={`badge ${o.status.toLowerCase()}`}>{label(o.status)}</span>
              </div>
              <p className="muted small-text">{dateTime(o.createdAt)}</p>
              <ul className="order-items">
                {o.items.map((l) => (<li key={l.name}><span>{l.quantity} × {l.name}</span><span>{money(l.price * l.quantity)}</span></li>))}
              </ul>
              {o.notes && <p className="muted small-text">Note: {o.notes}</p>}
              <div className="room-top">
                <b>{money(o.totalAmount)} {o.status !== "CANCELLED" && <span className={`badge ${o.paid ? "paid" : "unpaid"}`}>{o.paid ? "PAID" : "TO PAY AT CHECK-OUT"}</span>}</b>
                {o.status === "PLACED" && <button className="btn small danger" onClick={() => cancel(o.id)}>Cancel order</button>}
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
