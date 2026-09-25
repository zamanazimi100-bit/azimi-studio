"use client";

import { useEffect, useState } from "react";

export default function Home() {
  const [authCode, setAuthCode] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");

    if (code && code.trim()) {
      setAuthCode(code.trim());
    }
  }, []);

  function openGuardian() {
    if (!authCode) {
      setError("Authentication code is missing.");
      return;
    }

    const callbackUrl =
      `azimi://auth-callback?code=${encodeURIComponent(authCode)}`;

    window.location.href = callbackUrl;
  }

  if (authCode) {
    return (
      <main
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "24px",
          background: "#050505",
          color: "#ffffff",
          fontFamily: "Arial, Helvetica, sans-serif",
        }}
      >
        <section
          style={{
            width: "100%",
            maxWidth: "520px",
            padding: "40px",
            border: "1px solid rgba(255,255,255,0.14)",
            borderRadius: "20px",
            background: "rgba(255,255,255,0.04)",
            textAlign: "center",
            boxSizing: "border-box",
          }}
        >
          <div
            style={{
              fontSize: "12px",
              letterSpacing: "0.18em",
              opacity: 0.55,
              marginBottom: "18px",
            }}
          >
            AZIMI · SECURE AUTH
          </div>

          <h1
            style={{
              margin: "0 0 14px",
              fontSize: "32px",
              fontWeight: 700,
            }}
          >
            Authentication Ready
          </h1>

          <p
            style={{
              margin: "0 auto 28px",
              maxWidth: "420px",
              lineHeight: 1.6,
              opacity: 0.72,
            }}
          >
            Your AZIMI authentication request is
            ready to continue in Guardian.
          </p>

          <button
            type="button"
            onClick={openGuardian}
            style={{
              width: "100%",
              padding: "15px 20px",
              border: "none",
              borderRadius: "12px",
              background: "#ffffff",
              color: "#000000",
              fontSize: "15px",
              fontWeight: 700,
              cursor: "pointer",
            }}
          >
            OPEN AZIMI GUARDIAN
          </button>

          {error && (
            <p
              style={{
                marginTop: "18px",
                color: "#ff7070",
                fontSize: "14px",
              }}
            >
              {error}
            </p>
          )}

          <p
            style={{
              marginTop: "24px",
              marginBottom: 0,
              fontSize: "12px",
              opacity: 0.45,
              lineHeight: 1.5,
            }}
          >
            The authentication code is handled
            privately and is not displayed.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "24px",
        background: "#050505",
        color: "#ffffff",
        fontFamily: "Arial, Helvetica, sans-serif",
      }}
    >
      <section
        style={{
          textAlign: "center",
        }}
      >
        <h1>AZIMI STUDIO</h1>

        <p>
          Building the future. One project at a time.
        </p>
      </section>
    </main>
  );
}
