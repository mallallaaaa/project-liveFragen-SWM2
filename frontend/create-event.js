const eventForm = document.getElementById("event-form");
const eventTitle = document.getElementById("event-title");
const createButton = document.getElementById("event-submit");
const eventStatus = document.getElementById("event-status");
const participantLinkBox = document.getElementById("participant-link-box");
const participantLink = document.getElementById("participant-link");

function showEventStatus(message) {
  eventStatus.textContent = message;
  eventStatus.hidden = false;
}

eventForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (createButton.disabled) return;

  const title = eventTitle.value.trim();
  if (!title || title.length > 200) {
    eventTitle.setAttribute("aria-invalid", "true");
    showEventStatus(!title ? "Please enter an event title." : "The event title must be 200 characters or fewer.");
    eventTitle.focus();
    return;
  }

  eventTitle.removeAttribute("aria-invalid");
  eventTitle.disabled = true;
  createButton.disabled = true;
  showEventStatus("Creating your event…");

  try {
    const response = await fetch("/api/events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title }),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      showEventStatus(error?.message || "Your event could not be created. Please try again.");
      return;
    }

    const created = await response.json();
    if (!created || !Number.isSafeInteger(created.id) || created.id <= 0 || typeof created.title !== "string") {
      throw new Error("Invalid event response");
    }
    eventTitle.value = "";
    showEventStatus(`Event “${created.title}” created successfully. Event ID: ${created.id}.`);
    const link = new URL(`/event.html?eventId=${created.id}`, window.location.origin);
    participantLink.href = link.href;
    participantLink.textContent = link.href;
    participantLinkBox.hidden = false;
  } catch {
    showEventStatus("The server response could not be confirmed. Check your connection before trying again.");
  } finally {
    eventTitle.disabled = false;
    createButton.disabled = false;
    eventTitle.focus();
  }
});
