const nav = document.getElementById("navigation");
const menuButton = document.querySelector(".menu-button");
const chat = document.getElementById("chat");
const form = document.getElementById("ai-form");
const input = document.getElementById("ai-input");
const submitButton = form ? form.querySelector("button") : null;

const aiHistory = [];

function toggleMenu() {
if (nav) {
nav.classList.toggle("open");
}
}

function closeMenu() {
if (nav) {
nav.classList.remove("open");
}
}

if (menuButton) {
menuButton.addEventListener("click", toggleMenu);
}

document.querySelectorAll("#navigation a").forEach((link) => {
link.addEventListener("click", closeMenu);
});

function addMessage(role, text) {
if (!chat) return null;

const div = document.createElement("div");
div.className = "message " + (role === "user" ? "system" : "ai");

if (role === "user") {
div.textContent = "You: " + text;
} else {
div.textContent = text;
}

chat.appendChild(div);
chat.scrollTop = chat.scrollHeight;

return div;
}

async function askAI(question) {
if (!question || !chat) return;

addMessage("user", question);

const loading = addMessage("assistant", "Azimi AI is thinking...");

if (input) input.disabled = true;
if (submitButton) submitButton.disabled = true;

try {
const response = await fetch("/api/chat", {
method: "POST",
headers: {
"Content-Type": "application/json"
},
body: JSON.stringify({
message: question,
history: aiHistory
})
});

const data = await response.json();

if (!response.ok) {
  throw new Error(data.error || "AI request failed");
}

const reply =
  data.reply ||
  data.answer ||
  "Azimi AI could not generate a response.";

if (loading) {
  loading.textContent = reply;
}

aiHistory.push(
  {
    role: "user",
    content: question
  },
  {
    role: "assistant",
    content: reply
  }
);

if (aiHistory.length > 12) {
  aiHistory.splice(0, aiHistory.length - 12);
}

} catch (error) {
console.error("Azimi AI error:", error);

if (loading) {
  loading.textContent =
    "Azimi AI could not connect right now. Please try again.";
}

} finally {
if (input) {
input.disabled = false;
input.focus();
}

if (submitButton) {
  submitButton.disabled = false;
}

}
}

document.querySelectorAll(".ai-modules button").forEach((button) => {
button.addEventListener("click", () => {
const question = button.dataset.question;

if (question && input) {
  input.value = question;
  askAI(question);
  input.value = "";
}

});
});

if (form && input) {
form.addEventListener("submit", (event) => {
event.preventDefault();

const question = input.value.trim();

if (!question) return;

input.value = "";
askAI(question);

});
}
