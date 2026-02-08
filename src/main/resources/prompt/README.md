# Prompt Resources

This directory contains **system prompts** used by AI services in this project.

Prompts define the **behavior, role, and output format** of the LLM and are treated
as part of the application’s business logic rather than plain text.

---

## Directory Structure

```text
prompt/
├── codegen/
│   ├── html-file.txt
│   ├── multi-file.txt
│   └── quality-check.txt
├── image/
│   ├── collection-system.txt
│   └── collection-plan.txt
├── chat/
│   └── assistant-system.txt
└── README.md
```

## Prompt Descriptions

#### `html-file.txt`

- System prompt for single-file HTML code generation.
  Used when the AI is expected to return a complete HTML page.
- Mapped to: `@SystemMessage(fromResource = "prompt/codegen/html-file.txt")`

#### `multi-file.txt`

- System prompt for multi-file code generation (HTML / CSS / JS or other structures).
  Used when the AI should output structured, multi-file results.
- Mapped to: `@SystemMessage(fromResource = "prompt/codegen/multi-file.txt")`

## Design Notes

- Prompts are version-controlled and loaded from the classpath.
- Prompts are considered AI business rules, not UI text.
- Changes to prompts may directly affect system behavior and output quality.

## Conventions

- One prompt file = one clear responsibility
- Prefer explicit instructions to implicit assumptions
- Keep prompts deterministic and reproducible when possible
