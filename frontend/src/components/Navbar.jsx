import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import { THEMES, getTheme, setTheme } from "../theme.js";

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [theme, setThemeState] = useState(getTheme());
  const initial = user?.fullName?.trim()?.[0]?.toUpperCase() || "?";

  return (
    <>
      <header className="navbar">
        <Link to={isAdmin ? "/admin" : "/"} className="brand">🏨 Grand Stay</Link>
        <nav>
          <NavLink to="/" end>Rooms</NavLink>
          <NavLink to="/menu">Food menu</NavLink>
          {user && !isAdmin && (
            <>
              <NavLink to="/my-bookings">My bookings</NavLink>
              <NavLink to="/my-orders">My orders</NavLink>
            </>
          )}
          {user ? (
            <>
              <NavLink to="/account" className="user-chip" title="My account">
                <span className="avatar">{initial}</span>
                <span className="user-name">{user.fullName.split(" ")[0]}</span>
              </NavLink>
              <button className="btn small ghost light" onClick={() => { logout(); navigate("/"); }}>Logout</button>
            </>
          ) : (
            <>
              <NavLink to="/login">Login</NavLink>
              <Link to="/register" className="btn small gold">Sign up</Link>
            </>
          )}
          <select
            className="theme-select"
            value={theme}
            title="Choose a colour theme"
            aria-label="Colour theme"
            onChange={(e) => setThemeState(setTheme(e.target.value))}
          >
            {THEMES.map((t) => (<option key={t.id} value={t.id}>🎨 {t.label}</option>))}
          </select>
        </nav>
      </header>

      {isAdmin && (
        <div className="subnav">
          <div className="subnav-inner">
            <NavLink to="/admin" end>Dashboard</NavLink>
            <NavLink to="/admin/bookings">Bookings</NavLink>
            <NavLink to="/admin/rooms">Rooms</NavLink>
            <NavLink to="/admin/housekeeping">Housekeeping</NavLink>
            <NavLink to="/admin/food-orders">Food orders</NavLink>
            <NavLink to="/admin/menu">Menu</NavLink>
            <NavLink to="/admin/offers">Offers &amp; extras</NavLink>
            <NavLink to="/admin/staff">Staff</NavLink>
          </div>
        </div>
      )}
    </>
  );
}
