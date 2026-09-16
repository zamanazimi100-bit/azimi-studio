(() => {
  "use strict";

  // ------------------------------------------------------------
  // AZIMI AI CORE — FINAL V1 FRONTEND
  // ------------------------------------------------------------

  const nav = document.getElementById("navigation");
  const menuButton = document.querySelector(".menu-button");
  const chat = document.getElementById("chat");
  const form = document.getElementById("ai-form");
  const input = document.getElementById("ai-input");
  const submitButton = form ? form.querySelector("button") : null;

  const API_ENDPOINT = "/api/chat";
  const MAX_MESSAGE_LENGTH = 12000;
  const MAX_HISTORY_MESSAGES = 12;

  const aiHistory = [];
  let isProcessing = false;

  // ------------------------------------------------------------
  // NAVIGATION
  // ------------------------------------------------------------

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

  // ------------------------------------------------------------
  // CHAT DISPLAY
  // ------------------------------------------------------------

  function addMessage(role, text) {
    if (!chat) return null;

    const div = document.createElement("div");

    // Keep existing CSS compatibility.
    div.className =
      "message " + (role === "user" ? "system" : "ai");

    // textContent prevents injected HTML from being rendered.
    if (role === "user") {
      div.textContent = "You: " + text;
    } else {
      div.textContent = text;
    }

    chat.appendChild(div);
    chat.scrollTop = chat.scrollHeight;

    return div;
  }

  // ------------------------------------------------------------
  // HISTORY CONTROL
  // ------------------------------------------------------------

  function trimHistory() {
    if (aiHistory.length > MAX_HISTORY_MESSAGES) {
      aiHistory.splice(
        0,
        aiHistory.length - MAX_HISTORY_MESSAGES
      );
    }
  }

  // ------------------------------------------------------------
  // PROCESSING STATE
  // ------------------------------------------------------------

  function setProcessing(processing) {
    isProcessing = processing;

    if (input) {
      input.disabled = processing;
    }

    if (submitButton) {
      submitButton.disabled = processing;
    }
  }

  // ------------------------------------------------------------
  // AZIMI AI REQUEST
  // ------------------------------------------------------------

  async function askAI(question) {
    if (!chat || isProcessing) return;

    const cleanQuestion = String(question || "").trim();

    if (!cleanQuestion) return;

    if (cleanQuestion.length > MAX_MESSAGE_LENGTH) {
      addMessage(
        "assistant",
        "Your message is too long. Please send a shorter request."
      );
      return;
    }

    addMessage("user", cleanQuestion);

    const loading = addMessage(
      "assistant",
      "Azimi AI is thinking..."
    );

    setProcessing(true);

    try {
      const response = await fetch(API_ENDPOINT, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: cleanQuestion,
          history: aiHistory,
        }),
      });

      let data = null;

      try {
        data = await response.json();
      } catch {
        throw new Error(
          "The AI server returned an invalid response."
        );
      }

      if (!response.ok) {
        throw new Error(
          data?.error || "AI request failed."
        );
      }

      const reply =
        typeof data?.reply === "string"
          ? data.reply.trim()
          : typeof data?.answer === "string"
            ? data.answer.trim()
            : "";

      if (!reply) {
        throw new Error(
          "Azimi AI returned no readable response."
        );
      }

      if (loading) {
        loading.textContent = reply;
      }

      // Save only normal conversation content.
      aiHistory.push(
        {
          role: "user",
          content: cleanQuestion,
        },
        {
          role: "assistant",
          content: reply,
        }
      );

      trimHistory();

      if (chat) {
        chat.scrollTop = chat.scrollHeight;
      }
    } catch (error) {
      console.error("AZIMI AI error:", error);

      if (loading) {
        loading.textContent =
          "Azimi AI could not connect right now. Please try again.";
      }
    } finally {
      setProcessing(false);

      if (input) {
        input.focus();
      }
    }
  }

  // ------------------------------------------------------------
  // AI MODULE BUTTONS
  // ------------------------------------------------------------

  document
    .querySelectorAll(".ai-modules button")
    .forEach((button) => {
      button.addEventListener("click", () => {
        const question = button.dataset.question;

        if (!question || !input || isProcessing) {
          return;
        }

        input.value = question;
        askAI(question);
        input.value = "";
      });
    });

  // ------------------------------------------------------------
  // CHAT FORM
  // ------------------------------------------------------------

  if (form && input) {
    form.addEventListener("submit", (event) => {
      event.preventDefault();

      if (isProcessing) return;

      const question = input.value.trim();

      if (!question) return;

      input.value = "";

      askAI(question);
    });

    // Enter = send
    // Shift + Enter = new line
    input.addEventListener("keydown", (event) => {
      if (
        event.key === "Enter" &&
        !event.shiftKey
      ) {
        event.preventDefault();

        if (isProcessing) return;

        const question = input.value.trim();

        if (!question) return;

        input.value = "";

        askAI(question);
      }
    });
  }
})();
