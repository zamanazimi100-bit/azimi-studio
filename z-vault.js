(() => {
  "use strict";

  // ============================================================
  // Z VAULT — AZIMI.STUDIO SECURITY & STORAGE CONTROL CORE v2
  // Defensive • Privacy-first • Phone-first • User-controlled
  //
  // IMPORTANT:
  // This client module NEVER stores or accepts:
  // passwords, API keys, MFA codes, verification codes,
  // recovery codes/keys, access tokens, refresh tokens,
  // private keys, authentication cookies, or session secrets.
  //
  // Real file storage, AI memory storage, authentication,
  // authorization, encryption and database controls belong
  // on the server/database layer and will be connected separately.
  // ============================================================

  const CORE = {
    name: "Z VAULT",
    version: "2.0",
    mode: "DEFENSIVE",
    architecture: "PHONE_FIRST_SECURE_STORAGE",

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
      "authentication cookie",
      "secret credential"
    ],

    storage: {
      photos: {
        id: "photos",
        title: "Photo Vault",
        description: "Protected personal and project photos",
        allowed: true
      },

      videos: {
        id: "videos",
        title: "Video Vault",
        description: "Protected personal and project videos",
        allowed: true
      },

      files: {
        id: "files",
        title: "File Vault",
        description: "Documents, project files and safe digital records",
        allowed: true
      },

      documents: {
        id: "documents",
        title: "Document Vault",
        description: "Important non-secret documents",
        allowed: true
      },

      notes: {
        id: "notes",
        title: "Secure Notes",
        description: "Safe notes and project information",
        allowed: true
      },

      backups: {
        id: "backups",
        title: "Backup Center",
        description: "Verified backups and recovery information",
        allowed: true
      },

      aiMemory: {
        id: "ai-memory",
        title: "AZIMI AI Memory",
        description:
          "Dedicated storage for user-approved, non-secret AI memories",
        allowed: true,
        isolated: true
      }
    },

    securityServices: {
      accountShield: {
        title: "Account Shield",
        checks: [
          "Authentication reviewed",
          "Recovery methods reviewed",
          "Active sessions reviewed",
          "Trusted devices reviewed",
          "Connected applications reviewed"
        ]
      },

      deviceShield: {
        title: "Device Shield",
        checks: [
          "Screen lock reviewed",
          "System updates checked",
          "Installed applications reviewed",
          "Application permissions reviewed",
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
          "Destination checked",
          "Unexpected attachment avoided",
          "Urgency claims questioned",
          "Official source used for verification"
        ]
      },

      fileProtection: {
        title: "File Protection",
        checks: [
          "File type checked",
          "Upload source reviewed",
          "Unsafe content rejected",
          "Access permissions reviewed",
          "Backup status checked"
        ]
      },

      aiGuard: {
        title: "AI Guard",
        checks: [
          "Secrets excluded from AI prompts",
          "Sensitive information minimized",
          "Memory approval required",
          "AI output reviewed before action",
          "External actions require verification"
        ]
      },

      recoveryCenter: {
        title: "Recovery Center",
        checks: [
          "Incident identified",
          "Official recovery route opened",
          "Sessions reviewed",
          "Security strengthened",
          "Recovery verified"
        ]
      },

      backupCenter: {
        title: "Backup Center",
        checks: [
          "Important data identified",
          "Safe backup location selected",
          "Backup verified",
          "Recovery process documented",
          "Old unnecessary copies reviewed"
        ]
      },

      auditCenter: {
        title: "Security Audit",
        checks: [
          "Security events recorded",
          "Risk warnings reviewed",
          "Unexpected activity investigated",
          "Access changes reviewed",
          "User remains in control"
        ]
      }
    }
  };

  // ------------------------------------------------------------
  // SECRET DETECTION
  // ------------------------------------------------------------

  function containsSecret(value) {
    if (typeof value !== "string") return true;

    return (
      /password\s*[:=]/i.test(value) ||
      /api[_-]?key\s*[:=]/i.test(value) ||
      /secret\s*[:=]/i.test(value) ||
      /mfa\s*(code|token)?\s*[:=]/i.test(value) ||
      /verification\s+code/i.test(value) ||
      /recovery\s+(code|key)/i.test(value) ||
      /access[_-]?token\s*[:=]/i.test(value) ||
      /refresh[_-]?token\s*[:=]/i.test(value) ||
      /authorization\s*[:=]/i.test(value) ||
      /bearer\s+[A-Za-z0-9._-]{20,}/i.test(value) ||
      /private[_-]?key/i.test(value) ||
      /-----BEGIN .*PRIVATE KEY-----/i.test(value)
    );
  }

  // ------------------------------------------------------------
  // SAFE NOTE / MEMORY VALIDATION
  // ------------------------------------------------------------

  function validateSafeData(value, maxLength = 1000) {
    if (typeof value !== "string") {
      return {
        accepted: false,
        reason: "Invalid text"
      };
    }

    const clean = value.trim();

    if (!clean) {
      return {
        accepted: false,
        reason: "Empty content"
      };
    }

    if (containsSecret(clean)) {
      return {
        accepted: false,
        reason:
          "Z VAULT does not accept passwords, API keys, tokens, MFA codes, verification codes, recovery codes, or private keys."
      };
    }

    return {
      accepted: true,
      text: clean.slice(0, maxLength)
    };
  }

  function safeNote(note) {
    return validateSafeData(note, 1000);
  }

  function safeAIMemory(memory) {
    return validateSafeData(memory, 1000);
  }

  // ------------------------------------------------------------
  // STORAGE INFORMATION
  // ------------------------------------------------------------

  function getStorageModule(name) {
    return CORE.storage[name] || null;
  }

  function getStorageOverview() {
    return Object.values(CORE.storage).map((item) => ({
      id: item.id,
      title: item.title,
      allowed: item.allowed,
      isolated: Boolean(item.isolated)
    }));
  }

  // ------------------------------------------------------------
  // SECURITY INFORMATION
  // ------------------------------------------------------------

  function getSecurityModule(name) {
    return CORE.securityServices[name] || null;
  }

  function getSecurityOverview() {
    return Object.entries(CORE.securityServices).map(
      ([id, service]) => ({
        id,
        title: service.title,
        checks: service.checks.length
      })
    );
  }

  // ------------------------------------------------------------
  // RISK ENGINE
  // ------------------------------------------------------------

  function assessTextRisk(value) {
    if (typeof value !== "string") {
      return {
        level: "HIGH",
        reason: "Invalid data"
      };
    }

    if (containsSecret(value)) {
      return {
        level: "BLOCKED",
        reason: "Potential secret detected"
      };
    }

    if (value.length > 12000) {
      return {
        level: "HIGH",
        reason: "Content exceeds safe processing size"
      };
    }

    return {
      level: "LOW",
      reason: "No protected secret pattern detected"
    };
  }

  // ------------------------------------------------------------
  // DEFENSIVE AUDIT EVENTS
  // ------------------------------------------------------------

  const auditEvents = [];

  function recordSecurityEvent(type, details = {}) {
    const event = {
      type: String(type || "UNKNOWN").slice(0, 100),
      details:
        details && typeof details === "object"
          ? Object.freeze({ ...details })
          : {},
      timestamp: new Date().toISOString()
    };

    auditEvents.push(event);

    // Keep only a small client-side diagnostic history.
    // Persistent audit storage will be implemented server-side.
    if (auditEvents.length > 100) {
      auditEvents.shift();
    }

    return Object.freeze({ ...event });
  }

  function getRecentSecurityEvents(limit = 20) {
    const safeLimit = Math.max(
      1,
      Math.min(Number(limit) || 20, 100)
    );

    return auditEvents
      .slice(-safeLimit)
      .map((event) => ({
        ...event,
        details: { ...event.details }
      }));
  }

  // ------------------------------------------------------------
  // VAULT STATUS
  // ------------------------------------------------------------

  function getVaultStatus() {
    return {
      name: CORE.name,
      version: CORE.version,
      mode: CORE.mode,
      architecture: CORE.architecture,
      secretProtection: "ACTIVE",
      aiMemoryIsolation: "ACTIVE",
      storageControl: "READY",
      securityServices:
        Object.keys(CORE.securityServices).length,
      storageModules: Object.keys(CORE.storage).length
    };
  }

  // ------------------------------------------------------------
  // PUBLIC API
  // ------------------------------------------------------------

  window.ZVAULT = Object.freeze({
    name: CORE.name,
    version: CORE.version,
    mode: CORE.mode,

    protectedSecretTypes: Object.freeze([
      ...CORE.protectedSecretTypes
    ]),

    getVaultStatus,

    getStorageModule,
    getStorageOverview,

    getSecurityModule,
    getSecurityOverview,

    assessTextRisk,

    safeNote,
    safeAIMemory,

    recordSecurityEvent,
    getRecentSecurityEvents
  });

  recordSecurityEvent("VAULT_CORE_LOADED", {
    version: CORE.version,
    mode: CORE.mode
  });

  console.log(
    "Z VAULT Security & Storage Control Core v2 loaded."
  );
})();
