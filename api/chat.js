import { supabaseAdmin } from "../lib/supabase-server";

const ORCHESTRATOR_URL =
  "https://azimi-studio-unique-vercel-coral.vercel.app/api/azimi-orchestrator";

const MAX_MESSAGE_LENGTH = 12000;
const MAX_HISTORY_ITEMS = 12;
const MAX_MEMORY_ITEMS = 50;
const MAX_CONTEXT_LENGTH = 30000;
const MAX_ITEM_LENGTH = 4000;

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

function sanitizeHistory(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .slice(-MAX_HISTORY_ITEMS)
    .map((item) => {

      if (
        !item ||
        typeof item !== "object"
      ) {
        return null;
      }

      const role =
        typeof item.role === "string"
          ? item.role.trim().toLowerCase()
          : "";

      const content =
        typeof item.content === "string"
          ? item.content.trim().slice(
              0,
              MAX_ITEM_LENGTH
            )
          : "";

      if (
        role !== "user" &&
        role !== "assistant"
      ) {
        return null;
      }

      if (!content) {
        return null;
      }

      if (secretDetected(content)) {
        return null;
      }

      return {
        role,
        content,
      };
    })
    .filter(Boolean);
}

function sanitizeMemory(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .slice(-MAX_MEMORY_ITEMS)
    .map((item) => {

      if (
        !item ||
        typeof item !== "object"
      ) {
        return null;
      }

      const role =
        typeof item.role === "string"
          ? item.role.trim().toLowerCase()
          : "";

      const content =
        typeof item.content === "string"
          ? item.content.trim().slice(
              0,
              MAX_ITEM_LENGTH
            )
          : "";

      if (
        role !== "user" &&
        role !== "assistant" &&
        role !== "system"
      ) {
        return null;
      }

      if (!content) {
        return null;
      }

      if (secretDetected(content)) {
        return null;
      }

      return {
        role,
        content,
      };
    })
    .filter(Boolean);
}

function buildSafeContext(
  history,
  memory
) {
  const sections = [];

  if (history.length > 0) {
    sections.push(
      "SAFE CONVERSATION HISTORY:\n" +
        history
          .map(
            (item) =>
              `${item.role.toUpperCase()}: ${item.content}`
          )
          .join("\n")
    );
  }

  if (memory.length > 0) {
    sections.push(
      "APPROVED ATLAS MEMORY:\n" +
        memory
          .map(
            (item) =>
              `${item.role.toUpperCase()}: ${item.content}`
          )
          .join("\n")
    );
  }

  const context =
    sections.join(
      "\n\n"
    );

  return context
    .slice(
      0,
      MAX_CONTEXT_LENGTH
    );
}

async function callAtlasCore(
  message,
  context
) {
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
              "authenticated-v2",

            "X-AZIMI-ATLAS-SECRET":
              process.env
                .ATLAS_INTERNAL_SECRET || "",
          },

          body: JSON.stringify({
            message,

            context,

            atlasRequest: {
              version: "2.0",

              authenticated: true,

              memoryBoundary:
                "APPROVED_ATLAS_MEMORY_ONLY",

              vaultAccess:
                false,

              conversationHistory:
                context.includes(
                  "SAFE CONVERSATION HISTORY:"
                ),
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
        "2.0.0",

      fallback:
        data?.fallback === true,

      memoryUsed:
        data?.memoryUsed === true,

      contextUsed:
        data?.contextUsed === true,
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

  if (
    req.method !== "POST"
  ) {
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
     * 1. AUTHENTICATION
     * ---------------------------------------------------
     */

    const authentication =
      await authenticateUser(req);

    if (
      !authentication.ok
    ) {
      return res.status(
        authentication.status
      ).json({
        error:
          authentication.error,
      });
    }

    /*
     * ---------------------------------------------------
     * 2. CURRENT MESSAGE
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

    if (
      secretDetected(message)
    ) {
      return rejectProtectedRequest(
        res
      );
    }

    /*
     * ---------------------------------------------------
     * 3. SAFE CONTEXT
     * ---------------------------------------------------
     *
     * Android Guardian sends already-filtered history
     * and approved memory.
     *
     * The server filters them again.
     *
     * This is defense in depth.
     */

    const history =
      sanitizeHistory(
        req.body?.history
      );

    const memory =
      sanitizeMemory(
        req.body?.memory
      );

    const suppliedContext =
      typeof req.body?.context === "string"
        ? req.body.context
            .trim()
            .slice(
              0,
              MAX_CONTEXT_LENGTH
            )
        : "";

    if (
      suppliedContext &&
      secretDetected(
        suppliedContext
      )
    ) {
      return rejectProtectedRequest(
        res
      );
    }

    const generatedContext =
      buildSafeContext(
        history,
        memory
      );

    /*
     * Combine the locally generated safe context with
     * Atlas context supplied by the trusted /api gateway.
     *
     * Everything is still credential-filtered.
     */

    const contextParts = [];

    if (suppliedContext) {
      contextParts.push(
        "ATLAS CORE SAFE CONTEXT:\n" +
          suppliedContext
      );
    }

    if (generatedContext) {
      contextParts.push(
        generatedContext
      );
    }

    const context =
      contextParts
        .join("\n\n")
        .slice(
          0,
          MAX_CONTEXT_LENGTH
        );

    /*
     * ---------------------------------------------------
     * 4. ONLINE ATLAS EXECUTION
     * ---------------------------------------------------
     */

    const result =
      await callAtlasCore(
        message,
        context
      );

    if (
      !result.ok
    ) {

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

        contextUsed:
          false,

        vaultAccess:
          false,
      });
    }

    /*
     * ---------------------------------------------------
     * 5. RESPONSE
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
        context.length > 0 ||
        result.contextUsed === true,

      memoryUsed:
        memory.length > 0 ||
        result.memoryUsed === true,

      authenticated:
        true,

      vaultAccess:
        false,

      memoryBoundary:
        "APPROVED_ATLAS_MEMORY_ONLY",

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

      contextUsed:
        false,

      vaultAccess:
        false,
    });
  }
}
