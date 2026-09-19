# AZIMI MASTER BUILD PLAN

## Project Identity

**Project Name:** AZIMI Ecosystem

**Founder and Builder:** Zaman Azimi

**Repository:** zamanazimi100-bit/azimi-studio

**Development Approach:** Phone-first, security-conscious, verification-driven technology development.

**Current Date:** 2026-09-19

---

# 1. Vision

AZIMI is a personal technology ecosystem designed to help its creator build, test, organize and improve useful digital tools.

The long-term goal is to develop strong practical skills in:

- Software development
- Artificial intelligence
- Automation
- Cybersecurity defense
- Data systems
- Android development
- Web development
- Microsoft technologies
- Cloud technologies
- Productivity systems

AZIMI is not only a website or a single application.

It is a continuously developing technology workstation and portfolio.

---

# 2. North Star

Build real things.

Test them honestly.

Document progress.

Protect private information.

Improve through consistent work.

Never confuse an idea, prototype, implementation, and verified feature.

---

# 3. Core Principles

## 3.1 Verification Before Completion

A feature is not complete merely because:

- Code was written.
- A button exists.
- A build started.
- An APK was generated.
- A deployment succeeded.
- A screen looks correct.

A feature is complete only after:

1. Implementation.
2. Testing.
3. Real verification.
4. Documentation.
5. Commit and backup.

---

## 3.2 Security by Design

AZIMI must prioritize:

- Privacy.
- Defensive security.
- Secure authentication.
- Safe data handling.
- Minimal permissions.
- Clear user control.
- Recovery planning.
- Protection against accidental data loss.

---

## 3.3 Phone-First Development

The current development environment is an Android phone.

All practical workflows should support:

- GitHub mobile editing.
- Cloud-based builds.
- Browser-based testing.
- Mobile-friendly documentation.
- Remote development tools.
- Simple copy-and-paste workflows.

---

## 3.4 No Fake Features

AZIMI must never claim to provide a capability that has not actually been implemented and verified.

Unfinished features must be clearly labeled:

- PLANNED
- IN DEVELOPMENT
- NOT YET VERIFIED
- LIMITED
- DISABLED

---

# 4. AZIMI Ecosystem Modules

## 4.1 AZIMI Home

The central entry point for the ecosystem.

Responsibilities:

- Display ecosystem modules.
- Show system status.
- Provide navigation.
- Present important alerts.
- Connect to other AZIMI components.

---

## 4.2 AZIMI Icon System

A consistent visual identity for AZIMI modules.

Planned modules include:

- Z Control
- Z Vault
- Z Recovery
- Z Shield
- AZIMI AI
- Z Lab
- AZIMI Cloud
- AZIMI Workbench

---

## 4.3 AZIMI Guardian

The Android security and device utility application.

Planned responsibilities:

- Device information.
- Storage diagnostics.
- Battery information.
- Memory diagnostics.
- Vault access.
- Recovery tools.
- Defensive security tools.
- AI assistance.
- Local system utilities.

---

## 4.4 Z Vault

A protected area for private user data and future secure storage features.

Security requirements:

- Start locked.
- Require authentication.
- Never expose private data without authorization.
- Lock again when requested.
- Avoid storing secrets in logs.
- Handle cancelled authentication safely.
- Clearly communicate lock state.

---

## 4.5 Z Recovery

Recovery and continuity tools.

Planned responsibilities:

- Backup guidance.
- Restore guidance.
- Recovery checklists.
- Important project documentation.
- Emergency access information.
- Safe recovery workflows.

---

## 4.6 Z Shield

Defensive privacy and security tools.

Possible future capabilities:

- Security status information.
- Permission awareness.
- Network safety guidance.
- Privacy checks.
- Defensive monitoring.
- Safe configuration recommendations.

No harmful or unauthorized capabilities should be added.

---

## 4.7 AZIMI AI

The AI assistance layer of the ecosystem.

Planned responsibilities:

- Project planning.
- Coding assistance.
- Documentation assistance.
- Troubleshooting.
- Productivity support.
- Safe memory of relevant project context.

AZIMI AI must never store:

- Passwords.
- API keys.
- Verification codes.
- Recovery codes.
- Private authentication secrets.

---

## 4.8 Z Lab

An experimental environment for testing ideas.

Possible experiments:

- AI tools.
- Automation.
- Data processing.
- Interface prototypes.
- Android utilities.
- Cloud integrations.

Experiments must be labeled separately from stable features.

---

# 5. Current Development Phase

## PHASE 1 — AZIMI GUARDIAN FOUNDATION

Current component:

**Z Vault**

Current objective:

**Harden and verify the Vault authentication and unlock process.**

Current status:

**IN DEVELOPMENT — NOT YET VERIFIED**

---

# 6. Required Development Workflow

```text
BUILD
↓
TEST
↓
VERIFY
↓
DOCUMENT
↓
COMMIT
↓
UPDATE CURRENT STATE
↓
NEXT TASK
# 8. Current Guardian Architecture

## Android Application

Package:

`com.azimi.guardian`

The Guardian application is being developed as the Android-side foundation of the AZIMI ecosystem.

Current areas include:

- Z Control
- Z Vault
- Z Recovery
- Z Shield
- AZIMI AI
- Z Lab

---

# 9. Z Vault Authentication Plan

The Vault authentication flow must provide a clear locked/unlocked state.

## Required Flow

### Initial State

Vault must start locked.

### Unlock

When the user requests access:

1. Android authentication is requested.
2. The user authenticates.
3. Successful authentication unlocks the Vault.
4. The Vault interface becomes available.

### Cancelled Authentication

If authentication is cancelled or fails:

- Vault remains locked.
- Private content must remain inaccessible.
- The application must remain stable.

### Lock

When the user locks the Vault:

- Vault returns to the locked state.
- Protected content becomes inaccessible again.
- A new authentication attempt is required for access.

---

# 10. Vault Testing Checklist

The following tests must be performed on the real Android device.

## Test 1 — Initial Lock

Expected:

`LOCKED`

Result:

**NOT YET VERIFIED**

---

## Test 2 — Authentication Prompt

Expected:

Android device authentication prompt appears.

Result:

**NOT YET VERIFIED**

---

## Test 3 — Successful Authentication

Expected:

Successful authentication unlocks the Vault.

Result:

**NOT YET VERIFIED**

---

## Test 4 — Cancelled Authentication

Expected:

Cancelling authentication keeps the Vault locked.

Result:

**NOT YET VERIFIED**

---

## Test 5 — Lock Action

Expected:

Using the lock action returns the Vault to the locked state.

Result:

**NOT YET VERIFIED**

---

# 11. Guardian Diagnostics

## Current Diagnostics

Implemented foundation:

- Device information.
- Battery information.
- Storage information.

## RAM Diagnostics

Current status:

**PLANNED**

The next development task after Vault verification is to implement real RAM and memory diagnostics.

The implementation must use actual Android system information rather than fabricated or placeholder values.

---

# 12. Backup and Recovery

Backup and restore are important parts of the AZIMI continuity strategy.

Current status:

**NOT COMPLETE**

Future backup systems should distinguish between:

- Public project files.
- Application data.
- User data.
- Configuration.
- Sensitive credentials.

Private credentials must never be placed into public GitHub files.

---

# 13. Security Architecture

AZIMI security is defensive by design.

Security priorities include:

1. Authentication.
2. Authorization.
3. Data protection.
4. Privacy.
5. Recovery.
6. Safe defaults.
7. Minimal permissions.
8. Clear user control.

Security features must be tested on the actual target environment whenever possible.

---

# 14. Secrets Policy

The following must never be committed to the public repository:

- Passwords.
- API keys.
- Authentication tokens.
- Verification codes.
- Recovery codes.
- Private keys.
- Session secrets.
- Personal authentication credentials.

If a secret is accidentally exposed, it should be treated as compromised and replaced.

Documentation may describe how secrets are handled without containing the actual secrets.

---

# 15. Web and Cloud Components

AZIMI includes web and cloud components connected to the broader ecosystem.

Current technologies include:

- HTML.
- CSS.
- JavaScript.
- Next.js components.
- Vercel.
- Android/Kotlin.
- API endpoints.
- Cloud services.

Each component must be verified independently.

A successful deployment does not automatically mean every feature works correctly.

---

# 16. AZIMI AI Safety

AZIMI AI should assist with:

- Coding.
- Planning.
- Documentation.
- Troubleshooting.
- Project organization.
- Productivity.

AI memory should prioritize useful project context while protecting sensitive information.

The system must not intentionally retain secrets such as:

- Passwords.
- API keys.
- Verification codes.
- Recovery codes.
- Authentication tokens.

---

# 17. Project Documentation

The `docs` directory is the continuity layer for the project.

Core documentation files:

- `AZIMI_MASTER_BUILD_PLAN.md`
- `CURRENT_STATE.md`
- `DECISION_LOG.md`
- `CHANGELOG.md`

These files should remain consistent with the actual repository state.

If implementation changes, documentation should eventually be updated.

---

# 18. Change Management

Before modifying an important component:

1. Identify the current implementation.
2. Understand the existing behavior.
3. Make the smallest safe change.
4. Build.
5. Test.
6. Verify.
7. Document.
8. Commit.

Avoid replacing working code blindly.

---

# 19. Release Discipline

An APK or deployment should not be called a final release merely because it builds.

Release status should be explicit:

- BUILDING
- BUILT
- TESTING
- VERIFIED
- RELEASE CANDIDATE
- RELEASED

The actual status must be based on evidence.

---

# 20. Current Next Task

The immediate development priority is:

**Complete Z Vault authentication testing on the real Android device.**

After Vault verification:

**Implement real RAM and memory diagnostics.**

After that, continue through the Guardian foundation roadmap.

---

# 21. Continuity Rule

If a future conversation begins without the previous context, the repository documentation should provide enough information to understand:

- What AZIMI is.
- What has been built.
- What has been verified.
- What remains unfinished.
- What the current task is.
- What the next task should be.

The repository must never be used to hide unfinished work.

---

# 22. Master Build Principle

AZIMI grows through verified progress.

```text
IDEA
↓
DESIGN
↓
IMPLEMENT
↓
BUILD
↓
TEST
↓
VERIFY
↓
DOCUMENT
↓
COMMIT
↓
BACKUP
↓
IMPROVE
# 23. Guardian Roadmap

## Phase 1 — Foundation

Current focus:

- Z Vault authentication
- Device diagnostics
- Storage diagnostics
- Battery information
- Basic Guardian architecture

Status:

**IN DEVELOPMENT**

---

## Phase 2 — Diagnostics

Planned:

- Real RAM diagnostics
- Memory usage information
- Storage health information
- Battery diagnostics
- System information improvements

---

## Phase 3 — Recovery

Planned:

- Backup workflows
- Restore workflows
- Recovery documentation
- Project continuity tools

---

## Phase 4 — Defensive Security

Planned:

- Security status
- Permission awareness
- Privacy checks
- Network safety guidance
- Defensive monitoring

---

## Phase 5 — AI Integration

Planned:

- AZIMI AI integration
- Safe project memory
- Coding assistance
- Troubleshooting assistance
- Productivity workflows

---

## Phase 6 — Ecosystem Integration

Planned:

- AZIMI Cloud
- AZIMI Workbench
- Connect features
- Web integration
- Cross-component workflows

---

# 24. Backup Strategy

The AZIMI repository is the primary project source of truth.

Important project states should be preserved through:

- Git commits
- Repository history
- Documentation
- Release artifacts
- Appropriate backups

Before major architectural changes, the existing state should be preserved whenever practical.

---

# 25. Testing Standard

Testing should use the real target environment whenever possible.

For Android features:

- Test on the actual Android device.
- Record expected behavior.
- Record actual behavior.
- Record failures.
- Do not hide limitations.

For web features:

- Verify the deployed application.
- Test important interactions.
- Check API behavior separately.
- Confirm production behavior rather than relying only on source code.

---

# 26. Failure Handling

When something fails:

1. Stop assuming the feature works.
2. Record the observed error.
3. Identify the affected component.
4. Fix the smallest necessary part.
5. Rebuild.
6. Test again.
7. Document the result.

A failed test is useful information.

It should not be represented as a successful feature.

---

# 27. Versioning

Major AZIMI components should use clear version identifiers when appropriate.

Example:

- Guardian v1
- Guardian v1.1
- Guardian v2

Version changes should correspond to meaningful changes in functionality or architecture.

---

# 28. Documentation Integrity

Documentation must describe the actual project state.

If documentation becomes outdated:

1. Identify the outdated information.
2. Verify the current implementation.
3. Update the documentation.
4. Commit the correction.

Documentation is part of the engineering process, not an advertisement.

---

# 29. Project Quality Goals

AZIMI should continuously improve in:

- Reliability
- Security
- Privacy
- Performance
- Usability
- Maintainability
- Documentation
- Recovery
- Accessibility
- Professional presentation

Improvements should be based on tested results rather than assumptions.

---

# 30. Long-Term Direction

AZIMI is intended to become a strong personal technology ecosystem and portfolio demonstrating practical engineering ability.

Long-term areas of development include:

- Artificial intelligence
- Software engineering
- Android development
- Web development
- Automation
- Data systems
- Cloud systems
- Defensive security
- Microsoft technologies

The ecosystem should demonstrate actual work and measurable progress.

---

# 31. Final Project Rule

AZIMI must always distinguish between:

**IDEA**

Something planned.

**PROTOTYPE**

An early implementation.

**IMPLEMENTED**

Code or functionality exists.

**TESTED**

A test has been performed.

**VERIFIED**

The expected behavior has been confirmed.

**RELEASED**

The verified feature has been intentionally published.

These states must never be treated as interchangeable.

---

# 32. Current Truth

As of 2026-09-19:

**AZIMI Guardian is under active development.**

**Z Vault authentication is implemented but not yet fully verified on the real Android device.**

**Real RAM diagnostics are not yet implemented.**

**Backup and restore are not complete.**

**Z Shield/VPN functionality is not active.**

**AZIMI AI integration requires separate verification.**

The next exact engineering task remains:

**Verify Z Vault authentication on the real Android device.**

---

# 33. Build Path

```text
DOCUMENTATION
↓
VAULT HARDENING
↓
VAULT TESTING
↓
VAULT VERIFICATION
↓
RAM DIAGNOSTICS
↓
RECOVERY
↓
DEFENSIVE SECURITY
↓
AI INTEGRATION
↓
ECOSYSTEM INTEGRATION
↓
CONTINUOUS IMPROVEMENT
---

# 34. Closing Principle

AZIMI is built one verified step at a time.

No shortcuts.

No fake completion.

No hidden failures.

Build it.

Test it.

Verify it.

Document it.

Commit it.

Then move forward.
