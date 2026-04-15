# Prompt Resources

This directory contains **system prompts** used by AI services in this project.

Prompts define the **behavior, role, and output format** of the LLM and are treated
as part of the application’s business logic rather than plain text.

---

## Directory Structure

```text
prompt/
├── agent/
│   ├── code_assistant.txt
│   ├── life_advisor.txt
│   └── tax_assistant.txt
├── codegen/
│   ├── html-file.txt
│   └── multi-file.txt
└── README.md
```

## Prompt Descriptions

### Code Generation (`codegen/`)

#### `html-file.txt`

- System prompt for single-file HTML code generation.
- Used when the AI is expected to return a complete HTML page.
- Mapped to: `@SystemMessage(fromResource = "prompt/codegen/html-file.txt")` in `AICodeGeneratorService`.

#### `multi-file.txt`

- System prompt for multi-file code generation (HTML / CSS / JS or other structures).
- Used when the AI should output structured, multi-file results.
- Mapped to: `@SystemMessage(fromResource = "prompt/codegen/multi-file.txt")` in `AICodeGeneratorService`.

### Agent Chat (`agent/`)

#### `code_assistant.txt`

- System prompt defining the behavior of the **coding assistant** agent.
- Focuses on programming help, code explanation, and implementation guidance.

#### `life_advisor.txt`

- System prompt for the **life_advisor** persona, inspired by the character **Jingchun Qi** (literary fiction).
- Focuses on Confucian-style dialogue, layered subtext, and life reflection; integrates external ideas when compatible with that ethos.
- Optional per-chapter RAG markdown: conventions in `resources/rag/docs/life_advisor/chapters/README.md`; blank chapter skeleton in `resources/rag/doc-templates/life_advisor_chapter.md` (outside ingest glob).

#### `tax_assistant.txt`

- System prompt defining the behavior of the **财税助理** (tax assistant) agent.
- Focuses on tax illustration and calculation。

---

## Design Notes

- Prompts are version-controlled and loaded from the classpath.
- Prompts are considered AI business rules, not UI text.
- Changes to prompts may directly affect system behavior and output quality.

## Conventions

- One prompt file = one clear responsibility.
- Prefer explicit instructions to implicit assumptions.
- Keep prompts deterministic and reproducible when possible.
