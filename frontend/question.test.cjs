const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const vm = require("node:vm");
const script = readFileSync(`${__dirname}/question.js`, "utf8");

function setup(value, fetch, search = "?eventId=42") {
  let onSubmit;
  const events = [];
  const elements = {
    "question-form": {
      addEventListener: (_, handler) => { onSubmit = handler; },
      dispatchEvent(event) { events.push(event.type); },
    },
    "question-input": {
      value, disabled: false, focus() {},
      setAttribute() {}, removeAttribute() {},
    },
    "question-submit": { disabled: false },
    "question-status": { textContent: "", hidden: true },
    "question-event-heading": {},
  };
  vm.runInNewContext(script, {
    document: { getElementById: (id) => elements[id] }, fetch, Event, URLSearchParams,
    window: { location: { search } },
  });
  return {
    input: elements["question-input"], button: elements["question-submit"],
    status: elements["question-status"],
    events,
    submit: () => onSubmit({ preventDefault() {} }),
  };
}

for (const value of ["", " \t\n ", "a".repeat(501)]) {
  test(`rejects invalid input of length ${value.length} without a request`, async () => {
    const ui = setup(value, () => assert.fail("Invalid question sent"));
    await ui.submit();
    assert.match(ui.status.textContent, /Please enter|500 characters/);
    assert.equal(ui.status.hidden, false);
    assert.equal(ui.input.value, value);
  });
}

test("posts trimmed JSON, confirms success and clears the field", async () => {
  const ui = setup("  My question?  ", async (url, options) => {
    assert.equal(url, "/api/questions");
    assert.equal(options.method, "POST");
    assert.equal(options.headers["Content-Type"], "application/json");
    assert.deepEqual(JSON.parse(options.body), { text: "My question?", eventId: 42 });
    return { ok: true };
  });
  await ui.submit();
  assert.match(ui.status.textContent, /successfully/);
  assert.equal(ui.input.value, "");
  assert.equal(ui.button.disabled, false);
  assert.equal(ui.input.disabled, false);
  assert.deepEqual(ui.events, ["question-submitted"]);
});

for (const [name, fetch, message] of [
  ["API error", async () => ({ ok: false, json: async () => ({ message: "Question is too long" }) }), /Question is too long/],
  ["non-JSON server error", async () => ({ ok: false, json: async () => { throw new Error(); } }), /Please try again/],
  ["network error", async () => { throw new Error(); }, /Cannot reach the server/],
]) {
  test(`${name} shows a message and preserves input for retry`, async () => {
    const ui = setup("My question?", fetch);
    await ui.submit();
    assert.match(ui.status.textContent, message);
    assert.equal(ui.input.value, "My question?");
    assert.equal(ui.button.disabled, false);
    assert.equal(ui.input.disabled, false);
    assert.deepEqual(ui.events, []);
  });
}

test("prevents duplicate submissions while the request is pending", async () => {
  let resolve;
  let calls = 0;
  const ui = setup("My question?", () => {
    calls++;
    return new Promise((done) => { resolve = done; });
  });
  const pending = ui.submit();
  assert.equal(ui.button.disabled, true);
  assert.equal(ui.input.disabled, true);
  await ui.submit();
  assert.equal(calls, 1);
  resolve({ ok: true });
  await pending;
  assert.equal(ui.button.disabled, false);
});

for (const search of ["", "?eventId=0", "?eventId=abc", "?eventId=1&eventId=2"]) {
  test(`prevents submitting without an unambiguous event: ${search}`, async () => {
    const ui = setup("Question", () => assert.fail("Invalid event submitted"), search);
    await ui.submit();
    assert.equal(ui.input.disabled, true);
    assert.equal(ui.button.disabled, true);
    assert.match(ui.status.textContent, /valid event link/);
  });
}
