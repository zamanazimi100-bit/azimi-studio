import { supabaseAdmin } from "../lib/supabase-server";

function looksLikeSecret(value) {
  const text = String(value || "").trim();

  if (!text) return false;

  return (
    /password\s*[:=]/i.test(text) ||
    /passcode\s*[:=]/i.test(text) ||
    /verification\s*code/i.test(text) ||
    /security\s*code/i.test(text) ||
    /recovery\s*code/i.test(text) ||
    /backup\s*code/i.test(text) ||
    /api[_ -]?key\s*[:=]/i.test(text) ||
    /secret\s*key\s*[:=]/i.test(text) ||
    /service[_ -]?role/i.test(text) ||
    /bearer\s+[a-z0-9._-]+/i.test(text)
  );
}

function getAccessToken(req) {
  const header = req.headers.authorization || "";

  if (!header.startsWith("Bearer ")) {
    return null;
  }

  return header.slice(7).trim() || null;
}

async function authenticate(req) {
  const accessToken = getAccessToken(req);

  if (!accessToken) {
    return {
      error: "Authentication required."
    };
  }

  const {
    data: { user },
    error
  } = await supabaseAdmin.auth.getUser(accessToken);

  if (error || !user) {
    return {
      error: "Invalid or expired session."
    };
  }

  return { user };
}

export default async function handler(req, res) {
  try {
    const auth = await authenticate(req);

    if (auth.error) {
      return res.status(401).json({
        error: auth.error
      });
    }

    const userId = auth.user.id;

    if (req.method === "GET") {
      const { data, error } = await supabaseAdmin
        .from("ai_memories")
        .select("id, memory, created_at, updated_at")
        .eq("user_id", userId)
        .order("updated_at", { ascending: false })
        .limit(100);

      if (error) {
        console.error("Memory read error:", error);

        return res.status(500).json({
          error: "Memory service unavailable."
        });
      }

      const safeMemories = (data || []).filter(
        (item) => !looksLikeSecret(item.memory)
      );

      return res.status(200).json({
        memories: safeMemories,
        authenticated: true
      });
    }

    if (req.method === "POST") {
      const memory = String(req.body?.memory || "").trim();

      if (!memory) {
        return res.status(400).json({
          error: "Memory text is required."
        });
      }

      if (memory.length > 1000) {
        return res.status(400).json({
          error: "Memory is too long."
        });
      }

      if (looksLikeSecret(memory)) {
        return res.status(400).json({
          error:
            "This memory looks like sensitive credentials and was not stored."
        });
      }

      const { data: existing, error: existingError } =
        await supabaseAdmin
          .from("ai_memories")
          .select("id, memory")
          .eq("user_id", userId)
          .eq("memory", memory)
          .limit(1);

      if (existingError) {
        console.error("Memory duplicate check error:", existingError);

        return res.status(500).json({
          error: "Memory service unavailable."
        });
      }

      if (existing && existing.length > 0) {
        return res.status(200).json({
          memory: existing[0],
          alreadyExists: true,
          authenticated: true
        });
      }

      const { data, error } = await supabaseAdmin
        .from("ai_memories")
        .insert({
          user_id: userId,
          memory
        })
        .select("id, memory, created_at, updated_at")
        .single();

      if (error) {
        console.error("Memory insert error:", error);

        return res.status(500).json({
          error: "Memory service unavailable."
        });
      }

      return res.status(201).json({
        memory: data,
        saved: true,
        authenticated: true
      });
    }

    if (req.method === "DELETE") {
      const memoryId = String(req.body?.id || "").trim();

      if (!memoryId) {
        return res.status(400).json({
          error: "Memory id is required."
        });
      }

      const { error } = await supabaseAdmin
        .from("ai_memories")
        .delete()
        .eq("id", memoryId)
        .eq("user_id", userId);

      if (error) {
        console.error("Memory delete error:", error);

        return res.status(500).json({
          error: "Memory service unavailable."
        });
      }

      return res.status(200).json({
        deleted: true,
        authenticated: true
      });
    }

    res.setHeader("Allow", "GET, POST, DELETE");

    return res.status(405).json({
      error: "Method not allowed."
    });
  } catch (error) {
    console.error("Memory API error:", error);

    return res.status(500).json({
      error: "Memory service unavailable."
    });
  }
}
