const joinStatus = document.getElementById("join-status");
const joinedEvent = document.getElementById("joined-event");
const joinedTitle = document.getElementById("joined-event-title");
const joinedId = document.getElementById("joined-event-id");
const retryJoin = document.getElementById("join-retry");
const eventIds = new URLSearchParams(window.location.search).getAll("eventId");
const eventId = eventIds[0];

async function joinEvent() {
  joinedEvent.hidden = true;
  retryJoin.hidden = true;
  if (eventIds.length !== 1 || !/^[1-9]\d*$/.test(eventId)) {
    joinStatus.textContent = "This event link is invalid. Please ask the presenter for a valid link.";
    return;
  }

  retryJoin.disabled = true;
  joinStatus.textContent = "Loading event…";
  try {
    const response = await fetch(`/api/events/${eventId}`, { cache: "no-store" });
    if (!response.ok) {
      if (response.status === 404) {
        joinStatus.textContent = "This event could not be found. Please check the link with the presenter.";
      } else if (response.status === 400) {
        joinStatus.textContent = "This event link is invalid. Please ask the presenter for a valid link.";
      } else {
        throw new Error("Could not load event");
      }
      return;
    }
    const event = await response.json();
    if (!event || String(event.id) !== eventId || typeof event.title !== "string") {
      throw new Error("Invalid event response");
    }
    joinedTitle.textContent = event.title;
    joinedId.textContent = `Event ID: ${event.id}`;
    joinedEvent.hidden = false;
    joinStatus.textContent = "You have joined this event. No registration required.";
  } catch {
    joinStatus.textContent = "Cannot load the event. Check your connection and try again.";
    retryJoin.hidden = false;
  } finally {
    retryJoin.disabled = false;
  }
}

retryJoin.addEventListener("click", joinEvent);
joinEvent();
