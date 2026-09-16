(() => {
  "use strict";

  // ------------------------------------------------------------
  // AZIMI AI CORE — V1+ MEMORY CORE
  // Phone-first, privacy-first frontend
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
  const MAX_MEMORY_ITEMS = 50;
  const MAX_MEMORY_LENGTH = 1000;

  const MEMORY_STORAGE_KEY = "azimi_ai_memory_v1";

  let isProcessing = false;

  let aiHistory = [];
  let memory = loadMemory();

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

    // Preserve existing CSS compatibility.
    div.className =
      "message " + (role === "user" ? "system" : "ai");

    // Never inject AI/user text as HTML.
    div.textContent =
      role === "user"
        ? "You: " + text
        : text;

    chat.appendChild(div);
    chat.scrollTop = chat.scrollHeight;

    return div;
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
  // HISTORY
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
  // MEMORY SECURITY
  // ------------------------------------------------------------

  function looksLikeSecret(text) {
    const patterns = [
      /sk-[A-Za-z0-9_-]{20,}/i,
      /api[_ -]?key\s*[:=]/i,
      /secret\s*[:=]/i,
      /password\s*[:=]/i,
      /passwd\s*[:=]/i,
      /token\s*[:=]/i,
      /access[_ -]?token\s*[:=]/i,
      /refresh[_ -]?token\s*[:=]/i,
      /authorization\s*[:=]/i,
      /bearer\s+[A-Za-z0-9._-]+/i,
      /mfa\s*(code|token)?\s*[:=]/i,
      /verification\s*(code|token)?\s*[:=]/i,
      /recovery\s*(code|codes|key)\s*[:=]/i,
      /private[_ -]?key\s*[:=]/i,
      /-----BEGIN .*PRIVATE KEY-----/i
    ];

    return patterns.some((pattern) => pattern.test(text));
  }

  // ------------------------------------------------------------
  // MEMORY LOAD
  // ------------------------------------------------------------

  function loadMemory() {
    try {
      const stored =
        localStorage.getItem(MEMORY_STORAGE_KEY);

      if (!stored) return [];

      const parsed = JSON.parse(stored);

      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed
        .filter(
          (item) =>
            item &&
            typeof item.text === "string" &&
            item.text.trim()
        )
        .slice(0, MAX_MEMORY_ITEMS);
    } catch (error) {
      console.warn(
        "AZIMI MEMORY CORE could not load memory:",
        error
      );

      return [];
    }
  }

  // ------------------------------------------------------------
  // MEMORY SAVE
  // ------------------------------------------------------------

  function saveMemory() {
    try {
      localStorage.setItem(
        MEMORY_STORAGE_KEY,
        JSON.stringify(memory)
      );

      return true;
    } catch (error) {
      console.error(
        "AZIMI MEMORY CORE could not save memory:",
        error
      );

      return false;
    }
  }

  // ------------------------------------------------------------
  // ADD MEMORY
  // ------------------------------------------------------------

  function remember(text) {
    const clean = String(text || "").trim();

    if (!clean) {
      return {
        ok: false,
        message: "There is nothing to remember."
      };
    }

    if (clean.length > MAX_MEMORY_LENGTH) {
      return {
        ok: false,
        message:
          "That memory is too long. Keep it under 1,000 characters."
      };
    }

    if (looksLikeSecret(clean)) {
      return {
        ok: false,
        message:
          "I won't store secrets such as passwords, API keys, tokens, MFA codes, verification codes, or recovery codes."
      };
    }

    const duplicate = memory.some(
      (item) =>
        item.text.toLowerCase() === clean.toLowerCase()
    );

    if (duplicate) {
      return {
        ok: false,
        message: "That memory is already saved."
      };
    }

    memory.unshift({
      id:
        Date.now().toString(36) +
        Math.random().toString(36).slice(2, 8),
      text: clean,
      createdAt: new Date().toISOString()
    });

    if (memory.length > MAX_MEMORY_ITEMS) {
      memory = memory.slice(0, MAX_MEMORY_ITEMS);
    }

    saveMemory();

    return {
      ok: true,
      message: "Memory saved locally on this device."
    };
  }

  // ------------------------------------------------------------
  // FORGET MEMORY
  // ------------------------------------------------------------

  function forgetMemory(searchText) {
    const clean = String(searchText || "")
      .trim()
      .toLowerCase();

    if (!clean) {
      return {
        ok: false,
        message: "Tell me which memory to forget."
      };
    }

    const before = memory.length;

    memory = memory.filter(
      (item) =>
        !item.text.toLowerCase().includes(clean)
    );

    const removed = before - memory.length;

    saveMemory();

    if (!removed) {
      return {
        ok: false,
        message: "No matching memory was found."
      };
    }

    return {
      ok: true,
      message:
        removed === 1
          ? "Memory removed."
          : `${removed} matching memories removed.`
    };
  }

  // ------------------------------------------------------------
  // CLEAR ALL LOCAL MEMORY
  // ------------------------------------------------------------

  function clearAllMemory() {
    memory = [];

    try {
      localStorage.removeItem(MEMORY_STORAGE_KEY);
    } catch (error) {
      console.error(
        "AZIMI MEMORY CORE could not clear memory:",
        error
      );
    }
  }

  // ------------------------------------------------------------
  // MEMORY SUMMARY
  // ------------------------------------------------------------

  function memorySummary() {
    if (!memory.length) {
      return "AZIMI MEMORY CORE is empty.";
    }

    const lines = memory.map(
      (item, index) =>
        `${index + 1}. ${item.text}`
    );

    return (
      "AZIMI MEMORY CORE — LOCAL MEMORIES\n\n" +
      lines.join("\n")
    );
  }

  // ------------------------------------------------------------
  // MEMORY COMMANDS
  // ------------------------------------------------------------

  function handleMemoryCommand(question) {
    const clean = question.trim();

    const rememberMatch =
      clean.match(/^\/remember\s+(.+)/i);

    if (rememberMatch) {
      const result = remember(
        rememberMatch[1]
      );

      addMessage(
        "assistant",
        result.message
      );

      return true;
    }

    const forgetMatch =
      clean.match(/^\/forget\s+(.+)/i);

    if (forgetMatch) {
      const result = forgetMemory(
        forgetMatch[1]
      );

      addMessage(
        "assistant",
        result.message
      );

      return true;
    }

    if (/^\/memory$/i.test(clean)) {
      addMessage(
        "assistant",
        memorySummary()
      );

      return true;
    }

    if (/^\/clear-memory$/i.test(clean)) {
      clearAllMemory();

      addMessage(
        "assistant",
        "All AZIMI AI local memories have been cleared from this browser."
      );

      return true;
    }

    return false;
  }

  // ------------------------------------------------------------
  // MEMORY CONTEXT
  // ------------------------------------------------------------

  function getMemoryContext() {
    if (!memory.length) {
      return [];
    }

    return memory.map((item) => ({
      text: item.text
    }));
  }

  // ------------------------------------------------------------
  // AZIMI AI REQUEST
  // ------------------------------------------------------------

  async function askAI(question) {
    if (!chat || isProcessing) return;

    const cleanQuestion =
      String(question || "").trim();

    if (!cleanQuestion) return;

    if (
      cleanQuestion.length >
      MAX_MESSAGE_LENGTH
    ) {
      addMessage(
        "assistant",
        "Your message is too long. Please send a shorter request."
      );

      return;
    }

    // Local memory commands do not need an AI request.
    if (
      handleMemoryCommand(cleanQuestion)
    ) {
      if (input) {
        input.focus();
      }

      return;
    }

    addMessage(
      "user",
      cleanQuestion
    );

    const loading = addMessage(
      "assistant",
      "Azimi AI is thinking..."
    );

    setProcessing(true);

    try {
      const response = await fetch(
        API_ENDPOINT,
        {
          method: "POST",
          headers: {
            "Content-Type":
              "application/json"
          },
          body: JSON.stringify({
            message: cleanQuestion,
            history: aiHistory,
            memory: getMemoryContext()
          })
        }
      );

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
          data?.error ||
            "AI request failed."
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

      aiHistory.push(
        {
          role: "user",
          content: cleanQuestion
        },
        {
          role: "assistant",
          content: reply
        }
      );

      trimHistory();

      if (chat) {
        chat.scrollTop =
          chat.scrollHeight;
      }
    } catch (error) {
      console.error(
        "AZIMI AI error:",
        error
      );

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
    .querySelectorAll(
      ".ai-modules button"
    )
    .forEach((button) => {
      button.addEventListener(
        "click",
        () => {
          const question =
            button.dataset.question;

          if (
            !question ||
            !input ||
            isProcessing
          ) {
            return;
          }

          input.value = question;

          askAI(question);

          input.value = "";
        }
      );
    });

  // ------------------------------------------------------------
  // CHAT FORM
  // ------------------------------------------------------------

  if (form && input) {
    form.addEventListener(
      "submit",
      (event) => {
        event.preventDefault();

        if (isProcessing) return;

        const question =
          input.value.trim();

        if (!question) return;

        input.value = "";

        askAI(question);
      }
    );

    // Enter = send
    // Shift + Enter = new line
    input.addEventListener(
      "keydown",
      (event) => {
        if (
          event.key === "Enter" &&
          !event.shiftKey
        ) {
          event.preventDefault();

          if (isProcessing) return;

          const question =
            input.value.trim();

          if (!question) return;

          input.value = "";

          askAI(question);
        }
      }
    );
  }

  // ------------------------------------------------------------
  // MEMORY COMMAND HELP
  // ------------------------------------------------------------

  window.AZIMI_MEMORY = {
    remember,
    forget: forgetMemory,
    list: memorySummary,
    clear: clearAllMemory
  };

})();
