import _ from "lodash";

// Deliberately vulnerable. The key below is random and fake: it exists so the secrets scan has
// something to find. It is not a credential for any service.
export const api_key = "MiY9aLC8icA7F3T31x8sVqY9qZLWuyxhTcCcAP0I";

export function render(container, name) {
  // XSS: untrusted input written into the page as HTML.
  container.innerHTML = "Hello " + _.trim(name);
}
