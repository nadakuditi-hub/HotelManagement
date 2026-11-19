# Hotel Management System (Full Stack)

**Stack:** Java 17 · Spring Boot 3 · Spring Security + JWT · Spring Data JPA · MySQL · React 18 (Vite) · Axios

## Features
**Customer:** register / login, search rooms by dates, type, guests and price range (only available rooms shown), sort by price or rating, see room photos, book, view and cancel bookings, pay (simulated), view / print invoice, and rate a room (1 to 5 stars + comment) after check-out.
**Admin:** dashboard (revenue, occupancy today, booking counts), add / edit / enable / disable rooms, upload or remove room photos, view all bookings, check-in and check-out guests, cancel bookings, view payments, monthly revenue report and CSV export (API).

## Project structure
```
hotel-management/
├── database/schema.sql        # tables + sample rooms (for reference / ER diagram)
├── backend/                   # Spring Boot REST API (port 8080)
│   └── src/main/java/com/hotel/
│       ├── model/             # JPA entities: User, Room, Booking, Payment
│       ├── repository/        # Spring Data repositories
│       ├── service/           # BookingService (business rules)
│       ├── controller/        # Auth, Room, Booking, Payment, Admin
│       ├── security/          # JWT util, filter, security config
│       ├── dto/  exception/  config/
└── frontend/                  # React app (port 5173)
    └── src/ (pages, components, api.js, AuthContext.jsx)
```

## How to run
**Requirements:** JDK 17+, Maven, Node 18+, MySQL 8.

1. **Database:** start MySQL. The database `hotel_db` is created automatically.
   Edit `backend/src/main/resources/application.properties` and set your MySQL username and password.
2. **Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   On first start it creates the tables, a default admin and 7 sample rooms.
3. **Frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173

**Admin login:** `admin@hotel.com` / `admin123` (change it before showing the project publicly).
Customers register from the Sign up page.

## REST API
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | /api/auth/register | public | Create customer account |
| POST | /api/auth/login | public | Login, returns JWT |
| GET | /api/rooms?checkIn&checkOut&type&guests&minPrice&maxPrice&sort | public | Search (available) rooms; sort = price_asc / price_desc / rating |
| GET | /api/rooms/{id} | public | Room details with average rating |
| GET | /api/rooms/{id}/reviews | public | Reviews of a room |
| POST | /api/reviews | customer | Review a stay (only after check-out, once per booking) |
| POST/DELETE | /api/admin/rooms/{id}/image | admin | Upload (multipart field `file`) or remove a room photo |
| POST | /api/bookings | customer | Book a room |
| GET | /api/bookings/my | customer | My bookings |
| PUT | /api/bookings/{id}/cancel | owner / admin | Cancel (refunds payment) |
| POST | /api/payments | owner | Pay for a booking (simulated) |
| GET | /api/payments/booking/{id} | owner / admin | Invoice |
| GET | /api/admin/stats | admin | Dashboard numbers |
| GET | /api/admin/bookings | admin | All bookings |
| PUT | /api/admin/bookings/{id}/status | admin | Check-in / check-out |
| GET | /api/admin/payments | admin | All payments |
| GET | /api/admin/users | admin | Customers |
| GET/POST/PUT/DELETE | /api/admin/rooms | admin | Room management |

Send the token as `Authorization: Bearer <token>`.

## Key business rules (good for viva)
- A room cannot be double-booked: overlapping CONFIRMED / CHECKED_IN bookings are rejected (`BookingRepository.countOverlapping`).
- Check-out date is exclusive, so a guest can arrive the day another leaves.
- Guests cannot exceed room capacity; past dates are rejected.
- Total = nights × price per night, calculated on the server.
- Status flow: CONFIRMED → CHECKED_IN → CHECKED_OUT. Check-out requires payment. Cancelling a paid booking marks the payment REFUNDED.
- Passwords are stored with BCrypt. Admin routes require the ADMIN role.

- Reviews: only the guest who stayed can review, only after check-out, one review per booking, rating 1 to 5.
- Photos: uploads are checked by their real file signature (JPG / PNG / WEBP, max 5 MB), saved in the `uploads` folder next to the backend, and served at `/uploads/...`. A room can also use a pasted image link. If a photo is missing or broken, a drawn placeholder is shown.

## Ideas to extend
Real payment gateway (Razorpay), email confirmations, housekeeping and staff modules, Docker setup, unit tests.

---

## Round 2 features

**Customer**
- **Extras / add-ons** (breakfast, extra bed, airport pickup, late check-out, spa) chosen in the booking window; priced per night or per stay.
- **Coupons & offers**: offers strip on the home page (click to copy a code); apply a code in the booking window. PERCENT coupons can have a maximum discount; FLAT coupons subtract a fixed amount.
- **Live price**: the booking window shows the full price (room + extras − discount) before you confirm.
- **Food menu** (`/menu`) with veg filter and categories; **room-service ordering** after check-in, charged to the room (`/my-orders` to track).
- **My account** (`/account`): change password.

**Admin**
- **Housekeeping** (`/admin/housekeeping`): room states (Clean / Needs cleaning / Under maintenance), create cleaning or maintenance tasks, assign staff, mark done. A room can't be checked in until it is clean. Check-out creates a cleaning task automatically.
- **Staff** (`/admin/staff`): add, edit, activate / deactivate staff.
- **Offers & extras** (`/admin/offers`): manage add-ons and coupons (limits, dates, minimum amount, usage count).
- **Food menu** (`/admin/menu`) and **Room-service orders** (`/admin/food-orders`): start preparing, deliver, cancel, mark paid. Unpaid room-service orders block check-out.
- **Dashboard**: revenue-by-month chart, occupancy bar, CSV export buttons.

**Security & validation**
- Passwords: 8–64 characters with at least one letter and one number.
- Login lockout: 5 wrong passwords within 15 minutes locks the account for 15 minutes.
- Booking limits: maximum 30 nights; booking up to 365 days ahead.
- Input validation on all forms (server side, with clear messages); safe image links only (http, https or uploaded files).
- Warning in the backend console if the JWT secret is still the default.

## New API endpoints (round 2)
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | /api/bookings/quote | customer | Price preview with extras and coupon |
| GET | /api/offers | public | Active coupons to show as offers |
| GET | /api/addons | public | Active extras |
| GET | /api/menu | public | Available menu items |
| PUT | /api/account/password | logged in | Change password |
| POST | /api/orders | customer | Room-service order (checked-in stays only) |
| GET | /api/orders/my | customer | My orders |
| PUT | /api/orders/{id}/cancel | customer | Cancel own order (only while PLACED) |
| GET/POST/PUT | /api/admin/menu | admin | Manage menu items |
| GET | /api/admin/orders | admin | All room-service orders |
| PUT | /api/admin/orders/{id}/status | admin | PLACED → PREPARING → DELIVERED (or CANCELLED) |
| PUT | /api/admin/orders/{id}/paid | admin | Mark order paid |
| GET/POST/PUT | /api/admin/staff | admin | Manage staff |
| GET | /api/admin/housekeeping/rooms | admin | Room states |
| GET/POST | /api/admin/housekeeping/tasks | admin | List / create tasks |
| PUT | /api/admin/housekeeping/tasks/{id} | admin | Change status, assign or unassign staff |
| GET/POST/PUT | /api/admin/addons | admin | Manage extras |
| GET/POST/PUT | /api/admin/coupons | admin | Manage coupons |
