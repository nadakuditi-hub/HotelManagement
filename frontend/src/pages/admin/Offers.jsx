import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";
import { money, numOrNull, dateStr } from "../../format";

const emptyAddOn = { name: "", description: "", price: "", perNight: false, active: true };
const emptyCoupon = {
  code: "", description: "", type: "PERCENT", value: "", minAmount: "", maxDiscount: "",
  validFrom: "", validTo: "", usageLimit: "", active: true,
};

export default function Offers() {
  const [addOns, setAddOns] = useState([]);
  const [coupons, setCoupons] = useState([]);
  const [aForm, setAForm] = useState(emptyAddOn);
  const [aId, setAId] = useState(null);
  const [cForm, setCForm] = useState(emptyCoupon);
  const [cId, setCId] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = async () => {
    try {
      const [a, c] = await Promise.all([api.get("/admin/addons"), api.get("/admin/coupons")]);
      setAddOns(a.data);
      setCoupons(c.data);
    } catch (e) {
      setError(errMsg(e));
    }
  };
  useEffect(() => { load(); }, []);

  const attempt = async (fn, msg) => {
    setError("");
    setOk("");
    try { await fn(); setOk(msg); load(); } catch (e) { setError(errMsg(e)); }
  };

  // ----- extras -----
  const addOnBody = (f) => ({
    name: f.name.trim(),
    description: f.description.trim() || null,
    price: numOrNull(f.price),
    perNight: f.perNight,
    active: f.active,
  });
  const saveAddOn = (e) => {
    e.preventDefault();
    attempt(async () => {
      if (aId) await api.put(`/admin/addons/${aId}`, addOnBody(aForm));
      else await api.post("/admin/addons", addOnBody(aForm));
      setAForm(emptyAddOn);
      setAId(null);
    }, "Extra saved.");
  };
  const editAddOn = (a) => {
    setAId(a.id);
    setAForm({ name: a.name, description: a.description || "", price: a.price, perNight: a.perNight, active: a.active });
  };
  const toggleAddOn = (a) =>
    attempt(() => api.put(`/admin/addons/${a.id}`, { ...addOnBody(a), active: !a.active }), "Extra updated.");

  // ----- coupons -----
  const couponBody = (f) => ({
    code: f.code.trim().toUpperCase(),
    description: f.description.trim() || null,
    type: f.type,
    value: numOrNull(f.value),
    minAmount: numOrNull(f.minAmount),
    maxDiscount: numOrNull(f.maxDiscount),
    validFrom: f.validFrom || null,
    validTo: f.validTo || null,
    usageLimit: numOrNull(f.usageLimit),
    active: f.active,
  });
  const saveCoupon = (e) => {
    e.preventDefault();
    attempt(async () => {
      if (cId) await api.put(`/admin/coupons/${cId}`, couponBody(cForm));
      else await api.post("/admin/coupons", couponBody(cForm));
      setCForm(emptyCoupon);
      setCId(null);
    }, "Coupon saved.");
  };
  const editCoupon = (c) => {
    setCId(c.id);
    setCForm({
      code: c.code, description: c.description || "", type: c.type, value: c.value,
      minAmount: c.minAmount ?? "", maxDiscount: c.maxDiscount ?? "",
      validFrom: c.validFrom || "", validTo: c.validTo || "",
      usageLimit: c.usageLimit ?? "", active: c.active,
    });
  };
  const toggleCoupon = (c) =>
    attempt(() => api.put(`/admin/coupons/${c.id}`, { ...couponBody(c), active: !c.active }), "Coupon updated.");

  const cs = (k) => (e) => setCForm({ ...cForm, [k]: e.target.value });
  const as = (k) => (e) => setAForm({ ...aForm, [k]: e.target.value });

  return (
    <>
      <h1>Offers &amp; extras</h1>
      {error && <div className="alert error">{error}</div>}
      {ok && <div className="alert success">{ok}</div>}

      <h2>Add-ons &amp; extras</h2>
      <div className="two-col">
        <form className="card form" onSubmit={saveAddOn}>
          <h3>{aId ? "Edit extra" : "New extra"}</h3>
          <label>Name<input required maxLength={60} value={aForm.name} onChange={as("name")} /></label>
          <label>Description<input maxLength={200} value={aForm.description} onChange={as("description")} /></label>
          <label>Price (₹)<input type="number" min="0" step="0.01" required value={aForm.price} onChange={as("price")} /></label>
          <label className="check-inline"><input type="checkbox" checked={aForm.perNight} onChange={(e) => setAForm({ ...aForm, perNight: e.target.checked })} /> Charged per night (otherwise once per stay)</label>
          <label className="check-inline"><input type="checkbox" checked={aForm.active} onChange={(e) => setAForm({ ...aForm, active: e.target.checked })} /> Active</label>
          <div className="row">
            <button className="btn">{aId ? "Save" : "Add extra"}</button>
            {aId && <button type="button" className="btn ghost" onClick={() => { setAId(null); setAForm(emptyAddOn); }}>Cancel</button>}
          </div>
        </form>
        <div className="table-wrap card">
          <table>
            <thead><tr><th>Name</th><th>Price</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {addOns.map((a) => (
                <tr key={a.id}>
                  <td><b>{a.name}</b><div className="muted small-text">{a.description}</div></td>
                  <td>{money(a.price)}<div className="muted small-text">{a.perNight ? "per night" : "per stay"}</div></td>
                  <td><span className={`badge ${a.active ? "done" : "inactive"}`}>{a.active ? "Active" : "Hidden"}</span></td>
                  <td className="row">
                    <button className="btn small ghost" onClick={() => editAddOn(a)}>Edit</button>
                    <button className="btn small ghost" onClick={() => toggleAddOn(a)}>{a.active ? "Hide" : "Show"}</button>
                  </td>
                </tr>
              ))}
              {addOns.length === 0 && <tr><td colSpan="4" className="muted">No extras yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <h2 style={{ marginTop: 28 }}>Coupons &amp; offers</h2>
      <div className="two-col">
        <form className="card form" onSubmit={saveCoupon}>
          <h3>{cId ? "Edit coupon" : "New coupon"}</h3>
          <label>Code<input required maxLength={30} pattern="[A-Za-z0-9_-]+" value={cForm.code} onChange={cs("code")} placeholder="e.g. SUMMER15" /></label>
          <label>Description (shown to guests)<input maxLength={200} value={cForm.description} onChange={cs("description")} /></label>
          <label>Discount type
            <select value={cForm.type} onChange={cs("type")}>
              <option value="PERCENT">Percent off</option>
              <option value="FLAT">Flat amount off (₹)</option>
            </select>
          </label>
          <label>Value<input type="number" min="1" step="0.01" required value={cForm.value} onChange={cs("value")} /></label>
          <label>Minimum booking amount (₹, optional)<input type="number" min="0" value={cForm.minAmount} onChange={cs("minAmount")} /></label>
          {cForm.type === "PERCENT" && (
            <label>Maximum discount (₹, optional)<input type="number" min="1" value={cForm.maxDiscount} onChange={cs("maxDiscount")} /></label>
          )}
          <div className="row">
            <label style={{ flex: 1 }}>Valid from<input type="date" value={cForm.validFrom} onChange={cs("validFrom")} /></label>
            <label style={{ flex: 1 }}>Valid till<input type="date" value={cForm.validTo} onChange={cs("validTo")} /></label>
          </div>
          <label>Usage limit (optional)<input type="number" min="1" value={cForm.usageLimit} onChange={cs("usageLimit")} /></label>
          <label className="check-inline"><input type="checkbox" checked={cForm.active} onChange={(e) => setCForm({ ...cForm, active: e.target.checked })} /> Active</label>
          <div className="row">
            <button className="btn">{cId ? "Save" : "Create coupon"}</button>
            {cId && <button type="button" className="btn ghost" onClick={() => { setCId(null); setCForm(emptyCoupon); }}>Cancel</button>}
          </div>
        </form>
        <div className="table-wrap card">
          <table>
            <thead><tr><th>Code</th><th>Discount</th><th>Used</th><th>Valid</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {coupons.map((c) => (
                <tr key={c.id}>
                  <td><b>{c.code}</b><div className="muted small-text">{c.description}</div></td>
                  <td>{c.type === "PERCENT" ? `${c.value}%` : money(c.value)}{c.minAmount ? <div className="muted small-text">min {money(c.minAmount)}</div> : null}</td>
                  <td>{c.usedCount} / {c.usageLimit ?? "∞"}</td>
                  <td className="small-text">{c.validFrom ? dateStr(c.validFrom) : "Any"} → {c.validTo ? dateStr(c.validTo) : "No end"}</td>
                  <td><span className={`badge ${c.active ? "done" : "inactive"}`}>{c.active ? "Active" : "Off"}</span></td>
                  <td className="row">
                    <button className="btn small ghost" onClick={() => editCoupon(c)}>Edit</button>
                    <button className="btn small ghost" onClick={() => toggleCoupon(c)}>{c.active ? "Turn off" : "Turn on"}</button>
                  </td>
                </tr>
              ))}
              {coupons.length === 0 && <tr><td colSpan="6" className="muted">No coupons yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
