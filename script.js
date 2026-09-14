const nav = document.getElementById("navigation");
const menu = document.querySelector(".menu-button");
const chat = document.getElementById("chat");
const form = document.getElementById("ai-form");
const input = document.getElementById("ai-input");

function toggleMenu() {
  nav.classList.toggle("open");
}

document.querySelectorAll("#navigation a").forEach((link) => {
  link.addEventListener("click", () => {
    nav.classList.remove("open");
  });
});

function addMsg(text, type) {
  const message = document.createElement("div");

  message.className = "message " + type;
  message.textContent = text;

  chat.appendChild(message);
  chat.scrollTop = chat.scrollHeight;
}

function safeReply(question) {
  const q = question.toLowerCase();

  if (q.includes("recover") || q.includes("account")) {
    return "SAFE MODE: I can guide an account owner through official recovery steps, hacked-account guidance and security checks. I cannot bypass passwords, verification codes, MFA or ownership checks.";
  }

  if (q.includes("windows") || q.includes("windows 11")) {
    return "WINDOWS 11 COMPANION: I can explain Windows 11 features, Phone Link, updates, security, troubleshooting and compatibility checks.";
  }

  if (q.includes("project") || q.includes("idea")) {
    return "PROJECT BUILDER: Start with one real problem. Define the user, build the smallest useful version, publish it, learn from feedback and improve it. Proof beats promises.";
  }

  if (q.includes("automation")) {
    return "AUTOMATION ASSISTANT: Find a repetitive task, map the steps, identify the inputs and outputs, then automate one small part first.";
  }

  if (q.includes("code") || q.includes("coding")) {
    return "CODE ASSISTANT: Describe what you are trying to build or paste your code. I can explain it, help find problems and suggest cleaner approaches.";
  }

  if (q.includes("ai") || q.includes("artificial intelligence")) {
    return "AZIMI AI: AI can help turn ideas into useful tools, workflows and digital products. This website currently contains a safe demonstration interface. A real AI model can be connected through a secure server-side API.";
  }

  return "AZIMI AI DEMO: I received your request. This interface is ready for a real AI connection through a secure backend/API.";
}

function askAI(question) {
  if (!question) return;

  addMsg(question, "user");

  setTimeout(() => {
    addMsg(safeReply(question), "ai");
  }, 400);
}

form.addEventListener("submit", (event) => {
  event.preventDefault();

  const question = input.value.trim();

  if (!question) return;

  askAI(question);

  input.value = "";
});
