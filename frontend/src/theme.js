// Colour themes. The choice is remembered in this browser only.
// Importing this file applies the saved theme straight away.
export const THEMES = [
  { id: "blue", label: "Classic blue" },
  { id: "gold", label: "Royal gold" },
  { id: "maroon", label: "Maroon" },
  { id: "bluewhite", label: "Blue & white" },
  { id: "dark", label: "Dark" },
];

const KEY = "theme";

export function getTheme() {
  try {
    const saved = localStorage.getItem(KEY);
    return THEMES.some((t) => t.id === saved) ? saved : "blue";
  } catch {
    return "blue";
  }
}

export function applyTheme(id) {
  document.documentElement.setAttribute("data-theme", id);
}

export function setTheme(id) {
  try {
    localStorage.setItem(KEY, id);
  } catch {
    /* storage blocked: the theme still changes for this visit */
  }
  applyTheme(id);
  return id;
}

applyTheme(getTheme());
