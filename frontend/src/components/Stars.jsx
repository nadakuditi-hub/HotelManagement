// Average rating shown on room cards, e.g. "★ 4.5 (12)"
export function Rating({ value, count }) {
  if (!count) return <span className="muted small-text">No reviews yet</span>;
  return (
    <span className="rating">
      <span className="star on">★</span> <b>{Number(value).toFixed(1)}</b>
      <span className="muted"> ({count} {count === 1 ? "review" : "reviews"})</span>
    </span>
  );
}

// Fixed row of 5 stars for one review, e.g. ★★★★☆
export function StarRow({ rating }) {
  return (
    <span className="stars" aria-label={`${rating} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={`star ${n <= rating ? "on" : ""}`}>★</span>
      ))}
    </span>
  );
}

// Clickable stars used when writing a review
export function StarInput({ value, onChange }) {
  return (
    <div className="stars input" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          type="button"
          key={n}
          className={`star ${n <= value ? "on" : ""}`}
          onClick={() => onChange(n)}
          aria-label={`${n} star${n > 1 ? "s" : ""}`}
          aria-checked={n === value}
          role="radio"
        >
          ★
        </button>
      ))}
    </div>
  );
}
