/*
 * Accordio · reglas de lint del worker.
 *
 * Pocas y elegidas: esto no viene a discutir comillas ni sangrías, viene a
 * cazar lo que rompe en producción y no se ve leyendo. El ejemplo es reciente:
 * renombrar una función y dejar la llamada vieja. `node --check` lo daba por
 * bueno —la sintaxis era correcta— y solo habría fallado al abrir la web.
 * `no-undef` lo caza antes de salir de la máquina.
 *
 * Formato plano (`eslint.config.mjs`), que es el de ESLint 9.
 */

// Lo que el runtime de Cloudflare Workers ya pone en la mesa. Se declara a mano
// para no arrastrar el paquete `globals` por catorce nombres.
const globalesWorker = {
  fetch: "readonly",
  Request: "readonly",
  Response: "readonly",
  Headers: "readonly",
  URL: "readonly",
  URLSearchParams: "readonly",
  crypto: "readonly",
  caches: "readonly",
  console: "readonly",
  TextEncoder: "readonly",
  TextDecoder: "readonly",
  btoa: "readonly",
  atob: "readonly",
  structuredClone: "readonly",
  setTimeout: "readonly",
  clearTimeout: "readonly",
};

// Los guiones de comprobación y los tests corren en Node, no en el Worker.
const globalesNode = {
  ...globalesWorker,
  process: "readonly",
  Buffer: "readonly",
  globalThis: "readonly",
};

export default [
  {
    // `chords-db.js` son datos generados: una sola línea de 600 KB que no hay
    // que leer ni arreglar a mano.
    ignores: ["node_modules/**", "src/chords-db.js"],
  },
  {
    files: ["src/**/*.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: globalesWorker,
    },
    rules: {
      // Las dos que de verdad importan aquí.
      "no-undef": "error",
      "no-unused-vars": ["error", { argsIgnorePattern: "^_" }],

      // Errores silenciosos clásicos.
      "no-constant-condition": "error",
      "no-dupe-keys": "error",
      "no-unreachable": "error",
      "no-fallthrough": "error",
      // Un `catch {}` vacío es a veces deliberado (ver `readJson` en api.js),
      // así que se permite ese caso concreto y no el resto.
      "no-empty": ["error", { allowEmptyCatch: true }],
    },
  },
  {
    files: ["test/**/*.mjs", "*.mjs"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: globalesNode,
    },
    rules: {
      "no-undef": "error",
      "no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
    },
  },
];
