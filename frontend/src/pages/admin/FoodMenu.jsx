import { useEffect, useState } from "react";
import api, { errMsg, imgSrc } from "../../api";
import { money, numOrNull, label } from "../../format";
import { CATEGORIES, FoodTile, VegMark } from "../Menu.jsx";

const empty = { name: "", description: "", category: "MAIN_COURSE", price: "", veg: true, imageUrl: "", available: true };

export default function FoodMenu() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const load = async () => {
    try { setItems((await api.get("/admin/menu")).data); } catch (e) { setError(errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  const body = (f) => ({
    name: f.name.trim(),
    description: f.description.trim() || null,
    category: f.category,
    price: numOrNull(f.price),
    veg: f.veg,
    imageUrl: f.imageUrl.trim() || null,
    available: f.available,
  });

  const save = async (e) => {
    e.preventDefault();
    setError("");
    setOk("");
    try {
      if (editId) await api.put(`/admin/menu/${editId}`, body(form));
      else await api.post("/admin/menu", body(form));
      setOk(editId ? "Dish updated." : "Dish added to the menu.");
      setForm(empty);
      setEditId(null);
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const edit = (i) => {
    setEditId(i.id);
    setForm({
      name: i.name, description: i.description || "", category: i.category, price: i.price,
      veg: i.veg, imageUrl: i.imageUrl || "", available: i.available,
    });
    setOk("");
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const toggle = async (i) => {
    setError("");
    try {
      await api.put(`/admin/menu/${i.id}`, { ...body(i), available: !i.available });
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  // Photo from your computer: saved straight away for the dish being edited.
  const uploadPhoto = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file || !editId) return;
    setError("");
    setOk("");
    try {
      const data = new FormData();
      data.append("file", file);
      const res = await api.post(`/admin/menu/${editId}/image`, data);
      setForm((f) => ({ ...f, imageUrl: res.data.imageUrl || "" }));
      setOk("Photo uploaded.");
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const removePhoto = async () => {
    if (!editId || !confirm("Remove this dish's photo?")) return;
    setError("");
    try {
      const res = await api.delete(`/admin/menu/${editId}/image`);
      setForm((f) => ({ ...f, imageUrl: res.data.imageUrl || "" }));
      setOk("Photo removed.");
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const previewSrc = imgSrc(form.imageUrl);

  return (
    <>
      <h1>Food menu</h1>
      {error && <div className="alert error">{error}</div>}
      {ok && <div className="alert success">{ok}</div>}
      <div className="two-col">
        <form className="card form" onSubmit={save}>
          <h3>{editId ? "Edit dish" : "Add dish"}</h3>

          <div className="photo-box">
            {previewSrc ? (
              <img className="photo-preview" src={previewSrc} alt="Dish" />
            ) : (
              <div className="photo-empty">No photo yet. Name shows on the tile.</div>
            )}
            {editId ? (
              <div className="row">
                <label className="file-btn">
                  Upload photo from computer
                  <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadPhoto} hidden />
                </label>
                {previewSrc && <button type="button" className="btn small ghost" onClick={removePhoto}>Remove photo</button>}
              </div>
            ) : (
              <span className="muted small-text">Save the dish first, then edit it to upload a photo.</span>
            )}
          </div>

          <label>Name<input required maxLength={80} value={form.name} onChange={set("name")} /></label>
          <label>Description<input maxLength={200} value={form.description} onChange={set("description")} /></label>
          <label>Category
            <select value={form.category} onChange={set("category")}>
              {CATEGORIES.map((c) => (<option key={c} value={c}>{label(c)}</option>))}
            </select>
          </label>
          <label>Price (₹)<input type="number" min="1" step="0.01" required value={form.price} onChange={set("price")} /></label>
          <label>Or paste a photo link (optional)<input maxLength={500} value={form.imageUrl} onChange={set("imageUrl")} placeholder="https://..." /></label>
          <label className="check-inline"><input type="checkbox" checked={form.veg} onChange={(e) => setForm({ ...form, veg: e.target.checked })} /> Vegetarian</label>
          <label className="check-inline"><input type="checkbox" checked={form.available} onChange={(e) => setForm({ ...form, available: e.target.checked })} /> Available to order</label>
          <div className="row">
            <button className="btn">{editId ? "Save changes" : "Add dish"}</button>
            {editId && <button type="button" className="btn ghost" onClick={() => { setEditId(null); setForm(empty); }}>Cancel</button>}
          </div>
        </form>

        <div className="table-wrap card">
          <table>
            <thead><tr><th></th><th>Dish</th><th>Category</th><th>Price</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {items.map((i) => (
                <tr key={i.id}>
                  <td style={{ width: 70 }}><div className="thumb-sm"><FoodTile item={i} /></div></td>
                  <td><VegMark veg={i.veg} /> <b>{i.name}</b><div className="muted small-text">{i.description}</div></td>
                  <td className="small-text">{label(i.category)}</td>
                  <td>{money(i.price)}</td>
                  <td><span className={`badge ${i.available ? "done" : "inactive"}`}>{i.available ? "On menu" : "Hidden"}</span></td>
                  <td className="row">
                    <button className="btn small ghost" onClick={() => edit(i)}>Edit</button>
                    <button className="btn small ghost" onClick={() => toggle(i)}>{i.available ? "Hide" : "Show"}</button>
                  </td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan="6" className="muted">No dishes yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
