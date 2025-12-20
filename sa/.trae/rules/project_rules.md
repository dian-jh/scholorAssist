# 🏗 Project Rules

## 1. Type Safety & TypeScript
- **Strict Mode**: Always use strict mode in TypeScript.
- **Type-Only Imports**: Use `import type { ... }` for interfaces and types to avoid circular dependency issues and build errors (Vite/Rollup).
  - BAD: `import { NoteDTO } from '@/types/note'`
  - GOOD: `import type { NoteDTO } from '@/types/note'`
- **Explicit Types**: Define return types for functions and prop types for components.
- **No `any`**: Avoid `any` whenever possible. Use `unknown` or specific interfaces.

## 2. API & Data Handling
- **DTO Alignment**: Frontend interfaces MUST match backend DTOs exactly.
- **Error Handling**: Use global interceptors for auth errors (401). Handle business errors (400, 500) gracefully in UI.
- **Response Wrapping**: All API responses are wrapped in `Result<T>`. Frontend should unwrap `data` properly.

## 3. Component Structure
- **Composition API**: Use `<script setup lang="ts">`.
- **Props/Emits**: Define typed props and emits.
- **Style Isolation**: Use `<style scoped>`.

## 4. State Management
- **Pinia**: Use Pinia for global state (User, Categories).
- **Persistence**: Persist auth tokens and necessary user preferences.

## 5. File Naming
- **Components**: PascalCase (e.g., `DocumentList.vue`).
- **Composables**: camelCase with `use` prefix (e.g., `useDocumentData.ts`).
- **Utils/Helpers**: camelCase.
