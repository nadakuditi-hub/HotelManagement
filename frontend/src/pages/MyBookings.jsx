import { useEffect, useState } from "react";
import api, { errMsg } from "../api";
import { StarInput } from "../components/Stars.jsx";
import { money } from "../format";

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [payFor, setPayFor] = useState(null); // booking being paid
  const [method, setMethod] = useState("UPI");
  const [invoice, setInvoice] = useState(null);
  const [reviewFor, setReviewFor] = useState(null); // booking being reviewed
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");

  const load = async () => {
    try {
      setBookings((await api.get("/bookings/my")).data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const cancel = async (id) => {
    if (!confirm("Cancel this booking? Any payment will be refunded.")) return;
    setError("");
    try { await api.put(`/bookings/${id}/cancel`); load(); } catch (err) { setError(errMsg(err)); }
  };

  const pay = async () => {
    setError("");
    try {
      const res = await api.post("/payments", { bookingId: payFor.id, method });
      setInvoice(res.data);
      setPayFor(null);
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const viewInvoice = async (id) => {
    try { setInvoice((await api.get(`/payments/booking/${id}`)).data); } catch (err) { setError(errMsg(err)); }
  };

  const openReview = (b) => { setReviewFor(b); setRating(5); setComment(""); setError(""); };

  const submitReview = async () => {
    setError("");
    try {
      await api.post("/reviews", { bookingId: reviewFor.id, rating, comment });
      setReviewFor(null);
      setNotice("Thank you! Your review has been posted.");
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  if (loading) return <p>Loading...</p>;

  return (
    <>
      <h1>My bookings</h1>
      {notice && <div className="alert success">{notice}</div>}
      {error && <div className="alert error">{error}</div>}
      {bookings.length === 0 ? <p className="muted">You have no bookings yet.</p> : (
        <div className="table-wrap card">
          <table>
            <thead><tr><th>#</th><th>Room</th><th>Dates</th><th>Guests</th><th>Total</th><th>Status</th><th>Payment</th><th></th></tr></thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td>{b.roomNumber} <span className="muted">({b.roomType})</span></td>
                  <td>{b.checkIn} → {b.checkOut} <span className="muted">({b.nights}n)</span></td>
                  <td>{b.guests}</td>
                  <td>
                    {money(b.totalAmount)}
                    {(b.addOns?.length > 0 || Number(b.discountAmount) > 0) && (
                      <div className="muted small-text">
                        {b.addOns?.length > 0 && <>+ {b.addOns.map((a) => a.name).join(", ")} </>}
                        {Number(b.discountAmount) > 0 && <>· coupon {b.couponCode} (−{money(b.discountAmount)})</>}
                      </div>
                    )}
                  </td>
                  <td><span className={`badge ${b.status.toLowerCase()}`}>{b.status.replace("_", " ")}</span></td>
                  <td>
                    {b.status === "CANCELLED" ? <span className="muted">—</span>
                      : b.paid ? <button className="btn small ghost" onClick={() => viewInvoice(b.id)}>Invoice</button>
                      : <button className="btn small" onClick={() => setPayFor(b)}>Pay now</button>}
                  </td>
                  <td className="row">
                    {b.status === "CONFIRMED" && <button className="btn small danger" onClick={() => cancel(b.id)}>Cancel</button>}
                    {b.status === "CHECKED_OUT" && (b.reviewed
                      ? <span className="muted small-text">★ Reviewed</span>
                      : <button className="btn small" onClick={() => openReview(b)}>Rate stay</button>)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {payFor && (
        <div className="modal-bg" onClick={() => setPayFor(null)}>
          <div className="card modal" onClick={(e) => e.stopPropagation()}>
            <h3>Pay for booking #{payFor.id}</h3>
            <p>Room {payFor.roomNumber}, {payFor.nights} night(s)</p>
            <p className="price">₹{Number(payFor.totalAmount).toLocaleString("en-IN")}</p>
            <label>Payment method
              <select value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="UPI">UPI</option><option value="CARD">Card</option><option value="CASH">Cash at hotel</option>
              </select>
            </label>
            <p className="muted small-text">Demo only: no real money is charged.</p>
            <div className="row">
              <button className="btn" onClick={pay}>Pay</button>
              <button className="btn ghost" onClick={() => setPayFor(null)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {reviewFor && (
        <div className="modal-bg" onClick={() => setReviewFor(null)}>
          <div className="card modal" onClick={(e) => e.stopPropagation()}>
            <h3>Rate your stay in room {reviewFor.roomNumber}</h3>
            {error && <div className="alert error">{error}</div>}
            <StarInput value={rating} onChange={setRating} />
            <label>Comment (optional)
              <textarea rows="4" maxLength={500} placeholder="What did you like? What could be better?"
                value={comment} onChange={(e) => setComment(e.target.value)} />
            </label>
            <div className="row">
              <button className="btn" onClick={submitReview}>Submit review</button>
              <button className="btn ghost" onClick={() => setReviewFor(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {invoice && (
        <div className="modal-bg" onClick={() => setInvoice(null)}>
          <div className="card modal invoice" onClick={(e) => e.stopPropagation()}>
            <h3>🧾 Invoice {invoice.invoiceNumber}</h3>
            <table className="kv">
              <tbody>
                <tr><td>Guest</td><td>{invoice.customerName}</td></tr>
                <tr><td>Room</td><td>{invoice.roomNumber}</td></tr>
                <tr><td>Stay</td><td>{invoice.checkIn} → {invoice.checkOut}</td></tr>
                <tr><td>Method</td><td>{invoice.method}</td></tr>
                <tr><td>Status</td><td>{invoice.status}</td></tr>
                <tr><td>Paid on</td><td>{new Date(invoice.paidAt).toLocaleString()}</td></tr>
                <tr><td><b>Amount</b></td><td><b>₹{Number(invoice.amount).toLocaleString("en-IN")}</b></td></tr>
              </tbody>
            </table>
            <div className="row">
              <button className="btn" onClick={() => window.print()}>Print</button>
              <button className="btn ghost" onClick={() => setInvoice(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
