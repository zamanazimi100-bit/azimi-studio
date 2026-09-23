import { supabaseAdmin } from "../lib/supabase-server";

const ORCHESTRATOR_URL =
  "https://azimi-studio-unique-vercel-coral.vercel.app/api/azimi-orchestrator";

const MAX_MESSAGE_LENGTH = 12000;

const MAX_HISTORY_MESSAGES = 12;
const MAX_MEMORY_ITEMS = 50;
const MAX_CONTEXT_LENGTH = 30000;

function safeChatMessages(value, allowSystem = false) {
  if (!Array.isArray(value)) return [];

  return value
    .slice(- (allowSystem ? MAX_MEMORY_ITEMS : MAX_HISTORY_MESSAGES))
    .map((item) => {
      if (!item || typeof item !== "object") return null;

      const role =
        typeof item.role === "string"
          ? item.role.trim().toLowerCase()
          : "";

      const content =
        typeof item.content === "string"
          ? item.content.trim().slice(0, 4000)
          : "";

      const allowedRoles = allowSystem
        ? ["user", "assistant", "system"]
        : ["user", "assistant"];

      if (!allowedRoles.includes(role) || !content) return null;
      if (secretDetected(content)) return null;

      return { role, content };
    })
    .filter(Boolean);
}

function buildApprovedContext(history, memory) {
  const sections = [];

  if (memory.length) {
    sections.push(
      "APPROVED PERSISTENT AZIMI MEMORY:\n" +
      memory
        .map((item, index) => `${index + 1}. [${item.role}] ${item.content}`)
        .join("\n")
    );
  }

  if (history.length) {
    sections.push(
      "RECENT CONVERSATION CONTEXT:\n" +
      history
        .map((item) => `[${item.role}] ${item.content}`)
        .join("\n")
    );
  }

  return sections.join("\n\n").slice(0, MAX_CONTEXT_LENGTH);
}

function secretDetected(value) {
  if (typeof value !== "string") return true;

  return (
    /sk-[A-Za-z0-9_-]{20,}/i.test(value) ||
    /api[_-]?key\s*[:=]/i.test(value) ||
    /password\s*[:=]/i.test(value) ||
    /passwd\s*[:=]/i.test(value) ||
    /access[_-]?token\s*[:=]/i.test(value) ||
    /refresh[_-]?token\s*[:=]/i.test(value) ||
    /authorization\s*[:=]/i.test(value) ||
    /bearer\s+[A-Za-z0-9._-]{20,}/i.test(value) ||
    /verification\s+code/i.test(value) ||
    /mfa\s+code/i.test(value) ||
    /recovery\s+(code|key)/i.test(value) ||
    /private[_-]?key/i.test(value) ||
    /-----BEGIN .*PRIVATE KEY-----/i.test(value)
  );
}

function securityHeaders(res) {
  res.setHeader(
    "Cache-Control",
    "no-store, no-cache, must-revalidate, private"
  );

  res.setHeader(
    "Pragma",
    "no-cache"
  );

  res.setHeader(
    "X-Content-Type-Options",
    "nosniff"
  );

  res.setHeader(
    "X-Frame-Options",
    "DENY"
  );

  res.setHeader(
    "Referrer-Policy",
    "no-referrer"
  );

  res.setHeader(
    "Permissions-Policy",
    "camera=(), microphone=(), geolocation=()"
  );
}

function rejectProtectedRequest(res) {
  return res.status(400).json({
    error:
      "I won't process passwords, API keys, tokens, MFA codes, recovery codes, or private keys.",
  });
}

async function authenticateUser(req) {
  const authorization =
    req.headers.authorization || "";

  if (!authorization.startsWith("Bearer ")) {
    return {
      ok: false,
      status: 401,
      error: "Authentication required",
    };
  }

  const accessToken =
    authorization.slice(7).trim();

  if (!accessToken) {
    return {
      ok: false,
      status: 401,
      error: "Authentication required",
    };
  }

  const {
    data: { user },
    error,
  } =
    await supabaseAdmin.auth.getUser(
      accessToken
    );

  if (error || !user) {
    return {
      ok: false,
      status: 401,
      error:
        "Invalid authentication session",
    };
  }

  return {
    ok: true,
    user,
  };
}

async function callAtlasCore(message, approvedContext) {
  const controller =
    new AbortController();

  const timeout =
    setTimeout(
      () => controller.abort(),
      20000
    );

  try {
    const response =
      await fetch(
        ORCHESTRATOR_URL,
        {
          method: "POST",

          headers: {
            "Content-Type":
              "application/json",

            "Accept":
              "application/json",

            "X-AZIMI-ATLAS-REQUEST":
              "authenticated-v1",

            "X-AZIMI-ATLAS-SECRET":
              process.env
                .ATLAS_INTERNAL_SECRET || "",
          },

          /*
           * IMPORTANT:
           *
           * The current request plus explicitly supplied,
           * security-filtered conversation/memory context
           * crosses the online boundary.
           *
           * Never send:
           * - raw Vault contents
           * - credentials
           * - access tokens
           * - refresh tokens
           * - unsanitized private data
           */
          body: JSON.stringify({
            message,

            context: approvedContext || "",

            atlasRequest: {
              version: "1.3",
              authenticated: true,
              memoryBoundary:
                "GUARDIAN_APPROVED_CONTEXT_ONLY",
              vaultAccess:
                false,
              conversationHistory:
                Boolean(approvedContext),
            },
          }),

          signal:
            controller.signal,
        }
      );

    let data = null;

    try {
      data =
        await response.json();
    } catch {
      data = null;
    }

    if (!response.ok) {
      return {
        ok: false,
        error:
          data?.error ||
          `HTTP ${response.status}`,
      };
    }

    const reply =
      typeof data?.reply === "string"
        ? data.reply.trim()
        : "";

    if (!reply) {
      return {
        ok: false,
        error:
          "ATLAS returned no readable response",
      };
    }

    return {
      ok: true,

      reply,

      engine:
        data?.engine ||
        "AZIMI-CLOUDFLARE",

      model:
        data?.model ||
        "unknown",

      atlasVersion:
        data?.atlasVersion ||
        "1.2.0",

      fallback:
        data?.fallback === true,
    };
  } catch (error) {
    return {
      ok: false,

      error:
        error?.name === "AbortError"
          ? "ATLAS request timed out"
          : "ATLAS service unavailable",
    };
  } finally {
    clearTimeout(timeout);
  }
}

export default async function handler(
  req,
  res
) {
  securityHeaders(res);

  /*
   * ---------------------------------------------------
   * 1. METHOD GATE
   * ---------------------------------------------------
   */

  if (req.method !== "POST") {
    res.setHeader(
      "Allow",
      "POST"
    );

    return res.status(405).json({
      error:
        "Method not allowed",
    });
  }

  try {
    /*
     * ---------------------------------------------------
     * 2. AUTHENTICATION
     * ---------------------------------------------------
     */

    const authentication =
      await authenticateUser(req);

    if (!authentication.ok) {
      return res.status(
        authentication.status
      ).json({
        error:
          authentication.error,
      });
    }

    /*
     * ---------------------------------------------------
     * 3. READ CURRENT USER MESSAGE
     * ---------------------------------------------------
     */

    const message =
      typeof req.body?.message === "string"
        ? req.body.message.trim()
        : "";

    if (!message) {
      return res.status(400).json({
        error:
          "Message is empty",
      });
    }

    if (
      message.length >
      MAX_MESSAGE_LENGTH
    ) {
      return res.status(400).json({
        error:
          "Message is too long",
      });
    }

    /*
     * ---------------------------------------------------
     * 4. PROTECTED-CREDENTIAL GATE
     * ---------------------------------------------------
     */

    if (
      secretDetected(message)
    ) {
      return rejectProtectedRequest(
        res
      );
    }

    /*
     * ---------------------------------------------------
     * 5. SAFE CONVERSATION + APPROVED MEMORY CONTEXT
     * ---------------------------------------------------
     *
     * Guardian is allowed to send only explicitly supplied
     * and security-filtered context. The API never discovers
     * private device data or secrets.
     */

    const safeHistory =
      safeChatMessages(req.body?.history, false);

    const safeMemory =
      safeChatMessages(req.body?.memory, true);

    const approvedContext =
      buildApprovedContext(
        safeHistory,
        safeMemory
      );

    /*
     * ---------------------------------------------------
     * 6. ONLINE ATLAS EXECUTION
     * ---------------------------------------------------
     */

    const result =
      await callAtlasCore(
        message,
        approvedContext
      );

    /*
     * ---------------------------------------------------
     * 7. ATLAS FAILURE
     * ---------------------------------------------------
     */

    if (!result.ok) {
      console.error(
        "ATLAS online engine error:",
        result.error
      );

      return res.status(502).json({
        error:
          "ATLAS AI engine unavailable",

        authenticated:
          true,

        memoryUsed:
          false,

        vaultAccess:
          false,
      });
    }

    /*
     * ---------------------------------------------------
     * 8. RESPONSE
     * ---------------------------------------------------
     */

    return res.status(200).json({
      reply:
        result.reply,

      assistant:
        "ATLAS CORE",

      atlasVersion:
        result.atlasVersion,

      engine:
        result.engine,

      model:
        result.model,

      fallback:
        result.fallback === true,

      contextUsed:
        false,

      memoryUsed:
        false,

      authenticated:
        true,

      vaultAccess:
        false,

      memoryBoundary:
        "GUARDIAN_APPROVED_CONTEXT_ONLY",

      status:
        result.fallback === true
          ? "ATLAS_FALLBACK"
          : "ATLAS_OPERATIONAL",
    });
  } catch (error) {
    console.error(
      "ATLAS chat error:",
      error
    );

    return res.status(500).json({
      error:
        error?.name === "AbortError"
          ? "ATLAS request timed out"
          : "ATLAS service unavailable",

      memoryUsed:
        false,

      vaultAccess:
        false,
    });
  }
}
