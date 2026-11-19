import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg, imgSrc } from "../api";
import { useAuth } from "../AuthContext.jsx";
import { money, label } from "../format";

export const CATEGORIES = ["BREAKFAST", "STARTER", "MAIN_COURSE", "DESSERT", "BEVERAGE"];
export const CAT_ICON = { BREAKFAST: "🍳", STARTER: "🥗", MAIN_COURSE: "🍛", DESSERT: "🍰", BEVERAGE: "☕" };

export function FoodTile({ item }) {
  const [bad, setBad] = useState(false);
  const src = imgSrc(item.imageUrl);
  if (src && !bad) return <img className="food-img" src={src} alt={item.name} onError={() => setBad(true)} />;
  return (
    <div className={`food-tile cat-${item.category.toLowerCase()}`}>
      <span className="food-emoji">{CAT_ICON[item.category] || "🍽️"}</span>
      <span className="food-name">{item.name}</span>
    </div>
  );
}

export function VegMark({ veg }) {
  return <span className={`veg-mark ${veg ? "veg" : "nonveg"}`} title={veg ? "Vegetarian" : "Non-vegetarian"}><i /></span>;
}

export default function Menu() {
  const { user, isAdmin } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [cat, setCat] = useState("ALL");
  const [vegOnly, setVegOnly] = useState(false);
  const [stay, setStay] = useState(null); // current CHECKED_IN booking, if any
  const [cart, setCart] = useState({}); // menuItemId -> quantity
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get("/menu").then((r) => setItems(r.data)).catch((e) => setError(errMsg(e))).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!user || isAdmin) return;
    api.get("/bookings/my")
      .then((r) => setStay(r.data.find((b) => b.status === "CHECKED_IN") || null))
      .catch(() => {});
  }, [user, isAdmin]);

  const shown = useMemo(
    () => items.filter((i) => (cat === "ALL" || i.category === cat) && (!vegOnly || i.veg)),
    [items, cat, vegOnly]
  );

  const change = (id, delta) =>
    setCart((c) => {
      const q = Math.max(0, Math.min(10, (c[id] || 0) + delta));
      const next = { ...c };
      if (q === 0) delete next[id]; else next[id] = q;
      return next;
    });

  const lines = items.filter((i) => cart[i.id]).map((i) => ({ ...i, qty: cart[i.id] }));
  const total = lines.reduce((s, l) => s + Number(l.price) * l.qty, 0);

  const placeOrder = async () => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api.post("/orders", {
        bookingId: stay.id,
        items: lines.map((l) => ({ menuItemId: l.id, quantity: l.qty })),
        notes: notes.trim() || null,
      });
      setCart({});
      setNotes("");
      setNotice("Order placed! The kitchen will bring it to your room. Track it in “My orders”.");
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <section className="hero small">
        <h1>Food menu</h1>
        <p>Fresh meals from our kitchen, delivered to your room 24 x 7.</p>
      </section>

      {!user ? (
        <div className="alert info">Want to order to your room? <Link to="/login">Log in</Link> after you check in.</div>
      ) : isAdmin ? (
        <div className="alert info">You are viewing the guest menu. Edit items under <Link to="/admin/menu">Admin → Menu</Link>.</div>
      ) : stay ? (
        <div className="alert success">You are checked in to room {stay.roomNumber}. Add items and place a room-service order.</div>
      ) : (
        <div className="alert info">Room-service ordering opens once you are checked in. You can browse the menu any time.</div>
      )}
      {notice && <div className="alert success">{notice}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="chips">
        {["ALL", ...CATEGORIES].map((c) => (
          <button key={c} className={`chip ${cat === c ? "on" : ""}`} onClick={() => setCat(c)}>
            {c === "ALL" ? "All" : `${CAT_ICON[c]} ${label(c)}`}
          </button>
        ))}
        <label className="check-inline">
          <input type="checkbox" checked={vegOnly} onChange={(e) => setVegOnly(e.target.checked)} /> Veg only
        </label>
      </div>

      {loading ? (
        <div className="grid food-grid">
          {[1, 2, 3, 4, 5, 6].map((n) => (<div key={n} className="card food"><div className="sk sk-img" /><div className="sk sk-line" /><div className="sk sk-line w60" /></div>))}
        </div>
      ) : shown.length === 0 ? (
        <div className="empty"><div className="empty-icon">🍽️</div><p>No dishes found.</p></div>
      ) : (
        <div className="grid food-grid">
          {shown.map((i) => (
            <div className="card food" key={i.id}>
              <FoodTile item={i} />
              <div className="food-body">
                <div className="room-top">
                  <b>{i.name}</b>
                  <VegMark veg={i.veg} />
                </div>
                <p className="muted small-text">{i.description}</p>
                <div className="room-top">
                  <span className="food-price">{money(i.price)}</span>
                  {!user && <Link className="btn small" to="/login">Log in to order</Link>}
                  {user && !isAdmin && !stay && (
                    <button className="btn small" disabled title="Room service opens after you check in">Order after check-in</button>
                  )}
                  {stay && !isAdmin && (
                    cart[i.id] ? (
                      <span className="qty">
                        <button onClick={() => change(i.id, -1)}>−</button>
                        <b>{cart[i.id]}</b>
                        <button onClick={() => change(i.id, 1)}>+</button>
                      </span>
                    ) : (
                      <button className="btn small" onClick={() => change(i.id, 1)}>Add</button>
                    )
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {lines.length > 0 && stay && (
        <div className="cart-bar card">
          <div className="cart-lines">
            {lines.map((l) => (<span key={l.id}>{l.qty} × {l.name}</span>))}
          </div>
          <input placeholder="Note for the kitchen (optional)" maxLength={200} value={notes} onChange={(e) => setNotes(e.target.value)} />
          <div className="row between">
            <b>Total {money(total)} <span className="muted small-text">(charged to room {stay.roomNumber})</span></b>
            <span className="row">
              <button className="btn ghost small" onClick={() => setCart({})}>Clear</button>
              <button className="btn" disabled={busy} onClick={placeOrder}>{busy ? "Placing..." : "Place order"}</button>
            </span>
          </div>
        </div>
      )}
    </>
  );
}
