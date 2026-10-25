const questionList = document.getElementById("question-list");
const listStatus = document.getElementById("questions-status");
const refreshButton = document.getElementById("questions-refresh");
let latestLoad = 0;

async function loadQuestions() {
  const load = ++latestLoad;
  refreshButton.disabled = true;
  questionList.setAttribute("aria-busy", "true");
  listStatus.textContent = "Loading questions…";

  try {
    const response = await fetch("/api/questions", { cache: "no-store" });
    if (!response.ok) throw new Error("Could not load questions");
    const questions = await response.json();
    if (!Array.isArray(questions)) throw new Error("Invalid question list");
    if (load !== latestLoad) return;

    const items = questions.map((question) => {
      const item = document.createElement("li");
      item.className = "border-top border-light border-opacity-25 pt-3 mt-3";
      item.textContent = question.text;
      return item;
    });
    questionList.replaceChildren(...items);
    listStatus.textContent = questions.length ? "" : "No questions yet. Be the first to ask!";
  } catch {
    if (load !== latestLoad) return;
    listStatus.textContent = "Questions could not be refreshed. Please try Refresh again.";
  } finally {
    if (load === latestLoad) {
      refreshButton.disabled = false;
      questionList.setAttribute("aria-busy", "false");
    }
  }
}

refreshButton.addEventListener("click", loadQuestions);
document.getElementById("question-form").addEventListener("question-submitted", loadQuestions);
loadQuestions();
