import { useEffect, useState } from "react";
import api, { errMsg, downloadFile } from "../../api";
import { money, dateStr } from "../../format";

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [payments, setPayments] = useState([]);
  const [monthly, setMonthly] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const [s, p, m] = await Promise.all([
          api.get("/admin/stats"),
          api.get("/admin/payments"),
          api.get("/admin/reports/monthly"),
        ]);
        setStats(s.data);
        setPayments(p.data);
        setMonthly(m.data);
      } catch (err) {
        setError(errMsg(err));
      }
    })();
  }, []);

  const exportCsv = (path, file) => downloadFile(path, file).catch((e) => setError(errMsg(e)));

  if (error) return <div className="alert error">{error}</div>;
  if (!stats) return <p>Loading...</p>;

  const occupancy = stats.totalRooms ? Math.round((stats.occupiedToday / stats.totalRooms) * 100) : 0;
  const tiles = [
    ["Revenue", money(stats.revenue)],
    ["Occupancy today", `${occupancy}%`, `${stats.occupiedToday} of ${stats.totalRooms} rooms`],
    ["Total bookings", stats.totalBookings],
    ["Confirmed", stats.confirmed],
    ["Checked in", stats.checkedIn],
    ["Cancelled", stats.cancelled],
    ["Customers", stats.customers],
  ];
  const maxRev = Math.max(1, ...monthly.map((m) => Number(m.revenue)));

  return (
    <>
      <div className="row between" style={{ marginBottom: 6 }}>
        <h1>Admin dashboard</h1>
        <span className="row">
          <button className="btn small ghost" onClick={() => exportCsv("/admin/export/bookings", "bookings.csv")}>⬇ Bookings CSV</button>
          <button className="btn small ghost" onClick={() => exportCsv("/admin/export/payments", "payments.csv")}>⬇ Payments CSV</button>
        </span>
      </div>

      <div className="grid stats">
        {tiles.map(([label, value, sub]) => (
          <div className="card stat" key={label}>
            <div className="muted">{label}</div>
            <div className="stat-value">{value}</div>
            {sub && <div className="muted small-text">{sub}</div>}
            {label === "Occupancy today" && (
              <div className="meter"><span style={{ width: `${occupancy}%` }} /></div>
            )}
          </div>
        ))}
      </div>

      <h2>Revenue by month</h2>
      {monthly.length === 0 ? <p className="muted">No payments yet.</p> : (
        <div className="card chart">
          {monthly.map((m) => (
            <div className="bar-col" key={m.month} title={`${m.month}: ${money(m.revenue)} from ${m.payments} payment(s)`}>
              <span className="bar-val">{money(m.revenue)}</span>
              <div className="bar" style={{ height: `${Math.max(4, (Number(m.revenue) / maxRev) * 100)}%` }} />
              <span className="bar-label">{m.month}</span>
            </div>
          ))}
        </div>
      )}

      <h2 style={{ marginTop: 24 }}>Recent payments</h2>
      {payments.length === 0 ? <p className="muted">No payments yet.</p> : (
        <div className="table-wrap card">
          <table>
            <thead><tr><th>Invoice</th><th>Guest</th><th>Room</th><th>Method</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
            <tbody>
              {payments.slice(0, 10).map((p) => (
                <tr key={p.id}>
                  <td>{p.invoiceNumber}</td><td>{p.customerName}</td><td>{p.roomNumber}</td><td>{p.method}</td>
                  <td>{money(p.amount)}</td>
                  <td><span className={`badge ${p.status.toLowerCase()}`}>{p.status}</span></td>
                  <td>{dateStr(p.paidAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
