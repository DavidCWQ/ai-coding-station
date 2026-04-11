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

- System prompt for the **哲学大儒** agent (`life_advisor`): classical / Confucian-style dialogue, self-cultivation, and life reflection.
- Display name and persona can evolve with curated corpora and RAG (e.g. a dedicated character voice); not medical or crisis counseling.

#### `tax_assistant.txt`

- System prompt defining the behavior of the **tax assistant** agent.
- Focuses on一般性的税务说明与示例（不构成法律或财务建议，具体问题请咨询专业人士）。

---

## Design Notes

- Prompts are version-controlled and loaded from the classpath.
- Prompts are considered AI business rules, not UI text.
- Changes to prompts may directly affect system behavior and output quality.

## Conventions

- One prompt file = one clear responsibility.
- Prefer explicit instructions to implicit assumptions.
- Keep prompts deterministic and reproducible when possible.
