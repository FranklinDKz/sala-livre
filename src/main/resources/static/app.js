const $ = (selector) => document.querySelector(selector);
const state = { date: localDate(new Date()), rooms: [], reservations: [] };
let toastTimer;

function localDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function dateObject(value) {
  return new Date(`${value}T12:00:00`);
}

function readableDate(value) {
  const formatted = new Intl.DateTimeFormat("pt-BR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(dateObject(value));
  return formatted.charAt(0).toUpperCase() + formatted.slice(1);
}

function shiftDay(value, amount) {
  const date = dateObject(value);
  date.setDate(date.getDate() + amount);
  return localDate(date);
}

function time(value) {
  return value.slice(11, 16);
}

function notice(message, error = false) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.toggle("error", error);
  toast.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("visible"), 4200);
}

async function request(path, options = {}) {
  const response = await fetch(path, options);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(
      body.detail || "Não foi possível carregar a agenda. Tente novamente.",
    );
  }
  return response.json();
}

function element(tag, className, content) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (content !== undefined) node.textContent = content;
  return node;
}

function drawAxis() {
  const axis = $("#timeline-header");
  for (let hour = 7; hour < 22; hour += 1) {
    axis.append(element("span", "", `${String(hour).padStart(2, "0")}:00`));
  }
}

function renderRooms() {
  const container = $("#room-rows");
  container.replaceChildren();
  for (const room of state.rooms) {
    const row = element("div", "room-row");
    const info = element("div", "room-info");
    const letter = element("span", "room-letter", room.name.slice(0, 1));
    const details = element("div");
    details.append(
      element("strong", "", room.name),
      element("small", "", room.location),
      element("span", "capacity", `${room.capacity} pessoas`),
    );
    info.append(letter, details);

    const track = element("div", "timeline-track");
    track.setAttribute("aria-label", `Ocupação da ${room.name}`);
    const bookings = state.reservations.filter(
      (reservation) =>
        reservation.roomId === room.id && reservation.status === "ACTIVE",
    );
    for (const booking of bookings) {
      const start =
        Number(time(booking.startsAt).slice(0, 2)) +
        Number(time(booking.startsAt).slice(3)) / 60;
      const end =
        Number(time(booking.endsAt).slice(0, 2)) +
        Number(time(booking.endsAt).slice(3)) / 60;
      const block = element("div", "booking-block");
      block.style.left = `${((start - 7) / 15) * 100}%`;
      block.style.width = `${((end - start) / 15) * 100}%`;
      block.title = `${booking.title} · ${time(booking.startsAt)}–${time(booking.endsAt)} · ${booking.requester}`;
      block.append(
        element("strong", "", booking.title),
        element(
          "small",
          "",
          `${time(booking.startsAt)}–${time(booking.endsAt)}`,
        ),
      );
      track.append(block);
    }
    row.append(info, track);
    container.append(row);
  }
}

function renderReservations() {
  const list = $("#reservation-list");
  list.replaceChildren();
  $("#empty-state").hidden = state.reservations.length > 0;
  $("#visible-count").textContent =
    `${state.reservations.length} ${state.reservations.length === 1 ? "registro" : "registros"}`;
  for (const booking of state.reservations) {
    const item = element(
      "article",
      `reservation-item${booking.status === "CANCELLED" ? " cancelled" : ""}`,
    );
    const period = element(
      "div",
      "reservation-time",
      `${time(booking.startsAt)}–\n${time(booking.endsAt)}`,
    );
    const detail = element("div", "reservation-detail");
    detail.append(
      element("strong", "", booking.title),
      element("span", "", `${booking.roomName} · ${booking.requester}`),
    );
    item.append(period, detail);
    if (booking.status === "ACTIVE") {
      const cancel = element("button", "cancel-button", "Cancelar");
      cancel.type = "button";
      cancel.setAttribute("aria-label", `Cancelar ${booking.title}`);
      cancel.addEventListener("click", () => cancelBooking(booking));
      item.append(cancel);
    } else {
      item.append(element("span", "cancelled-label", "Cancelada"));
    }
    list.append(item);
  }
}

function render() {
  $("#selected-day").textContent = readableDate(state.date);
  $("#date-picker").value = state.date;
  $("#room-count").textContent = state.rooms.length;
  $("#booking-count").textContent = state.reservations.filter(
    (item) => item.status === "ACTIVE",
  ).length;
  renderRooms();
  renderReservations();
}

async function load() {
  const requestedDate = state.date;
  try {
    const [rooms, reservations] = await Promise.all([
      state.rooms.length ? Promise.resolve(state.rooms) : request("/api/rooms"),
      request(`/api/reservations?date=${requestedDate}`),
    ]);
    if (requestedDate !== state.date) return;
    state.rooms = rooms;
    state.reservations = reservations;
    render();
  } catch (error) {
    notice(error.message, true);
  }
}

function setDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return;
  state.date = value;
  load();
}

function populateTimes() {
  const start = $("#start-time");
  const end = $("#end-time");
  for (let minutes = 7 * 60; minutes <= 22 * 60; minutes += 30) {
    const value = `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
    if (minutes < 22 * 60) start.add(new Option(value, value));
    if (minutes > 7 * 60) end.add(new Option(value, value));
  }
}

function openBooking() {
  if (state.date < localDate(new Date())) {
    notice("Escolha hoje ou uma data futura para criar uma reserva.", true);
    return;
  }
  const form = $("#booking-form");
  form.reset();
  const roomSelect = $("#room-select");
  roomSelect.replaceChildren();
  for (const room of state.rooms)
    roomSelect.add(
      new Option(`${room.name} · ${room.capacity} pessoas`, room.id),
    );

  let start = "09:00";
  if (state.date === localDate(new Date())) {
    const now = new Date();
    const nextSlot = Math.max(
      7 * 60,
      Math.ceil((now.getHours() * 60 + now.getMinutes() + 1) / 30) * 30,
    );
    if (nextSlot > 21 * 60 + 30) {
      notice("Não há mais horários para hoje. Escolha outro dia.", true);
      return;
    }
    start = `${String(Math.floor(nextSlot / 60)).padStart(2, "0")}:${String(nextSlot % 60).padStart(2, "0")}`;
  }
  const endMinutes =
    Number(start.slice(0, 2)) * 60 + Number(start.slice(3)) + 60;
  const end = `${String(Math.floor(Math.min(endMinutes, 22 * 60) / 60)).padStart(2, "0")}:${String(Math.min(endMinutes, 22 * 60) % 60).padStart(2, "0")}`;
  $("#start-time").value = start;
  $("#end-time").value = end;
  $("#dialog-date").textContent = readableDate(state.date);
  $("#booking-dialog").showModal();
  form.elements.title.focus();
}

async function cancelBooking(booking) {
  if (!confirm(`Cancelar a reserva “${booking.title}”?`)) return;
  try {
    await request(`/api/reservations/${booking.id}/cancel`, {
      method: "PATCH",
    });
    notice("Reserva cancelada. O horário está livre novamente.");
    await load();
  } catch (error) {
    notice(error.message, true);
  }
}

$("#previous-day").addEventListener("click", () =>
  setDate(shiftDay(state.date, -1)),
);
$("#next-day").addEventListener("click", () =>
  setDate(shiftDay(state.date, 1)),
);
$("#today-button").addEventListener("click", () =>
  setDate(localDate(new Date())),
);
$("#date-picker").addEventListener("change", (event) =>
  setDate(event.target.value),
);
$("#new-booking").addEventListener("click", openBooking);
$("#empty-booking").addEventListener("click", openBooking);
$("#close-dialog").addEventListener("click", () =>
  $("#booking-dialog").close(),
);
$("#cancel-dialog").addEventListener("click", () =>
  $("#booking-dialog").close(),
);
$("#booking-dialog").addEventListener("click", (event) => {
  if (event.target === $("#booking-dialog")) $("#booking-dialog").close();
});
$("#booking-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = $("#save-booking");
  button.disabled = true;
  const data = Object.fromEntries(new FormData(event.target));
  try {
    await request("/api/reservations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        roomId: Number(data.roomId),
        title: data.title,
        requester: data.requester,
        startsAt: `${state.date}T${data.start}:00`,
        endsAt: `${state.date}T${data.end}:00`,
      }),
    });
    $("#booking-dialog").close();
    notice("Reserva confirmada.");
    await load();
  } catch (error) {
    notice(error.message, true);
  } finally {
    button.disabled = false;
  }
});

drawAxis();
populateTimes();
load();
