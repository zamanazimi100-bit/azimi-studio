import { createMcpHandler } from "mcp-handler";

const handler = createMcpHandler(
  (server) => {
    server.registerTool(
      "get_azimi_status",
      {
        description:
          "Returns the safe, public, read-only status of Azimi Studio.",
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
  },
  {
    serverInfo: {
      name: "azimi-mcp",
      version: "0.1.0",
    },
  }
);

export { handler as GET, handler as POST, handler as DELETE };
