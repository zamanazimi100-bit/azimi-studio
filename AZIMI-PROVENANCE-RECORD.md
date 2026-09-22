# AZIMI Provenance Record

**Record ID:** AZI-PROVENANCE-001  
**Record Type:** Internal Ownership, Provenance and Verification Record  
**Status:** ACTIVE  
**Record Date:** 2026-09-22  
**Owner:** Zaman Azimi  
**Owner Role:** FOUNDER_CREATOR_OWNER  
**Authority:** FINAL_OWNER_AUTHORITY

---

## 1. Purpose

This document records the provenance, ownership context, development history,
verification state, and evidence references for the AZIMI ecosystem.

It is an internal project record maintained as part of the AZIMI Ownership,
Protection, Continuity, Recovery, and Verification architecture.

This record does not replace legal registration, contracts, licenses,
trademarks, patents, copyright registration, or other formal legal processes.

---

## 2. Creator and Owner

**Creator:** Zaman Azimi

**Owner:** Zaman Azimi

**Owner Role:** FOUNDER_CREATOR_OWNER

**Owner Authority:** FINAL_OWNER_AUTHORITY

The AZIMI architecture is designed around explicit owner control.

External services, infrastructure providers, repositories, deployment
platforms, databases, and AI providers do not receive ownership authority
through their use within AZIMI.

---

## 3. Primary Systems

### AZIMI

**System ID:** AZI

**Identity:** AZIMI

**Purpose:** Technology ecosystem and protected project architecture

**Owner Authority:** Zaman Azimi

**Status:** ACTIVE

### AZIMI.STUDIO

**System ID:** AZI-STUDIO

**Identity:** AZIMI.STUDIO

**Purpose:** Official studio, public identity, portfolio, and future
products/services

**Owner Authority:** Zaman Azimi

**Status:** ACTIVE

---

## 4. Primary Repository

**Repository:** `zamanazimi100-bit/azimi-studio`

**Repository Role:** Project source and version-control infrastructure

**Ownership Authority:** None

The repository is treated as infrastructure for storing and versioning
project source code and documentation.

The AZIMI ownership model requires the project to remain exportable and
recoverable independently of the repository provider.

---

## 5. AZIMI Guardian

**System:** AZIMI Guardian

**Package:** `com.azimi.guardian`

**Platform:** Android

**Minimum SDK:** 29

**Compile SDK:** 35

**Target SDK:** 35

**Current Application Version:** 1.0

Guardian is the device-side protection and enforcement layer of the AZIMI
architecture.

Guardian is responsible for enforcing local security boundaries,
authorization boundaries, protected storage, device-side controls,
diagnostics, recovery foundations, and controlled access to Atlas.

---

## 6. Atlas

**System:** Atlas

**Asset ID:** AZI-ATLAS-001

**Role:** CONTROLLED_INTELLIGENCE_AND_COORDINATION

Atlas is designed as a provider-independent intelligence and coordination
layer within AZIMI.

Atlas architecture separates:

- intelligence planning
- routing
- local intelligence
- external providers
- authentication
- memory
- protected storage
- Guardian enforcement
- owner authority

External AI providers are replaceable services.

No external AI provider is the owner or authority of Atlas.

---

## 7. Z Vault

**System:** Z Vault

**Asset ID:** AZI-VAULT-001

**Access Level:** OWNER_ONLY

Z Vault is the protected storage foundation for sensitive AZIMI data and
owner-controlled project information.

The intended architecture prevents raw Z Vault contents from being
automatically transmitted to:

- `/api/chat`
- Supabase
- Cloudflare
- external AI providers
- other external intelligence services

Only explicitly approved, non-secret project context may be transformed
into an online request when permitted by Guardian and Atlas security policy.

---

## 8. Memory Protection

AZIMI memory follows the approved-project-context model.

The following categories are prohibited from project memory:

- passwords
- API keys
- authentication tokens
- recovery codes
- verification codes
- private credentials
- biometric material
- other protected authentication information

Memory must remain owner-controlled and portable.

Memory must not become an automatic cloud copy of Z Vault.

There is no automatic requirement to save every interaction.

Explicit deletion and forget operations remain authoritative.

---

## 9. Provider Independence

AZIMI follows a provider-independent architecture.

### GitHub

Role:

`REPOSITORY_INFRASTRUCTURE`

Ownership authority:

`false`

### Vercel

Role:

`DEPLOYMENT_INFRASTRUCTURE`

Ownership authority:

`false`

### Cloudflare

Role:

`INFRASTRUCTURE`

Ownership authority:

`false`

### Supabase

Role:

`DATA_INFRASTRUCTURE`

Ownership authority:

`false`

### AI Providers

Role:

`REPLACEABLE_INTELLIGENCE_SERVICES`

Ownership authority:

`false`

Provider lock-in:

`false`

The architecture must remain capable of replacing external providers without
transferring ownership of AZIMI.

---

## 10. Provenance Chain

The AZIMI provenance chain is:

1. IDEA
2. REQUIREMENT
3. DESIGN
4. IMPLEMENTATION
5. VERSION
6. TEST
7. RELEASE
8. PUBLICATION
9. BACKUP
10. VERIFICATION

This record contributes to the documentation and verification stages of
that chain.

---

## 11. Guardian Build Verification Checkpoint

### Current Recorded Checkpoint

**Build:** Guardian Build #49

**Build Result:** SUCCESSFUL

**Installation Result:** SUCCESSFUL

**Verification Source:** Owner-reported device verification

**Application State:** Installed and operational on the development device

The successful Build #49 checkpoint represents a verified implementation
milestone in the current Guardian development history.

This statement records the current project evidence supplied through the
development workflow. It is not an independent third-party certification.

---

## 12. Current Architecture Checkpoint

At this provenance checkpoint, the following architecture components have
been established or implemented within the project:

- AZIMI Core
- Atlas Core
- Atlas Requirement Engine
- Atlas Local Engine
- Atlas Router
- Atlas Availability
- Atlas Provider contract
- Atlas Provider Registry
- Cloudflare Atlas Provider
- AZIMI AI Client
- AZIMI Network layer
- AZIMI Authentication
- Guardian Storage
- Vault Crypto
- Atlas Memory Store
- Atlas Owner Authority
- Guardian Diagnostics Startup
- Z Security foundations
- Z Language foundations
- Z Vault foundation
- Z Recovery foundation
- Z Shield foundation
- Z Connect foundation
- Z Control
- Z Cloud architecture
- Z Launcher architecture
- Z Lab
- Z Origin owner-space foundation

Components may continue to evolve as development progresses.

This record describes the checkpoint and does not freeze the architecture.

---

## 13. Owner Authority Boundary

The AZIMI architecture distinguishes between:

### Owner Identity

Persistent recognition of the project's creator and owner.

### Active Authorization

Temporary permission to access protected owner functionality.

Remembered owner identity does not automatically grant active protected
authority.

Active authorization may be revoked while owner identity remains recorded.

This separation is intended to preserve continuity without weakening
authorization boundaries.

---

## 14. Atlas Session Principle

After successful owner authentication for an authorized Atlas session,
Atlas should remain available throughout that active authorized session.

Atlas should not repeatedly require owner authentication for every message
or normal use.

The authorized session ends when one of the following occurs:

- the owner explicitly locks Atlas
- the owner explicitly exits the protected session
- Guardian security policy requires re-authentication
- the application/session terminates according to its security lifecycle

This preserves both usability and explicit security boundaries.

---

## 15. Backup Requirement

AZIMI follows a minimum three-copy backup model:

1. Active Project
2. Separate Independent Backup
3. Owner-Controlled Archive

Restore testing is required.

The goal is to ensure that project continuity does not depend on a single
provider, account, device, deployment, database, or storage location.

---

## 16. Evidence Register

The following evidence categories are recognized for this record.

### Source Evidence

- Project repository
- Version history
- Commit history
- Source files
- Architecture documentation

### Build Evidence

- Successful Guardian Build #49
- Build logs
- APK artifact
- Build metadata

### Device Verification Evidence

- Successful APK installation
- Guardian application launch
- Guardian feature verification
- Security-state verification

### Architecture Evidence

- Ownership Registry
- Atlas architecture files
- Guardian architecture files
- Provider-independence implementation
- Memory protection implementation
- Security boundary implementation

---

## 17. Evidence References

The following evidence references are intentionally left available for
future attachment.

### Git Commit

`[COMMIT_SHA_TO_BE_RECORDED]`

### APK SHA-256

`[APK_SHA256_TO_BE_RECORDED]`

### Build Artifact Reference

`[BUILD_ARTIFACT_REFERENCE_TO_BE_RECORDED]`

### Verification Screenshot References

`[SCREENSHOT_REFERENCE_1]`

`[SCREENSHOT_REFERENCE_2]`

`[SCREENSHOT_REFERENCE_3]`

### Independent Backup Reference

`[BACKUP_REFERENCE_TO_BE_RECORDED]`

Evidence references must be added only when the corresponding evidence
actually exists.

No evidence identifier should be invented.

---

## 18. Ownership Registry Relationship

The authoritative ownership framework for the AZIMI ecosystem is maintained
in:

`OWNERSHIP-REGISTRY.json`

This provenance record supplements that registry.

It does not replace the Ownership Registry.

The Ownership Registry remains the primary internal declaration of:

- ownership
- owner authority
- asset classes
- access levels
- provider independence
- backup policy
- memory policy
- change control
- audit requirements
- governance

---

## 19. Change Control

Protected AZIMI changes follow the defined change-control sequence:

1. IDENTITY
2. AUTHORIZATION
3. CHANGE
4. AUDIT
5. BACKUP
6. VERIFICATION

Protected architecture should not be silently replaced, transferred, or
removed without the required authorization and verification process.

---

## 20. Recovery Principle

The authoritative project source remains the project repository together
with independent backups and owner-controlled archives.

Recovery planning must preserve:

- source code
- architecture
- documentation
- configuration knowledge
- approved project memory
- provenance records
- ownership records
- verification history

Recovery must not depend exclusively on one external provider.

---

## 21. Security Principle

AZIMI follows a protected-boundary model.

Protected information must remain inside its authorized security boundary.

Atlas may coordinate approved information.

Guardian enforces device-side boundaries.

Z Vault protects owner-controlled information.

External providers provide replaceable infrastructure or intelligence
services.

No external provider is granted final ownership authority.

---

## 22. Continuity Principle

AZIMI development is intended to continue from established architecture
rather than repeatedly restarting the project.

Existing:

- decisions
- architecture
- source code
- milestones
- verification records
- approved project context
- recovery information
- provenance records

should remain recoverable and traceable through the project's version and
backup systems.

---

## 23. Current Status

**Ownership Registry:** ACTIVE

**Provenance Record:** ACTIVE

**AZIMI Guardian:** ACTIVE

**Atlas:** FOUNDATION / ACTIVE DEVELOPMENT

**Z Vault:** FOUNDATION

**Z Recovery:** FOUNDATION

**Z Shield:** FOUNDATION

**Z Connect:** FOUNDATION

**Z Cloud:** FOUNDATION

**Z Launcher:** FOUNDATION

**Z Lab:** FOUNDATION

**Z Origin:** FOUNDATION

**Provider Independence:** ENABLED BY ARCHITECTURAL DESIGN

**Memory Protection:** ENABLED BY POLICY AND IMPLEMENTATION

**Build #49:** SUCCESSFUL

**Device Installation:** VERIFIED BY OWNER

**Independent Evidence Attachments:** PENDING

---

## 24. Record Integrity

This document is intended to be version-controlled with the AZIMI project.

Future modifications should be traceable through version history and the
AZIMI change-control process.

The record should not contain:

- passwords
- API keys
- authentication tokens
- recovery codes
- private credentials
- biometric secrets
- other secret authentication material

---

## 25. Owner Declaration

This record documents the AZIMI project's internal provenance and ownership
context as maintained by its creator.

**Creator:** Zaman Azimi

**Owner:** Zaman Azimi

**Role:** FOUNDER_CREATOR_OWNER

**Final Authority:** Zaman Azimi

**Registry:** AZI-OWNERSHIP-ROOT

**Record:** AZI-PROVENANCE-001

**Status:** ACTIVE

---

## 26. Verification Signature Record

**Owner:** Zaman Azimi

**Verification Date:** 2026-09-22

**Verification Method:** Project/build/device review

**Verification Status:** RECORDED

**Independent Third-Party Certification:** NOT CLAIMED

---

# End of AZIMI Provenance Record
