export default async function handler(req, res) {
  if (req.method === "GET") {
    return res.status(200).json({
      name: "azimi-mcp",
      version: "0.1.0",
      project: "Azimi Studio",
      component: "Azimi MCP",
      mode: "read-only",
      tools: [
        {
          name: "get_azimi_status",
          description:
            "Returns the safe, public status of the Azimi Studio project."
        }
      ],
      security: {
        passwords: false,
        apiKeys: false,
        verificationCodes: false,
        recoveryCodes: false,
        destructiveActions: false
      }
    });
  }

  if (req.method === "POST") {
    return res.status(200).json({
      content: [
        {
          type: "text",
          text: JSON.stringify({
            project: "Azimi Studio",
            component: "Azimi MCP",
            version: "v1",
            mode: "read-only",
            security: {
              passwords: false,
              apiKeys: false,
              verificationCodes: false,
              recoveryCodes: false,
              destructiveActions: false
            }
          })
        }
      ]
    });
  }

  return res.status(405).json({
    error: "Method not allowed"
  });
}
