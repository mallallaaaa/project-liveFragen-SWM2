const form = document.getElementById("question-form");
const input = document.getElementById("question-input");
const submitButton = document.getElementById("question-submit");
const status = document.getElementById("question-status");

function showStatus(message) {
  status.textContent = message;
  status.hidden = false;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (submitButton.disabled) return;

  const text = input.value.trim();
  if (!text || text.length > 500) {
    input.setAttribute("aria-invalid", "true");
    showStatus(!text ? "Please enter a question." : "Your question must be 500 characters or fewer.");
    input.focus();
    return;
  }

  input.removeAttribute("aria-invalid");
  submitButton.disabled = true;
  input.disabled = true;
  showStatus("Submitting your question…");

  try {
    const response = await fetch("/api/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      showStatus(error.message || "Your question could not be submitted. Please try again.");
      return;
    }

    input.value = "";
    showStatus("Your question has been submitted successfully.");
  } catch {
    showStatus("Cannot reach the server. Check your connection and try again.");
  } finally {
    submitButton.disabled = false;
    input.disabled = false;
    input.focus();
  }
});
