import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { errMsg } from "../api";
import { useAuth } from "../AuthContext.jsx";
import RoomImage from "../components/RoomImage.jsx";
import { Rating } from "../components/Stars.jsx";
import ReviewsModal from "../components/ReviewsModal.jsx";
import BookingModal from "../components/BookingModal.jsx";
import { money, dateStr } from "../format";

const today = () => new Date().toISOString().slice(0, 10);
const addDays = (d, n) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x.toISOString().slice(0, 10);
};

const FEATURES = [
  ["📶", "Free Wi-Fi"],
  ["🍽️", "24 x 7 room service"],
  ["🛡️", "Secure booking"],
  ["↩️", "Free cancellation until check-in"],
];

export default function Rooms() {
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({
    checkIn: today(), checkOut: addDays(today(), 1), type: "", guests: 1, minPrice: "", maxPrice: "", sort: "price_asc",
  });
  const [rooms, setRooms] = useState([]);
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [booking, setBooking] = useState(null); // room being booked
  const [reviewsFor, setReviewsFor] = useState(null);
  const [msg, setMsg] = useState("");

  const set = (k) => (e) => setFilters({ ...filters, [k]: e.target.value });
  const nights = Math.max(0, Math.round((new Date(filters.checkOut) - new Date(filters.checkIn)) / 86400000));

  const search = async (e) => {
    e?.preventDefault();
    setError("");
    setLoading(true);
    try {
      const params = { checkIn: filters.checkIn, checkOut: filters.checkOut, guests: filters.guests, sort: filters.sort };
      if (filters.type) params.type = filters.type;
      if (filters.minPrice !== "") params.minPrice = filters.minPrice;
      if (filters.maxPrice !== "") params.maxPrice = filters.maxPrice;
      setRooms((await api.get("/rooms", { params })).data);
    } catch (err) {
      setRooms([]);
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    search();
    api.get("/offers").then((r) => setOffers(r.data)).catch(() => {});
  }, []); // eslint-disable-line

  const resetFilters = () => setFilters({ ...filters, type: "", guests: 1, minPrice: "", maxPrice: "", sort: "price_asc" });

  const copyCode = async (code) => {
    try {
      await navigator.clipboard.writeText(code);
      setMsg(`Coupon ${code} copied. Enter it in the booking window to get the discount.`);
    } catch {
      setMsg(`Use coupon code ${code} in the booking window to get the discount.`);
    }
  };

  const startBooking = (room) => {
    if (!user) return navigate("/login");
    setMsg("");
    setBooking(room);
  };

  const booked = (room) => {
    setBooking(null);
    setMsg(`Room ${room.roomNumber} booked! Open “My bookings” to pay.`);
    search();
  };

  return (
    <>
      <section className="hero">
        <h1>Stay in comfort at Grand Stay</h1>
        <p>Pick your dates, compare rooms, add extras and book in a minute.</p>
        <div className="features">
          {FEATURES.map(([icon, text]) => (<span key={text} className="feature">{icon} {text}</span>))}
        </div>
      </section>

      {offers.length > 0 && (
        <section className="offers">
          <h3 className="section-title">Offers for you</h3>
          <div className="offer-list">
            {offers.map((o) => (
              <button key={o.code} className="offer" onClick={() => copyCode(o.code)} title="Click to copy the code">
                <span className="offer-code">{o.code}</span>
                <span className="offer-text">
                  {o.description || (o.type === "PERCENT" ? `${o.value}% off` : `${money(o.value)} off`)}
                </span>
                {o.validTo && <span className="muted small-text">Valid till {dateStr(o.validTo)}</span>}
              </button>
            ))}
          </div>
        </section>
      )}

      <form className="card filters" onSubmit={search}>
        <label>Check-in<input type="date" min={today()} value={filters.checkIn} onChange={set("checkIn")} /></label>
        <label>Check-out<input type="date" min={addDays(filters.checkIn, 1)} value={filters.checkOut} onChange={set("checkOut")} /></label>
        <label>Guests<input type="number" min="1" max="6" value={filters.guests} onChange={set("guests")} /></label>
        <label>Type
          <select value={filters.type} onChange={set("type")}>
            <option value="">Any</option>
            <option>SINGLE</option><option>DOUBLE</option><option>DELUXE</option><option>SUITE</option>
          </select>
        </label>
        <label>Min price (₹)<input type="number" min="0" placeholder="0" value={filters.minPrice} onChange={set("minPrice")} /></label>
        <label>Max price (₹)<input type="number" min="0" placeholder="Any" value={filters.maxPrice} onChange={set("maxPrice")} /></label>
        <label>Sort by
          <select value={filters.sort} onChange={set("sort")}>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
            <option value="rating">Top rated</option>
          </select>
        </label>
        <button className="btn">Search</button>
        <button type="button" className="btn ghost" onClick={resetFilters}>Reset</button>
      </form>

      {msg && <div className="alert success">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      {loading ? (
        <div className="grid">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <div className="card room" key={n}>
              <div className="sk sk-img" />
              <div className="room-body"><div className="sk sk-line w60" /><div className="sk sk-line" /><div className="sk sk-line w40" /></div>
            </div>
          ))}
        </div>
      ) : rooms.length === 0 ? (
        <div className="empty">
          <div className="empty-icon">🛏️</div>
          <p>No rooms match these dates and filters.</p>
          <button className="btn ghost" onClick={resetFilters}>Clear filters</button>
        </div>
      ) : (
        <>
          <p className="muted">{rooms.length} room{rooms.length > 1 ? "s" : ""} available</p>
          <div className="grid">
            {rooms.map((r) => (
              <div className="card room" key={r.id}>
                <div className="room-media">
                  <RoomImage key={r.imageUrl} room={r} />
                  <span className={`badge floating ${r.type.toLowerCase()}`}>{r.type}</span>
                </div>
                <div className="room-body">
                  <div className="room-top">
                    <h3>Room {r.roomNumber}</h3>
                    <button className="link" onClick={() => setReviewsFor(r)}><Rating value={r.avgRating} count={r.reviewCount} /></button>
                  </div>
                  <p className="muted">{r.description}</p>
                  <p>👥 Up to {r.capacity} guest(s)</p>
                  <p className="price">{money(r.pricePerNight)}<small> / night</small></p>
                  {nights > 0 && (<p className="muted">{nights} night(s) = <b>{money(r.pricePerNight * nights)}</b></p>)}
                  {isAdmin ? (
                    <p className="muted small-text">Admins can't book rooms.</p>
                  ) : (
                    <button className="btn" onClick={() => startBooking(r)}>Book now</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {booking && <BookingModal room={booking} filters={filters} onClose={() => setBooking(null)} onBooked={booked} />}
      {reviewsFor && <ReviewsModal room={reviewsFor} onClose={() => setReviewsFor(null)} />}
    </>
  );
}
