import { useEffect, useState } from "react";
import api, { errMsg } from "../api";
import { money, dateStr } from "../format";

// Booking window: choose extras, apply a coupon, see the live price, then confirm.
export default function BookingModal({ room, filters, onClose, onBooked }) {
  const [addOns, setAddOns] = useState([]);
  const [selected, setSelected] = useState([]); // ids of chosen extras
  const [couponInput, setCouponInput] = useState("");
  const [coupon, setCoupon] = useState(""); // code the guest asked to apply
  const [quote, setQuote] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const base = {
    roomId: room.id,
    checkIn: filters.checkIn,
    checkOut: filters.checkOut,
    guests: Number(filters.guests),
  };

  useEffect(() => {
    api.get("/addons").then((r) => setAddOns(r.data)).catch(() => {});
  }, []);

  // Ask the server for the price whenever extras or the coupon change.
  useEffect(() => {
    let cancelled = false;
    api
      .post("/bookings/quote", { ...base, addOnIds: selected, couponCode: coupon || null })
      .then((r) => { if (!cancelled) { setQuote(r.data); setError(""); } })
      .catch((e) => { if (!cancelled) setError(errMsg(e)); });
    return () => { cancelled = true; };
  }, [selected, coupon]); // eslint-disable-line

  const toggle = (id) => setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));

  const applyCoupon = (e) => {
    e.preventDefault();
    setCoupon(couponInput.trim().toUpperCase());
  };
  const removeCoupon = () => { setCoupon(""); setCouponInput(""); };

  const confirm = async () => {
    setBusy(true);
    setError("");
    try {
      // Use the code the server accepted, so a rejected coupon is never sent.
      await api.post("/bookings", { ...base, addOnIds: selected, couponCode: quote?.couponCode || null });
      onBooked(room);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="card modal wide" onClick={(e) => e.stopPropagation()}>
        <div className="room-top">
          <h3>Book room {room.roomNumber}</h3>
          <button className="btn small ghost" onClick={onClose}>Close</button>
        </div>
        <p className="muted">
          {dateStr(filters.checkIn)} → {dateStr(filters.checkOut)} · {filters.guests} guest(s)
        </p>

        {addOns.length > 0 && (
          <>
            <h4 className="section-title">Add extras</h4>
            <div className="extras">
              {addOns.map((a) => (
                <label key={a.id} className={`extra ${selected.includes(a.id) ? "on" : ""}`}>
                  <input type="checkbox" checked={selected.includes(a.id)} onChange={() => toggle(a.id)} />
                  <span className="extra-text">
                    <b>{a.name}</b>
                    <span className="muted small-text">{a.description}</span>
                  </span>
                  <span className="extra-price">{money(a.price)}<small>{a.perNight ? " / night" : " / stay"}</small></span>
                </label>
              ))}
            </div>
          </>
        )}

        <h4 className="section-title">Coupon</h4>
        {quote?.couponCode ? (
          <div className="alert success row between">
            <span>🏷 <b>{quote.couponCode}</b> applied: you save {money(quote.discount)}</span>
            <button className="btn small ghost" onClick={removeCoupon}>Remove</button>
          </div>
        ) : (
          <form className="row" onSubmit={applyCoupon}>
            <input placeholder="Enter coupon code" value={couponInput} maxLength={30}
              onChange={(e) => setCouponInput(e.target.value)} style={{ flex: 1 }} />
            <button className="btn ghost" disabled={!couponInput.trim()}>Apply</button>
          </form>
        )}
        {quote?.couponError && <div className="alert error">{quote.couponError}</div>}

        <h4 className="section-title">Price</h4>
        {quote ? (
          <table className="kv summary">
            <tbody>
              <tr><td>Room ({quote.nights} night{quote.nights > 1 ? "s" : ""} × {money(room.pricePerNight)})</td><td>{money(quote.roomAmount)}</td></tr>
              {quote.addOns.map((a) => (<tr key={a.name}><td>{a.name}</td><td>{money(a.amount)}</td></tr>))}
              {Number(quote.discount) > 0 && (<tr className="discount"><td>Discount ({quote.couponCode})</td><td>− {money(quote.discount)}</td></tr>)}
              <tr className="total"><td>Total</td><td>{money(quote.total)}</td></tr>
            </tbody>
          </table>
        ) : !error ? <p className="muted">Calculating price...</p> : null}

        {error && <div className="alert error">{error}</div>}
        <div className="row">
          <button className="btn" onClick={confirm} disabled={busy || !quote}>{busy ? "Booking..." : "Confirm booking"}</button>
          <button className="btn ghost" onClick={onClose}>Cancel</button>
        </div>
        <p className="muted small-text">You can pay from “My bookings” after booking. Free cancellation until check-in.</p>
      </div>
    </div>
  );
}
