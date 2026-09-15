export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({
      error: "Method not allowed",
    });
  }

  try {
    const { message } = req.body || {};

    if (!message || typeof message !== "string") {
      return res.status(400).json({
        error: "Message is required",
      });
    }

    const apiKey = process.env.OPENAI_API_KEY;

    if (!apiKey) {
      return res.status(500).json({
        error: "OPENAI_API_KEY is not configured on the server.",
      });
    }

    const response = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: "gpt-5.6-luna",
        instructions:
          "You are Azimi AI, the intelligent assistant for AZIMI.STUDIO. Be helpful, accurate, practical, professional, and concise. Explain clearly and help the user step by step.",
        input: message,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      console.error("OpenAI API error:", data);

      return res.status(response.status).json({
        error:
          data?.error?.message ||
          data?.message ||
          `OpenAI request failed with status ${response.status}.`,
      });
    }

    let reply = "";

    if (typeof data.output_text === "string") {
      reply = data.output_text;
    }

    if (!reply && Array.isArray(data.output)) {
      for (const item of data.output) {
        if (!Array.isArray(item.content)) continue;

        for (const content of item.content) {
          if (
            content?.type === "output_text" &&
            typeof content.text === "string"
          ) {
            reply += content.text;
          }
        }
      }
    }

    if (!reply) {
      console.error("Unexpected OpenAI response:", JSON.stringify(data));

      return res.status(502).json({
        error: "The AI returned a response, but no readable text was found.",
      });
    }

    return res.status(200).json({
      reply: reply.trim(),
    });
  } catch (error) {
    console.error("Azimi AI server error:", error);

    return res.status(500).json({
      error: "Azimi AI server error.",
    });
  }
}
