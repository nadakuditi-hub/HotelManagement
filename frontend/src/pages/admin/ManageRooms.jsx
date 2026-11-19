import { useEffect, useState } from "react";
import api, { errMsg } from "../../api";
import RoomImage from "../../components/RoomImage.jsx";

const empty = { roomNumber: "", type: "SINGLE", pricePerNight: "", capacity: 1, description: "", imageUrl: "", active: true };
const MAX_MB = 5;

export default function ManageRooms() {
  const [rooms, setRooms] = useState([]);
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = async () => {
    try { setRooms((await api.get("/admin/rooms")).data); } catch (err) { setError(errMsg(err)); }
  };
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setNotice("");
    const body = { ...form, pricePerNight: Number(form.pricePerNight), capacity: Number(form.capacity) };
    try {
      if (editingId) await api.put(`/admin/rooms/${editingId}`, body);
      else await api.post("/admin/rooms", body);
      setForm(empty);
      setEditingId(null);
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const edit = (r) => {
    setEditingId(r.id);
    setForm({ roomNumber: r.roomNumber, type: r.type, pricePerNight: r.pricePerNight, capacity: r.capacity,
      description: r.description || "", imageUrl: r.imageUrl || "", active: r.active });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const toggle = async (r) => {
    try {
      await api.put(`/admin/rooms/${r.id}`, { ...r, active: !r.active });
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const upload = async (room, file) => {
    if (!file) return;
    setError("");
    setNotice("");
    if (file.size > MAX_MB * 1024 * 1024) return setError(`Image must be ${MAX_MB} MB or smaller`);
    const data = new FormData();
    data.append("file", file);
    try {
      await api.post(`/admin/rooms/${room.id}/image`, data);
      setNotice(`Photo updated for room ${room.roomNumber}.`);
      load();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  const removePhoto = async (room) => {
    if (!confirm(`Remove the photo of room ${room.roomNumber}?`)) return;
    try {
      await api.delete(`/admin/rooms/${room.id}/image`);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1>Manage rooms</h1>
      {notice && <div className="alert success">{notice}</div>}
      {error && <div className="alert error">{error}</div>}
      <form className="card filters" onSubmit={submit}>
        <label>Room no.<input required value={form.roomNumber} onChange={set("roomNumber")} /></label>
        <label>Type
          <select value={form.type} onChange={set("type")}>
            <option>SINGLE</option><option>DOUBLE</option><option>DELUXE</option><option>SUITE</option>
          </select>
        </label>
        <label>Price / night (₹)<input type="number" min="1" required value={form.pricePerNight} onChange={set("pricePerNight")} /></label>
        <label>Capacity<input type="number" min="1" max="10" required value={form.capacity} onChange={set("capacity")} /></label>
        <label className="wide">Description<input value={form.description} onChange={set("description")} /></label>
        <label className="wide">Photo link (optional)<input placeholder="https://..." value={form.imageUrl} onChange={set("imageUrl")} /></label>
        <button className="btn">{editingId ? "Update room" : "Add room"}</button>
        {editingId && <button type="button" className="btn ghost" onClick={() => { setEditingId(null); setForm(empty); }}>Cancel</button>}
      </form>
      <p className="muted small-text">To upload a photo from your computer, use the Upload button in the table below (JPG, PNG or WEBP, up to {MAX_MB} MB).</p>

      <div className="table-wrap card">
        <table>
          <thead><tr><th>Photo</th><th>Room</th><th>Type</th><th>Price</th><th>Capacity</th><th>Description</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {rooms.map((r) => (
              <tr key={r.id}>
                <td>
                  <div className="thumb"><RoomImage key={r.imageUrl} room={r} /></div>
                  <div className="row" style={{ marginTop: 6 }}>
                    <label className="file-btn">
                      Upload
                      <input type="file" accept="image/jpeg,image/png,image/webp" hidden
                        onChange={(e) => { upload(r, e.target.files[0]); e.target.value = ""; }} />
                    </label>
                    {r.imageUrl && <button className="btn small ghost" onClick={() => removePhoto(r)}>Remove</button>}
                  </div>
                </td>
                <td>{r.roomNumber}</td><td>{r.type}</td>
                <td>₹{Number(r.pricePerNight).toLocaleString("en-IN")}</td>
                <td>{r.capacity}</td><td>{r.description}</td>
                <td><span className={`badge ${r.active ? "confirmed" : "cancelled"}`}>{r.active ? "ACTIVE" : "INACTIVE"}</span></td>
                <td>
                  <div className="row">
                    <button className="btn small ghost" onClick={() => edit(r)}>Edit</button>
                    <button className="btn small ghost" onClick={() => toggle(r)}>{r.active ? "Disable" : "Enable"}</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
