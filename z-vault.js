(() => {
  "use strict";

  // ============================================================
  // Z VAULT — AZIMI.STUDIO SECURITY FOUNDATION v1
  // Defensive, privacy-first, phone-first
  //
  // IMPORTANT:
  // This module NEVER stores passwords, API keys, MFA codes,
  // verification codes, recovery codes, tokens, or private keys.
  // ============================================================

  const ZVAULT = {
    name: "Z VAULT",
    version: "1.0",
    mode: "DEFENSIVE",

    protectedSecretTypes: [
      "password",
      "api key",
      "mfa code",
      "verification code",
      "recovery code",
      "recovery key",
      "access token",
      "refresh token",
      "private key",
      "session token",
      "authentication cookie"
    ],

    modules: {
      accountShield: {
        title: "Account Shield",
        checks: [
          "Authentication method reviewed",
          "Recovery methods reviewed",
          "Active sessions reviewed",
          "Trusted devices reviewed",
          "Connected applications reviewed"
        ]
      },

      phoneShield: {
        title: "Phone Shield",
        checks: [
          "Screen lock enabled",
          "System updates checked",
          "App permissions reviewed",
          "Unknown applications reviewed",
          "Device recovery prepared"
        ]
      },

      privacyShield: {
        title: "Privacy Shield",
        checks: [
          "Browser permissions reviewed",
          "Application permissions reviewed",
          "Unnecessary access removed",
          "Information sharing reviewed",
          "Privacy settings reviewed"
        ]
      },

      phishingDefense: {
        title: "Phishing Defense",
        checks: [
          "Sender verified",
          "Link destination checked",
          "Unexpected attachment avoided",
          "Urgency claim questioned",
          "Official website used for verification"
        ]
      },

      recoveryCenter: {
        title: "Recovery Center",
        checks: [
          "Incident identified",
          "Official provider recovery opened",
          "Sessions reviewed",
          "Account security strengthened",
          "Recovery completed through official channels"
        ]
      },

      backupCenter: {
        title: "Backup Center",
        checks: [
          "Important project files identified",
          "Safe backup location selected",
          "Backup tested",
          "Recovery process documented",
          "Old unnecessary copies reviewed"
        ]
      },

      aiGuard: {
        title: "AI Guard",
        checks: [
          "Secrets excluded from AI prompts",
          "Sensitive information minimized",
          "AI output reviewed before action",
          "External actions verified",
          "Security decisions remain user-controlled"
        ]
      }
    }
  };

  function containsSecret(text) {
    if (typeof text !== "string") return true;

    return (
      /password\s*[:=]/i.test(text) ||
      /api[_-]?key\s*[:=]/i.test(text) ||
      /secret\s*[:=]/i.test(text) ||
      /mfa\s+code/i.test(text) ||
      /verification\s+code/i.test(text) ||
      /recovery\s+(code|key)/i.test(text) ||
      /access[_-]?token\s*[:=]/i.test(text) ||
      /refresh[_-]?token\s*[:=]/i.test(text) ||
      /private[_-]?key/i.test(text) ||
      /-----BEGIN .*PRIVATE KEY-----/i.test(text)
    );
  }

  function safeNote(note) {
    if (typeof note !== "string") {
      return {
        accepted: false,
        reason: "Invalid note"
      };
    }

    const clean = note.trim();

    if (!clean) {
      return {
        accepted: false,
        reason: "Empty note"
      };
    }

    if (containsSecret(clean)) {
      return {
        accepted: false,
        reason:
          "Z VAULT does not store passwords, API keys, MFA codes, verification codes, recovery codes, tokens, or private keys."
      };
    }

    return {
      accepted: true,
      text: clean.slice(0, 1000)
    };
  }

  function getModule(name) {
    return ZVAULT.modules[name] || null;
  }

  function getSecurityOverview() {
    return Object.entries(ZVAULT.modules).map(
      ([id, module]) => ({
        id,
        title: module.title,
        checks: module.checks.length
      })
    );
  }

  window.ZVAULT = Object.freeze({
    name: ZVAULT.name,
    version: ZVAULT.version,
    mode: ZVAULT.mode,
    protectedSecretTypes: Object.freeze([
      ...ZVAULT.protectedSecretTypes
    ]),
    getModule,
    getSecurityOverview,
    safeNote
  });

  console.log(
    "Z VAULT security foundation loaded."
  );
})();
