const nav = document.getElementById("navigation");
const menu = document.querySelector(".menu-button");
const chat = document.getElementById("chat");
const form = document.getElementById("ai-form");
const input = document.getElementById("ai-input");

function toggleMenu() {
  if (nav) nav.classList.toggle("open");
}

if (menu) {
  menu.addEventListener("click", toggleMenu);
}

document.querySelectorAll("#navigation a").forEach((link) => {
  link.addEventListener("click", () => {
    if (nav) nav.classList.remove("open");
  });
});

function addMsg(text, type) {
  if (!chat) return;

  const message = document.createElement("div");
  message.className = "message " + type;
  message.textContent = text;

  chat.appendChild(message);
  chat.scrollTop = chat.scrollHeight;
}

async function askAI(question) {
  if (!question) return;

  addMsg(question, "user");
  addMsg("Azimi AI is thinking...", "ai");

  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        message: question
      })
    });

    const data = await response.json();

    const messages = chat ? chat.querySelectorAll(".message.ai") : [];
    const thinkingMessage = messages[messages.length - 1];

    if (!response.ok) {
      throw new Error(data.error || "AI request failed");
    }

    if (thinkingMessage) {
      thinkingMessage.textContent =
        data.reply || "Azimi AI couldn't generate a response.";
    } else {
      addMsg(data.reply || "Azimi AI couldn't generate a response.", "ai");
    }
  } catch (error) {
    const messages = chat ? chat.querySelectorAll(".message.ai") : [];
    const thinkingMessage = messages[messages.length - 1];

    const errorMessage =
      "Azimi AI could not connect right now. Please try again.";

    if (thinkingMessage) {
      thinkingMessage.textContent = errorMessage;
    } else {
      addMsg(errorMessage, "ai");
    }

    console.error("Azimi AI error:", error);
  }
}

if (form && input) {
  form.addEventListener("submit", (event) => {
    event.preventDefault();

    const question = input.value.trim();

    if (!question) return;

    input.value = "";
    askAI(question);
  });
}
