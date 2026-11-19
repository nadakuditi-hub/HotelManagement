import { useState } from "react";
import { imgSrc } from "../api";

const PALETTE = {
  SINGLE: ["#dbeafe", "#93c5fd"],
  DOUBLE: ["#ede9fe", "#c4b5fd"],
  DELUXE: ["#fef3c7", "#fcd34d"],
  SUITE: ["#fce7f3", "#f9a8d4"],
};

// Drawn picture of a bed, used when a room has no photo (or the photo link is broken).
function Illustration({ type }) {
  const [c1, c2] = PALETTE[type] || PALETTE.SINGLE;
  const w = type === "SINGLE" ? 150 : type === "DOUBLE" ? 220 : 270;
  const x = (400 - w) / 2;
  const id = `bg-${type}`;
  return (
    <svg className="room-img" viewBox="0 0 400 260" preserveAspectRatio="xMidYMid slice" role="img" aria-label={`${type} room`}>
      <defs>
        <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={c1} />
          <stop offset="1" stopColor={c2} />
        </linearGradient>
      </defs>
      <rect width="400" height="260" fill={`url(#${id})`} />
      <rect x="40" y="34" width="90" height="78" rx="6" fill="#fff" opacity=".75" />
      <line x1="85" y1="34" x2="85" y2="112" stroke="#9ca3af" strokeWidth="2" />
      <line x1="40" y1="73" x2="130" y2="73" stroke="#9ca3af" strokeWidth="2" />
      <rect y="205" width="400" height="55" fill="#000" opacity=".08" />
      <rect x={x} y="118" width={w} height="72" rx="8" fill="#14213d" />
      <rect x={x - 6} y="156" width={w + 12} height="52" rx="8" fill="#fff" />
      <rect x={x - 6} y="176" width={w + 12} height="32" rx="6" fill="#b8892b" />
      <rect x={x + 10} y="140" width={Math.min(70, w / 2 - 14)} height="22" rx="10" fill="#f3f4f6" />
      {w > 150 && <rect x={x + w - 10 - Math.min(70, w / 2 - 14)} y="140" width={Math.min(70, w / 2 - 14)} height="22" rx="10" fill="#f3f4f6" />}
      {type === "SUITE" && <circle cx="330" cy="70" r="16" fill="#fff" opacity=".8" />}
    </svg>
  );
}

export default function RoomImage({ room }) {
  const [failed, setFailed] = useState(false);
  const src = imgSrc(room.imageUrl);
  if (src && !failed) {
    return <img className="room-img" src={src} alt={`Room ${room.roomNumber}`} loading="lazy" onError={() => setFailed(true)} />;
  }
  return <Illustration type={room.type} />;
}
