// AZIMI AI ORCHESTRATOR
// Provider-agnostic intelligence routing layer.
// Security boundary:
// - Scans the actual user message for secrets.
// - Does NOT scan AZIMI's internal instructions/context.
// - No secrets are forwarded to Cloudflare.
// - No external actions are claimed unless a real tool performs them.

const CLOUDFLARE_ENGINE =
  "https://azimi-ai.zamanazimi100.workers.dev/";

const TIMEOUT_MS = 15000;
const MAX_MESSAGE_LENGTH = 12000;
const MAX_CONTEXT_LENGTH = 30000;

function headers() {
  return {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Referrer-Policy": "no-referrer",
  };
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: headers(),
  });
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

function cleanContext(value) {
  if (typeof value !== "string") {
    return "";
  }

  return value
    .trim()
    .slice(0, MAX_CONTEXT_LENGTH);
}

async function callCloudflare(message, context) {
  const controller = new AbortController();

  const timeout = setTimeout(
    () => controller.abort(),
    TIMEOUT_MS
  );

  try {
    const response = await fetch(
      CLOUDFLARE_ENGINE,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
        },
        body: JSON.stringify({
          message,
          context,
        }),
        signal: controller.signal,
      }
    );

    let data = null;

    try {
      data = await response.json();
    } catch {
      data = null;
    }

    if (!response.ok) {
      return {
        ok: false,
        error: data?.error || `HTTP ${response.status}`,
      };
    }

    const reply =
      typeof data?.reply === "string"
        ? data.reply.trim()
        : "";

    if (!reply) {
      return {
        ok: false,
        error: "Engine returned no readable response",
      };
    }

    return {
      ok: true,
      reply,
      engine:
        data?.engine || "AZIMI-CLOUDFLARE",
      model:
        data?.model || "unknown",
    };
  } catch (error) {
    return {
      ok: false,
      error:
        error?.name === "AbortError"
          ? "Engine timeout"
          : "Engine unavailable",
    };
  } finally {
    clearTimeout(timeout);
  }
}

function localFallback() {
  return {
    ok: true,
    reply:
      "AZIMI AI is in local fallback mode. The primary intelligence engine is currently unavailable, so no external AI response was generated.",
    engine: "AZIMI-LOCAL-FALLBACK",
    model: "deterministic",
  };
}

export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({
      error: "Method not allowed",
    });
  }

  try {
    const message =
      typeof req.body?.message === "string"
        ? req.body.message.trim()
        : "";

    if (!message) {
      return res.status(400).json({
        error: "Message is empty",
      });
    }

    if (message.length > MAX_MESSAGE_LENGTH) {
      return res.status(400).json({
        error: "Message is too long",
      });
    }

    if (secretDetected(message)) {
      return res.status(400).json({
        error:
          "I won't process passwords, API keys, tokens, MFA codes, recovery codes, or private keys.",
      });
    }

    const context = cleanContext(
      req.body?.context
    );

    // ENGINE 1 — Cloudflare Workers AI
    const cloudflare = await callCloudflare(
      message,
      context
    );

    if (cloudflare.ok) {
      return res.status(200).json({
        reply: cloudflare.reply,
        assistant: "AZIMI AI CORE",
        engine: cloudflare.engine,
        model: cloudflare.model,
        fallback: false,
        memoryUsed: Boolean(context),
        authenticated: false,
      });
    }

    // ENGINE 2 — deterministic local fallback
    const fallback = localFallback();

    return res.status(200).json({
      reply: fallback.reply,
      assistant: "AZIMI AI CORE",
      engine: fallback.engine,
      model: fallback.model,
      fallback: true,
      primaryError: cloudflare.error,
      diagnostic: "Cloudflare provider failed",
      memoryUsed: Boolean(context),
      authenticated: false,
    });
  } catch (error) {
    console.error(
      "AZIMI Orchestrator error:",
      error
    );

    return res.status(500).json({
      error: "AZIMI Orchestrator unavailable",
    });
  }
}
