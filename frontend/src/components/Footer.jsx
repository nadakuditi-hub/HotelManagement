import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div>
          <div className="footer-brand">🏨 Grand Stay</div>
          <p>Comfortable rooms, great food and friendly service.</p>
        </div>
        <div>
          <b>Explore</b>
          <Link to="/">Rooms</Link>
          <Link to="/menu">Food menu</Link>
          <Link to="/my-bookings">My bookings</Link>
        </div>
        <div>
          <b>Contact</b>
          <span>Front desk: 24 x 7</span>
          <span>+91 98765 00000</span>
          <span>stay@grandstay.example</span>
        </div>
      </div>
      <div className="footer-bottom">© {new Date().getFullYear()} Grand Stay Hotel. Demo project: payments are simulated.</div>
    </footer>
  );
}
