import { createMcpHandler, McpServer } from "@modelcontextprotocol/server";

const handler = createMcpHandler(() => {
  const server = new McpServer({
    name: "azimi-mcp",
    version: "0.1.0",
  });

  server.registerTool(
    "get_azimi_status",
    {
      description:
        "Returns the safe, public status of the Azimi Studio project.",
    },
    async () => ({
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
              destructiveActions: false,
            },
          }),
        },
      ],
    })
  );

  return server;
});

export default handler;
