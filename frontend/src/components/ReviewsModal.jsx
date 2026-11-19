import { useEffect, useState } from "react";
import api, { errMsg } from "../api";
import { Rating, StarRow } from "./Stars.jsx";

export default function ReviewsModal({ room, onClose }) {
  const [reviews, setReviews] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        setReviews((await api.get(`/rooms/${room.id}/reviews`)).data);
      } catch (err) {
        setError(errMsg(err));
      }
    })();
  }, [room.id]);

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="card modal wide" onClick={(e) => e.stopPropagation()}>
        <div className="room-top">
          <h3>Room {room.roomNumber} reviews</h3>
          <button className="btn small ghost" onClick={onClose}>Close</button>
        </div>
        <Rating value={room.avgRating} count={room.reviewCount} />
        {error && <div className="alert error">{error}</div>}
        {!reviews && !error && <p>Loading...</p>}
        {reviews && reviews.length === 0 && <p className="muted">No reviews yet. Guests can review a room after check-out.</p>}
        <div className="review-list">
          {reviews?.map((r) => (
            <div className="review" key={r.id}>
              <div className="room-top">
                <b>{r.customerName}</b>
                <span className="muted small-text">{new Date(r.createdAt).toLocaleDateString()}</span>
              </div>
              <StarRow rating={r.rating} />
              {r.comment && <p>{r.comment}</p>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
