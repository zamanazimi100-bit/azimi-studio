import { supabaseAdmin } from "../lib/supabase-server.js";

const BUCKET = "z-vault";
const MAX_NAME_LENGTH = 180;

function getToken(req) {
  const header = req.headers.authorization || "";

  if (!header.startsWith("Bearer ")) {
    return null;
  }

  return header.slice(7).trim();
}

function safeFileName(name) {
  if (typeof name !== "string") return null;

  const cleaned = name
    .trim()
    .replace(/[\/\\]/g, "_")
    .replace(/\.\./g, "_")
    .replace(/[^\w.\- ()[\]]/g, "_")
    .slice(0, MAX_NAME_LENGTH);

  return cleaned || null;
}

function isSafePath(path, userId) {
  if (typeof path !== "string") return false;

  return (
    path.startsWith(`${userId}/`) &&
    !path.includes("..") &&
    !path.includes("//")
  );
}

async function authenticate(req) {
  const token = getToken(req);

  if (!token) {
    return {
      error: "Authentication required."
    };
  }

  const { data, error } =
    await supabaseAdmin.auth.getUser(token);

  if (error || !data?.user) {
    return {
      error: "Invalid or expired authentication session."
    };
  }

  return {
    user: data.user
  };
}

export default async function handler(req, res) {
  try {
    const auth = await authenticate(req);

    if (auth.error) {
      return res.status(401).json({
        ok: false,
        error: auth.error
      });
    }

    const user = auth.user;
    const userId = user.id;

    /*
     * GET
     * List only this authenticated user's Vault files.
     */
    if (req.method === "GET") {
      const { data, error } = await supabaseAdmin.storage
        .from(BUCKET)
        .list(userId, {
          limit: 100,
          offset: 0,
          sortBy: {
            column: "created_at",
            order: "desc"
          }
        });

      if (error) {
        console.error("Z VAULT list error:", error);

        return res.status(500).json({
          ok: false,
          error: "Vault storage unavailable."
        });
      }

      return res.status(200).json({
        ok: true,
        files: data || []
      });
    }

    /*
     * POST
     * Create a short-lived signed upload URL.
     *
     * The actual file is uploaded directly to
     * the private Supabase bucket.
     */
    if (req.method === "POST") {
      const body =
        typeof req.body === "string"
          ? JSON.parse(req.body || "{}")
          : req.body || {};

      const action = body.action;

      if (action === "create-upload") {
        const fileName = safeFileName(body.fileName);

        if (!fileName) {
          return res.status(400).json({
            ok: false,
            error: "A valid file name is required."
          });
        }

        const path = `${userId}/${crypto.randomUUID()}-${fileName}`;

        const { data, error } =
          await supabaseAdmin.storage
            .from(BUCKET)
            .createSignedUploadUrl(path);

        if (error) {
          console.error(
            "Z VAULT signed upload error:",
            error
          );

          return res.status(500).json({
            ok: false,
            error: "Unable to prepare secure upload."
          });
        }

        return res.status(200).json({
          ok: true,
          path,
          token: data.token
        });
      }

      if (action === "create-download") {
        const path = body.path;

        if (!isSafePath(path, userId)) {
          return res.status(403).json({
            ok: false,
            error: "Vault access denied."
          });
        }

        const { data, error } =
          await supabaseAdmin.storage
            .from(BUCKET)
            .createSignedUrl(path, 60);

        if (error) {
          console.error(
            "Z VAULT signed download error:",
            error
          );

          return res.status(500).json({
            ok: false,
            error: "Unable to create secure download."
          });
        }

        return res.status(200).json({
          ok: true,
          url: data.signedUrl,
          expiresIn: 60
        });
     
