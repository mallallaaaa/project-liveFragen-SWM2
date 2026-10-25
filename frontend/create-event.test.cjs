const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const vm = require("node:vm");
const source = readFileSync(`${__dirname}/create-event.js`, "utf8");

function setup(value, fetch) {
  let onSubmit;
  const input = {
    value, disabled: false, attributes: {}, focus() {},
    setAttribute(name, value) { this.attributes[name] = value; },
    removeAttribute(name) { delete this.attributes[name]; },
  };
  const button = { disabled: false };
  const status = { textContent: "", hidden: true };
  const elements = {
    "event-form": { addEventListener: (_, handler) => { onSubmit = handler; } },
    "event-title": input, "event-submit": button, "event-status": status,
  };
  vm.runInNewContext(source, { document: { getElementById: (id) => elements[id] }, fetch });
  return { input, button, status, submit: () => onSubmit({ preventDefault() {} }) };
}

for (const value of ["", " \t\n ", "\u2003", "a".repeat(201)]) {
  test(`rejects invalid title of length ${value.length} without a request`, async () => {
    const ui = setup(value, () => assert.fail("Invalid event sent"));
    await ui.submit();
    assert.match(ui.status.textContent, /Please enter|200 characters/);
    assert.equal(ui.status.hidden, false);
    assert.equal(ui.input.attributes["aria-invalid"], "true");
    assert.equal(ui.input.value, value);
  });
}

test("posts trimmed JSON and confirms event title and ID as plain text", async () => {
  const title = '<img src=x onerror="alert(1)">';
  const ui = setup(`  ${title}  `, async (url, options) => {
    assert.equal(url, "/api/events");
    assert.equal(options.method, "POST");
    assert.equal(options.headers["Content-Type"], "application/json");
    assert.deepEqual(JSON.parse(options.body), { title });
    return { ok: true, json: async () => ({ id: 42, title }) };
  });
  await ui.submit();
  assert.equal(ui.status.textContent, `Event “${title}” created successfully. Event ID: 42.`);
  assert.equal(ui.status.innerHTML, undefined);
  assert.equal(ui.input.value, "");
  assert.equal(ui.button.disabled, false);
  assert.equal(ui.input.disabled, false);
});

test("accepts a title at the 200-character boundary", async () => {
  const title = "a".repeat(200);
  const ui = setup(title, async () => ({ ok: true, json: async () => ({ id: 1, title }) }));
  await ui.submit();
  assert.match(ui.status.textContent, /created successfully/);
});

for (const [name, fetch, message] of [
  ["API validation error", async () => ({ ok: false, json: async () => ({ message: "Please enter an event title." }) }), /Please enter/],
  ["server error", async () => ({ ok: false, json: async () => { throw new Error(); } }), /Please try again/],
  ["null error body", async () => ({ ok: false, json: async () => null }), /Please try again/],
  ["network error", async () => { throw new Error(); }, /could not be confirmed/],
  ["unreadable success response", async () => ({ ok: true, json: async () => { throw new Error(); } }), /could not be confirmed/],
  ["invalid success response", async () => ({ ok: true, json: async () => ({}) }), /could not be confirmed/],
]) {
  test(`${name} preserves the title and re-enables submission`, async () => {
    const ui = setup("My lecture", fetch);
    await ui.submit();
    assert.match(ui.status.textContent, message);
    assert.equal(ui.input.value, "My lecture");
    assert.equal(ui.button.disabled, false);
    assert.equal(ui.input.disabled, false);
  });
}

test("prevents duplicate submissions while a request is pending", async () => {
  let resolve;
  let calls = 0;
  const ui = setup("My lecture", () => {
    calls++;
    return new Promise((done) => { resolve = done; });
  });
  const pending = ui.submit();
  assert.equal(ui.button.disabled, true);
  assert.equal(ui.input.disabled, true);
  await ui.submit();
  assert.equal(calls, 1);
  resolve({ ok: true, json: async () => ({ id: 1, title: "My lecture" }) });
  await pending;
  assert.equal(ui.button.disabled, false);
});
