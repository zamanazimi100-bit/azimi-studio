(() => {
  "use strict";

  // ------------------------------------------------------------
  // AZIMI AI CORE — V1+ AUTHENTICATED MEMORY CORE
  // Phone-first, privacy-first frontend
  // ------------------------------------------------------------

  const nav = document.getElementById("navigation");
  const menuButton = document.querySelector(".menu-button");

  const chat = document.getElementById("chat");
  const form = document.getElementById("ai-form");
  const input = document.getElementById("ai-input");
  const submitButton = form ? form.querySelector("button") : null;

  const loginButton =
    document.getElementById("azimi-login-button");

  const logoutButton =
    document.getElementById("azimi-logout-button");

  const authMessage =
    document.getElementById("azimi-auth-message");

  const authStatus =
    document.getElementById("azimi-ai-status");

  const API_ENDPOINT = "/api/chat";
  const AUTH_CONFIG_ENDPOINT = "/api/auth-config";

  const MAX_MESSAGE_LENGTH = 12000;
  const MAX_HISTORY_MESSAGES = 12;

  const MAX_MEMORY_ITEMS = 50;
  const MAX_MEMORY_LENGTH = 1000;

  const MEMORY_STORAGE_KEY = "azimi_ai_memory_v1";
  const SESSION_STORAGE_KEY = "azimi_supabase_session_v1";

  let isProcessing = false;
  let aiHistory = [];

  let memory = loadMemory();

  let supabaseConfig = null;
  let session = loadSession();

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

  document
    .querySelectorAll("#navigation a")
    .forEach((link) => {
      link.addEventListener("click", closeMenu);
    });

  // ------------------------------------------------------------
  // CHAT DISPLAY
  // ------------------------------------------------------------

  function addMessage(role, text) {
    if (!chat) return null;

    const div = document.createElement("div");

    div.className =
      "message " +
      (role === "user" ? "system" : "ai");

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
      input.disabled =
        processing || !session;
    }

    if (submitButton) {
      submitButton.disabled =
        processing || !session;
    }
  }

  // ------------------------------------------------------------
  // AUTH UI
  // ------------------------------------------------------------

  function updateAuthUI() {
    const authenticated = Boolean(session?.access_token);

    if (authStatus) {
      authStatus.textContent = authenticated
        ? "AUTHENTICATED"
        : "AUTHENTICATION REQUIRED";
    }

    if (loginButton) {
      loginButton.hidden = authenticated;
    }

    if (logoutButton) {
      logoutButton.hidden = !authenticated;
    }

    if (input) {
      input.disabled =
        !authenticated || isProcessing;
    }

    if (submitButton) {
      submitButton.disabled =
        !authenticated || isProcessing;
    }

    if (authMessage) {
      authMessage.textContent = authenticated
        ? "AZIMI AI is connected securely."
        : "Sign in with your email to use AZIMI AI.";
    }
  }

  // ------------------------------------------------------------
  // AUTH SESSION STORAGE
  // ------------------------------------------------------------

  function loadSession() {
    try {
      const stored =
        localStorage.getItem(
  SESSION_STORAGE_KEY
);

      if (!stored) return null;

      const parsed = JSON.parse(stored);

      if (
        !parsed ||
        typeof parsed.access_token !== "string"
      ) {
        return null;
      }

      return parsed;
    } catch (error) {
      console.warn(
        "AZIMI AUTH session could not load:",
        error
      );

      return null;
    }
  }

  function saveSession(nextSession) {
    session = nextSession;

    try {
      if (nextSession) {
        localStorage.setItem(
  SESSION_STORAGE_KEY,
          JSON.stringify(nextSession)
        );
      } else {
        localStorage.removeItem(
  SESSION_STORAGE_KEY
        );
      }
    } catch (error) {
      console.error(
        "AZIMI AUTH session could not save:",
        error
      );
    }

    updateAuthUI();
  }

  // ------------------------------------------------------------
  // SUPABASE CONFIG
  // ------------------------------------------------------------

  async function loadSupabaseConfig() {
    if (supabaseConfig) {
      return supabaseConfig;
    }

    const response = await fetch(
      AUTH_CONFIG_ENDPOINT,
      {
        method: "GET",
        headers: {
          Accept: "application/json"
        }
      }
    );

    if (!response.ok) {
      throw new Error(
        "AZIMI authentication configuration could not be loaded."
      );
    }

    const data = await response.json();

    if (
      typeof data?.supabaseUrl !== "string" ||
      typeof data?.supabasePublishableKey !== "string"
    ) {
      throw new Error(
        "AZIMI authentication configuration is incomplete."
      );
    }

    supabaseConfig = {
      url: data.supabaseUrl,
      key: data.supabasePublishableKey
    };

    return supabaseConfig;
  }

  // ------------------------------------------------------------
  // SUPABASE AUTH REQUEST
  // ------------------------------------------------------------

  async function supabaseAuthRequest(
    path,
    options = {}
  ) {
    const config =
      await loadSupabaseConfig();

    const response = await fetch(
      config.url + "/auth/v1/" + path,
      {
        ...options,
        headers: {
          apikey: config.key,
          "Content-Type":
            "application/json",
          ...(options.headers || {})
        }
      }
    );

    let data = null;

    try {
      data = await response.json();
    } catch {
      data = null;
    }

    if (!response.ok) {
      throw new Error(
        data?.msg ||
          data?.message ||
          data?.error_description ||
          data?.error ||
          "Supabase authentication request failed."
      );
    }

    return data;
  }

  // ------------------------------------------------------------
  // MAGIC-LINK LOGIN
  // ------------------------------------------------------------

  async function requestLogin() {
    if (!loginButton) return;

    const email = window.prompt(
      "Enter your email address to receive an AZIMI AI sign-in link:"
    );

    if (!email) {
      return;
    }

    const cleanEmail =
      email.trim().toLowerCase();

    if (
      !cleanEmail ||
      !cleanEmail.includes("@") ||
      cleanEmail.length > 320
    ) {
      if (authMessage) {
        authMessage.textContent =
          "Please enter a valid email address.";
      }

      return;
    }

    loginButton.disabled = true;

    if (authMessage) {
      authMessage.textContent =
        "Sending your secure sign-in link...";
    }

    try {
      const redirectTo =
        window.location.origin +
        window.location.pathname;

      await supabaseAuthRequest(
        "otp",
        {
          method: "POST",
          body: JSON.stringify({
            email: cleanEmail,
            create_user: true,
            options: {
              email_redirect_to: redirectTo
            }
          })
        }
      );

      if (authMessage) {
        authMessage.textContent =
          "Check your email for the AZIMI AI sign-in link.";
      }
    } catch (error) {
      console.error(
        "AZIMI login error:",
        error
      );

      if (authMessage) {
        authMessage.textContent =
  `Sign-in could not be started: ${error.message || "Unknown error"}`;
      }
    } finally {
      loginButton.disabled = false;
    }
  }

  // ------------------------------------------------------------
  // AUTH CALLBACK
  // ------------------------------------------------------------

  function readAuthCallback() {
    const hash =
      window.location.hash;

    if (!hash || hash.length < 2) {
      return false;
    }

    const params =
      new URLSearchParams(
        hash.substring(1)
      );

    const accessToken =
      params.get("access_token");

    const refreshToken =
      params.get("refresh_token");

    if (!accessToken) {
      return false;
    }

    const expiresIn =
      Number(
        params.get("expires_in") || 3600
      );

    const expiresAt =
      Math.floor(Date.now() / 1000) +
      expiresIn;

    saveSession({
      access_token: accessToken,
      refresh_token:
        refreshToken || "",
      expires_at: expiresAt,
      token_type:
        params.get("token_type") || "bearer"
    });

    window.history.replaceState(
      {},
      document.title,
      window.location.pathname +
        window.location.search
    );

    if (authMessage) {
      authMessage.textContent =
        "AZIMI AI authentication successful.";
    }

    return true;
  }

  // ------------------------------------------------------------
  // REFRESH SESSION
  // ------------------------------------------------------------

  async function refreshSessionIfNeeded() {
    if (!session?.access_token) {
      return false;
    }

    const expiresAt =
      Number(session.expires_at || 0);

    const now =
      Math.floor(Date.now() / 1000);

    if (
      expiresAt &&
      expiresAt - now > 60
    ) {
      return true;
    }

    if (!session.refresh_token) {
      saveSession(null);
      return false;
    }

    try {
      const data =
        await supabaseAuthRequest(
          "token?grant_type=refresh_token",
          {
            method: "POST",
            body: JSON.stringify({
              refresh_token:
                session.refresh_token
            })
          }
        );

      if (!data?.access_token) {
        throw new Error(
          "No refreshed access token was returned."
        );
      }

      saveSession({
        access_token:
          data.access_token,
        refresh_token:
          data.refresh_token ||
          session.refresh_token,
        expires_at:
          Math.floor(Date.now() / 1000) +
          Number(data.expires_in || 3600),
        token_type:
          data.token_type || "bearer"
      });

      return true;
    } catch (error) {
      console.error(
        "AZIMI session refresh failed:",
        error
      );

      saveSession(null);

      return false;
    }
  }

  // ------------------------------------------------------------
  // LOGOUT
  // ------------------------------------------------------------

  async function logout() {
    const currentToken =
      session?.access_token;

    try {
      if (currentToken) {
        const config =
          await loadSupabaseConfig();

        await fetch(
          config.url + "/auth/v1/logout",
          {
            method: "POST",
            headers: {
              apikey: config.key,
              Authorization:
                "Bearer " + currentToken
            }
          }
        );
      }
    } catch (error) {
      console.warn(
        "AZIMI logout request failed:",
        error
      );
    }

    saveSession(null);

    aiHistory = [];

    if (authMessage) {
      authMessage.textContent =
        "You have been signed out of AZIMI AI.";
    }
  }

  // ------------------------------------------------------------
  // AUTH BUTTONS
  // ------------------------------------------------------------

  if (loginButton) {
    loginButton.addEventListener(
      "click",
      requestLogin
    );
  }

  if (logoutButton) {
    logoutButton.addEventListener(
      "click",
      logout
    );
  }

  // ------------------------------------------------------------
  // HISTORY
  // ------------------------------------------------------------

  function trimHistory() {
    if (
      aiHistory.length >
      MAX_HISTORY_MESSAGES
    ) {
      aiHistory.splice(
        0,
        aiHistory.length -
          MAX_HISTORY_MESSAGES
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

    return patterns.some(
      (pattern) => pattern.test(text)
    );
  }

  // ------------------------------------------------------------
  // MEMORY LOAD
  // ------------------------------------------------------------

  function loadMemory() {
    try {
      const stored =
        localStorage.getItem(
          MEMORY_STORAGE_KEY
        );

      if (!stored) return [];

      const parsed =
        JSON.parse(stored);

      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed
        .filter(
          (item) =>
            item &&
            typeof item.text ===
              "string" &&
            item.text.trim()
        )
        .slice(
          0,
          MAX_MEMORY_ITEMS
        );
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
    const clean =
      String(text || "").trim();

    if (!clean) {
      return {
        ok: false,
        message:
          "There is nothing to remember."
      };
    }

    if (
      clean.length >
      MAX_MEMORY_LENGTH
    ) {
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

    const duplicate =
      memory.some(
        (item) =>
          item.text.toLowerCase() ===
          clean.toLowerCase()
      );

    if (duplicate) {
      return {
        ok: false,
        message:
          "That memory is already saved."
      };
    }

    memory.unshift({
      id:
        Date.now().toString(36) +
        Math.random()
          .toString(36)
          .slice(2, 8),
      text: clean,
      createdAt:
        new Date().toISOString()
    });

    if (
      memory.length >
      MAX_MEMORY_ITEMS
    ) {
      memory =
        memory.slice(
          0,
          MAX_MEMORY_ITEMS
        );
    }

    saveMemory();

    return {
      ok: true,
      message:
        "Memory saved locally on this device."
    };
  }

  // ------------------------------------------------------------
  // FORGET MEMORY
  // ------------------------------------------------------------

  function forgetMemory(
    searchText
  ) {
    const clean =
      String(searchText || "")
        .trim()
        .toLowerCase();

    if (!clean) {
      return {
        ok: false,
        message:
          "Tell me which memory to forget."
      };
    }

    const before =
      memory.length;

    memory =
      memory.filter(
        (item) =>
          !item.text
            .toLowerCase()
            .includes(clean)
      );

    const removed =
      before - memory.length;

    saveMemory();

    if (!removed) {
      return {
        ok: false,
        message:
          "No matching memory was found."
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
      localStorage.removeItem(
        MEMORY_STORAGE_KEY
      );
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

    const lines =
      memory.map(
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

  function handleMemoryCommand(
    question
  ) {
    const clean =
      question.trim();

    const rememberMatch =
      clean.match(
        /^\/remember\s+(.+)/i
      );

    if (rememberMatch) {
      const result =
        remember(
          rememberMatch[1]
        );

      addMessage(
        "assistant",
        result.message
      );

      return true;
    }

    const forgetMatch =
      clean.match(
        /^\/forget\s+(.+)/i
      );

    if (forgetMatch) {
      const result =
        forgetMemory(
          forgetMatch[1]
        );

      addMessage(
        "assistant",
        result.message
      );

      return true;
    }

    if (
      /^\/memory$/i.test(
        clean
      )
    ) {
      addMessage(
        "assistant",
        memorySummary()
      );

      return true;
    }

    if (
      /^\/clear-memory$/i.test(
        clean
      )
    ) {
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

    return memory.map(
      (item) => ({
        text: item.text
      })
    );
  }

  // ------------------------------------------------------------
  // AZIMI AI REQUEST
  // ------------------------------------------------------------

  async function askAI(question) {
    if (!chat || isProcessing) {
      return;
    }

    const authenticated =
      await refreshSessionIfNeeded();

    if (!authenticated) {
      addMessage(
        "assistant",
        "Please sign in to AZIMI AI first."
      );

      updateAuthUI();

      return;
    }

    const cleanQuestion =
      String(question || "").trim();

    if (!cleanQuestion) {
      return;
    }

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

    if (
      handleMemoryCommand(
        cleanQuestion
      )
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

    const loading =
      addMessage(
        "assistant",
        "Azimi AI is thinking..."
      );

    setProcessing(true);

    try {
      const response =
        await fetch(
          API_ENDPOINT,
          {
            method: "POST",
            headers: {
              "Content-Type":
                "application/json",
              Authorization:
                "Bearer " +
                session.access_token
            },
            body: JSON.stringify({
              message:
                cleanQuestion,
              history:
                aiHistory,
              memory:
                getMemoryContext()
            })
          }
        );

      let data = null;

      try {
        data =
          await response.json();
      } catch {
        throw new Error(
          "The AI server returned an invalid response."
        );
      }

      if (!response.ok) {
        if (
          response.status ===
          401
        ) {
          saveSession(null);

          throw new Error(
            "Your AZIMI AI session has expired. Please sign in again."
          );
        }

        throw new Error(
          data?.error ||
            "AI request failed."
        );
      }

      const reply =
        typeof data?.reply ===
        "string"
          ? data.reply.trim()
          : typeof data?.answer ===
            "string"
            ? data.answer.trim()
            : "";

      if (!reply) {
        throw new Error(
          "Azimi AI returned no readable response."
        );
      }

      if (loading) {
        loading.textContent =
          reply;
      }

      aiHistory.push(
        {
          role: "user",
          content:
            cleanQuestion
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
          error?.message ||
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
    .forEach(
      (button) => {
        button.addEventListener(
          "click",
          () => {
            const question =
              button.dataset
                .question;

            if (
              !question ||
              !input ||
              isProcessing ||
              !session
            ) {
              return;
            }

            input.value =
              question;

            askAI(question);

            input.value = "";
          }
        );
      }
    );

  // ------------------------------------------------------------
  // CHAT FORM
  // ------------------------------------------------------------

  if (form && input) {
    form.addEventListener(
      "submit",
      (event) => {
        event.preventDefault();

        if (
          isProcessing ||
          !session
        ) {
          return;
        }

        const question =
          input.value.trim();

        if (!question) {
          return;
        }

        input.value = "";

        askAI(question);
      }
    );

    input.addEventListener(
      "keydown",
      (event) => {
        if (
          event.key ===
            "Enter" &&
          !event.shiftKey
        ) {
          event.preventDefault();

          if (
            isProcessing ||
            !session
          ) {
            return;
          }

          const question =
            input.value.trim();

          if (!question) {
            return;
          }

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

  // ------------------------------------------------------------
  // INITIALIZE AZIMI AI
  // ------------------------------------------------------------

  async function initialize() {
    try {
      readAuthCallback();

      await loadSupabaseConfig();

      await refreshSessionIfNeeded();

      updateAuthUI();
    } catch (error) {
      console.error(
        "AZIMI AI initialization error:",
        error
      );

      if (authMessage) {
        authMessage.textContent =
          "AZIMI AI authentication is temporarily unavailable.";
      }

      updateAuthUI();
    }
  }

  initialize();

})();
