import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import Navbar from "./components/Navbar.jsx";
import Footer from "./components/Footer.jsx";
import Rooms from "./pages/Rooms.jsx";
import Menu from "./pages/Menu.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import MyBookings from "./pages/MyBookings.jsx";
import MyOrders from "./pages/MyOrders.jsx";
import Account from "./pages/Account.jsx";
import Dashboard from "./pages/admin/Dashboard.jsx";
import ManageRooms from "./pages/admin/ManageRooms.jsx";
import ManageBookings from "./pages/admin/ManageBookings.jsx";
import Housekeeping from "./pages/admin/Housekeeping.jsx";
import Staff from "./pages/admin/Staff.jsx";
import Offers from "./pages/admin/Offers.jsx";
import FoodMenu from "./pages/admin/FoodMenu.jsx";
import FoodOrders from "./pages/admin/FoodOrders.jsx";

function Protected({ children, admin }) {
  const { user, isAdmin } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (admin && !isAdmin) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Rooms />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/my-bookings" element={<Protected><MyBookings /></Protected>} />
          <Route path="/my-orders" element={<Protected><MyOrders /></Protected>} />
          <Route path="/account" element={<Protected><Account /></Protected>} />
          <Route path="/admin" element={<Protected admin><Dashboard /></Protected>} />
          <Route path="/admin/bookings" element={<Protected admin><ManageBookings /></Protected>} />
          <Route path="/admin/rooms" element={<Protected admin><ManageRooms /></Protected>} />
          <Route path="/admin/housekeeping" element={<Protected admin><Housekeeping /></Protected>} />
          <Route path="/admin/food-orders" element={<Protected admin><FoodOrders /></Protected>} />
          <Route path="/admin/menu" element={<Protected admin><FoodMenu /></Protected>} />
          <Route path="/admin/offers" element={<Protected admin><Offers /></Protected>} />
          <Route path="/admin/staff" element={<Protected admin><Staff /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}
