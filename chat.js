import crypto from "crypto";

const ORCHESTRATOR_URL =
  "https://azimi-studio-unique-vercel-coral.vercel.app/api/azimi-orchestrator";

const MAX_MESSAGE_LENGTH = 12000;

const MAX_HISTORY_MESSAGES = 12;
const MAX_MEMORY_ITEMS = 50;
const MAX_CONTEXT_LENGTH = 30000;

/*
 * Guardian signatures are intentionally short-lived.
 *
 * This limits the usefulness of a captured signed request.
 */
const MAX_REQUEST_AGE_MS = 60 * 1000;

/*
 * -------------------------------------------------------
 * GUARDIAN ENROLLED IDENTITY
 * -------------------------------------------------------
 *
 * The public key is NOT secret.
 *
 * It is stored server-side as a Vercel environment variable.
 *
 * Never put the corresponding private key in Vercel.
 */
const GUARDIAN_KEY_ID =
  process.env.ATLAS_GUARDIAN_KEY_ID ||
  "ZAMAN-AZIMI-GUARDIAN-01";

const GUARDIAN_PUBLIC_KEY =
  process.env.ATLAS_GUARDIAN_PUBLIC_KEY ||
  "";

/*
 * -------------------------------------------------------
 * SECURITY HEADERS
 * -------------------------------------------------------
 */

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

/*
 * -------------------------------------------------------
 * SECRET PROTECTION
 * -------------------------------------------------------
 */

function secretDetected(value) {
  if (typeof value !== "string") {
    return true;
  }

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

/*
 * -------------------------------------------------------
 * SAFE CHAT CONTEXT
 * -------------------------------------------------------
 */

function safeChatMessages(
  value,
  allowSystem = false
) {
  if (!Array.isArray(value)) {
    return [];
  }

  const limit =
    allowSystem
      ? MAX_MEMORY_ITEMS
      : MAX_HISTORY_MESSAGES;

  return value
    .slice(-limit)
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
          ? item.content
              .trim()
              .slice(0, 4000)
          : "";

      const allowedRoles =
        allowSystem
          ? [
              "user",
              "assistant",
              "system"
            ]
          : [
              "user",
              "assistant"
            ];

      if (
        !allowedRoles.includes(role) ||
        !content
      ) {
        return null;
      }

      if (
        secretDetected(content)
      ) {
        return null;
      }

      return {
        role,
        content
      };
    })
    .filter(Boolean);
}

function buildApprovedContext(
  history,
  memory
) {
  const sections = [];

  if (
    memory.length
  ) {
    sections.push(
      "APPROVED PERSISTENT AZIMI MEMORY:\n" +
      memory
        .map(
          (item, index) =>
            `${index + 1}. [${item.role}] ${item.content}`
        )
        .join("\n")
    );
  }

  if (
    history.length
  ) {
    sections.push(
      "RECENT CONVERSATION CONTEXT:\n" +
      history
        .map(
          (item) =>
            `[${item.role}] ${item.content}`
        )
        .join("\n")
    );
  }

  return sections
    .join("\n\n")
    .slice(
      0,
      MAX_CONTEXT_LENGTH
    );
}

/*
 * -------------------------------------------------------
 * RAW REQUEST BODY
 * -------------------------------------------------------
 *
 * The signature is bound to the exact bytes Guardian sent.
 *
 * We intentionally disable automatic body parsing.
 */
export const config = {
  api: {
    bodyParser: false,
  },
};

function readRawBody(req) {

  return new Promise(
    (resolve, reject) => {

      const chunks = [];

      req.on(
        "data",
        (chunk) => {
          chunks.push(
            Buffer.isBuffer(chunk)
              ? chunk
              : Buffer.from(chunk)
          );
        }
      );

      req.on(
        "end",
        () => {
          resolve(
            Buffer.concat(
              chunks
            ).toString("utf8")
          );
        }
      );

      req.on(
        "error",
        reject
      );
    }
  );
}

/*
 * -------------------------------------------------------
 * GUARDIAN SIGNATURE VERIFICATION
 * -------------------------------------------------------
 */

function verifyGuardianRequest(
  req,
  rawBody
) {

  if (
    !GUARDIAN_PUBLIC_KEY
  ) {
    return {
      ok: false,
      status: 500,
      error:
        "Guardian Atlas public identity is not configured.",
      code:
        "GUARDIAN_IDENTITY_NOT_CONFIGURED",
    };
  }

  const keyId =
    req.headers[
      "x-azimi-guardian-key-id"
    ] || "";

  const timestampHeader =
    req.headers[
      "x-azimi-guardian-timestamp"
    ] || "";

  const requestId =
    req.headers[
      "x-azimi-guardian-request-id"
    ] || "";

  const suppliedSignature =
    req.headers[
      "x-azimi-guardian-signature"
    ] || "";

  if (
    keyId !== GUARDIAN_KEY_ID
  ) {
    return {
      ok: false,
      status: 401,
      error:
        "Guardian identity rejected.",
      code:
        "GUARDIAN_KEY_REJECTED",
    };
  }

  if (
    !timestampHeader ||
    !requestId ||
    !suppliedSignature
  ) {
    return {
      ok: false,
      status: 401,
      error:
        "Guardian authorization proof is incomplete.",
      code:
        "GUARDIAN_PROOF_INCOMPLETE",
    };
  }

  if (
    requestId.length > 128
  ) {
    return {
      ok: false,
      status: 401,
      error:
        "Guardian request identifier is invalid.",
      code:
        "GUARDIAN_REQUEST_ID_INVALID",
    };
  }

  const timestamp =
    Number(
      timestampHeader
    );

  if (
    !Number.isSafeInteger(
      timestamp
    )
  ) {
    return {
      ok: false,
      status: 401,
      error:
        "Guardian request timestamp is invalid.",
      code:
        "GUARDIAN_TIMESTAMP_INVALID",
    };
  }

  const age =
    Math.abs(
      Date.now() -
      timestamp
    );

  if (
    age >
    MAX_REQUEST_AGE_MS
  ) {
    return {
      ok: false,
      status: 408,
      error:
        "Guardian request has expired.",
      code:
        "GUARDIAN_REQUEST_EXPIRED",
    };
  }

  /*
   * Hash the exact raw HTTP body.
   */
  const bodyHash =
    crypto
      .createHash("sha256")
      .update(
        rawBody,
        "utf8"
      )
      .digest("hex");

  /*
   * Must match GuardianAtlasIdentity.kt exactly.
   */
  const canonicalPayload =
    `${timestamp}\n${requestId}\n${bodyHash}`;

  try {

    const verifier =
      crypto.createVerify(
        "SHA256"
      );

    verifier.update(
      canonicalPayload,
      "utf8"
    );

    verifier.end();

    const valid =
      verifier.verify(
        GUARDIAN_PUBLIC_KEY,
        Buffer.from(
          suppliedSignature,
          "base64"
        )
      );

    if (!valid) {
      return {
        ok: false,
        status: 401,
        error:
          "Guardian cryptographic authorization failed.",
        code:
          "GUARDIAN_SIGNATURE_INVALID",
      };
    }

  } catch (error) {

    console.error(
      "Guardian signature verification error:",
      error
    );

    return {
      ok: false,
      status: 401,
      error:
        "Guardian cryptographic authorization failed.",
      code:
        "GUARDIAN_SIGNATURE_INVALID",
    };
  }

  return {
    ok: true,
    keyId,
    requestId,
    timestamp,
  };
}

/*
 * -------------------------------------------------------
 * ATLAS CORE
 * -------------------------------------------------------
 */

async function callAtlasCore(
  message,
  approvedContext
) {

  const controller =
    new AbortController();

  const timeout =
    setTimeout(
      () => controller.abort(),
      20_000
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

            /*
             * CRITICAL:
             *
             * This secret exists ONLY on Vercel.
             *
             * It is never supplied by Guardian.
             */
            "X-AZIMI-ATLAS-SECRET":
              process.env
                .ATLAS_INTERNAL_SECRET ||
              "",
          },

          body:
            JSON.stringify({
              message,

              context:
                approvedContext || "",

              atlasRequest: {
                version:
                  "1.3",

                authenticated:
                  true,

                authentication:
                  "GUARDIAN_CRYPTOGRAPHIC_IDENTITY",

                memoryBoundary:
                  "GUARDIAN_APPROVED_CONTEXT_ONLY",

                vaultAccess:
                  false,

                conversationHistory:
                  Boolean(
                    approvedContext
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

    if (
      !response.ok
    ) {

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
        error?.name ===
        "AbortError"
          ? "ATLAS request timed out"
          : "ATLAS service unavailable",
    };

  } finally {

    clearTimeout(
      timeout
    );
  }
}

/*
 * -------------------------------------------------------
 * MAIN API HANDLER
 * -------------------------------------------------------
 */

export default async function handler(
  req,
  res
) {

  securityHeaders(
    res
  );

  /*
   * 1. METHOD GATE
   */

  if (
    req.method !== "POST"
  ) {

    res.setHeader(
      "Allow",
      "POST"
    );

    return res
      .status(405)
      .json({
        error:
          "Method not allowed",
      });
  }

  try {

    /*
     * 2. READ EXACT BODY
     */

    const rawBody =
      await readRawBody(
        req
      );

    if (
      !rawBody
    ) {

      return res
        .status(400)
        .json({
          error:
            "Request body is empty",
        });
    }

    /*
     * 3. GUARDIAN CRYPTOGRAPHIC
     *    AUTHENTICATION
     */

    const guardianAuth =
      verifyGuardianRequest(
        req,
        rawBody
      );

    if (
      !guardianAuth.ok
    ) {

      console.error(
        "Guardian Atlas authorization rejected:",
        guardianAuth.code
      );

      return res
        .status(
          guardianAuth.status
        )
        .json({
          error:
            guardianAuth.error,

          code:
            guardianAuth.code,

          assistant:
            "ATLAS CORE",

          authenticated:
            false,
        });
    }

    /*
     * 4. PARSE BODY
     */

    let body;

    try {

      body =
        JSON.parse(
          rawBody
        );

    } catch {

      return res
        .status(400)
        .json({
          error:
            "Invalid JSON request body",
        });
    }

    /*
     * 5. USER MESSAGE
     */

    const message =
      typeof body?.message === "string"
        ? body.message.trim()
        : "";

    if (
      !message
    ) {

      return res
        .status(400)
        .json({
          error:
            "Message is empty",
        });
    }

    if (
      message.length >
      MAX_MESSAGE_LENGTH
    ) {

      return res
        .status(400)
        .json({
          error:
            "Message is too long",
        });
    }

    /*
     * 6. PROTECTED-CREDENTIAL GATE
     */

    if (
      secretDetected(
        message
      )
    ) {

      return res
        .status(400)
        .json({
          error:
            "I won't process passwords, API keys, tokens, MFA codes, recovery codes, or private keys.",
        });
    }

    /*
     * 7. SANITIZE HISTORY + MEMORY
     */

    const safeHistory =
      safeChatMessages(
        body?.history,
        false
      );

    const safeMemory =
      safeChatMessages(
        body?.memory,
        true
      );

    const approvedContext =
      buildApprovedContext(
        safeHistory,
        safeMemory
      );

    /*
     * 8. ATLAS CORE
     */

    const result =
      await callAtlasCore(
        message,
        approvedContext
      );

    /*
     * 9. ENGINE FAILURE
     */

    if (
      !result.ok
    ) {

      console.error(
        "ATLAS online engine error:",
        result.error
      );

      return res
        .status(502)
        .json({

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
     * 10. SUCCESS
     */

    return res
      .status(200)
      .json({

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
          Boolean(
            approvedContext
          ),

        memoryUsed:
          Boolean(
            safeMemory.length
          ),

        authenticated:
          true,

        authentication:
          "GUARDIAN_CRYPTOGRAPHIC_IDENTITY",

        guardianKeyId:
          guardianAuth.keyId,

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

    return res
      .status(500)
      .json({

        error:
          error?.name ===
          "AbortError"
            ? "ATLAS request timed out"
            : "ATLAS service unavailable",

        memoryUsed:
          false,

        vaultAccess:
          false,
      });
  }
}
