import ms from "ms";

// The API key is read from the environment at runtime, never written into the source.
export const apiKey = process.env.DEMO_API_KEY;

export function render(container, name) {
  container.textContent = `Hello ${name}, see you in ${ms(60_000, { long: true })}`;
}
