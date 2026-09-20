// AZIMI AI — CLOUDFLARE FREE ENGINE
// Stateless intelligence node.
// No private memory. No secrets. No external actions.

const MODEL = "@cf/google/gemma-4-26b-a4b-it";

const ALLOWED_ORIGIN =
  "https://azimi-studio-unique-vercel-coral.vercel.app";

function headers(origin = ALLOWED_ORIGIN) {
  return {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Referrer-Policy": "no-referrer",
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  };
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

function json(data, status, origin) {
  return new Response(JSON.stringify(data), {
    status,
    headers: headers(origin),
  });
}

export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";

    if (
      origin &&
      origin !== ALLOWED_ORIGIN
    ) {
      return json(
        { error: "Origin not allowed" },
        403,
        ALLOWED_ORIGIN
      );
    }

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: headers(ALLOWED_ORIGIN),
      });
    }

    if (request.method !== "POST") {
      return json(
        { error: "Method not allowed" },
        405,
        ALLOWED_ORIGIN
      );
    }

    const contentType =
      request.headers.get("Content-Type") || "";

    if (!contentType.includes("application/json")) {
      return json(
        { error: "JSON required" },
        415,
        ALLOWED_ORIGIN
      );
    }

    try {
      const body = await request.json();

      if (
        !body ||
        typeof body.message !== "string"
      ) {
        return json(
          { error: "Invalid message" },
          400,
          ALLOWED_ORIGIN
        );
      }

      const message = body.message.trim();

      if (!message) {
        return json(
          { error: "Message is empty" },
          400,
          ALLOWED_ORIGIN
        );
      }

      if (message.length > 12000) {
        return json(
          { error: "Message is too long" },
          400,
          ALLOWED_ORIGIN
        );
      }

      if (secretDetected(message)) {
        return json(
          {
            error:
              "I won't process passwords, API keys, tokens, MFA codes, recovery codes, or private keys.",
          },
          400,
          ALLOWED_ORIGIN
        );
      }

      const system = `
You are AZIMI AI, a replaceable intelligence engine inside AZIMI CORE.

Your role is to provide useful reasoning and technical assistance.

You are NOT the owner of AZIMI.

You do NOT have access to private AZIMI memory, passwords,
authentication credentials, private files, recovery codes,
API keys, or external accounts.

Never ask for secrets.

Never claim that an external action was performed unless
a real connected tool performed it.

Prefer practical, phone-friendly instructions.

When helping with AZIMI, follow:

BUILD → TEST → SECURITY REVIEW → DEPLOY → VERIFY → IMPROVE.

Be concise, accurate, honest, and professional.
`;

      const result = await env.AI.run(
        MODEL,
        {
          messages: [
            {
              role: "system",
              content: system,
            },
            {
              role: "user",
              content: message,
            },
          ],
          max_tokens: 1200,
        }
      );

      const reply =
        result?.response ||
        result?.result?.response ||
        "";

      if (
        typeof reply !== "string" ||
        !reply.trim()
      ) {
        return json(
          {
            error:
              "Cloudflare AI returned no readable response",
          },
          502,
          ALLOWED_ORIGIN
        );
      }

      return json(
        {
          reply: reply.trim(),
          assistant: "AZIMI AI CORE",
          engine: "AZIMI-CLOUDFLARE",
          model: MODEL,
          persistentMemory: false,
          authenticated: false,
          next: "AZIMI CORE ORCHESTRATOR",
        },
        200,
        ALLOWED_ORIGIN
      );
    } catch (error) {
      console.error(
        "AZIMI Cloudflare AI error:",
        error
      );

      return json(
        {
          error: "Cloudflare AI engine unavailable",
        },
        503,
        ALLOWED_ORIGIN
      );
    }
  },
};
