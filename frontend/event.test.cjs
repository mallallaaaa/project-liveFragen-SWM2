const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const vm = require("node:vm");
const source = readFileSync(`${__dirname}/event.js`, "utf8");
const settle = () => new Promise((resolve) => setImmediate(resolve));

function setup(search, fetch) {
  let retry;
  const status = {};
  const event = { hidden: true };
  const title = {};
  const id = {};
  const button = { hidden: true, addEventListener: (_, fn) => { retry = fn; } };
  const elements = { "join-status": status, "joined-event": event,
    "joined-event-title": title, "joined-event-id": id, "join-retry": button };
  vm.runInNewContext(source, {
    document: { getElementById: (id) => elements[id] },
    window: { location: { search } }, URLSearchParams, fetch,
  });
  return { status, event, title, id, button, retry: () => retry() };
}

test("opens the exact event from the link without credentials and displays plain text", async () => {
  const title = '<img src=x onerror="alert(1)">';
  const ui = setup("?eventId=42", async (url, options) => {
    assert.equal(url, "/api/events/42");
    assert.equal(options.cache, "no-store");
    assert.equal(options.headers, undefined);
    return { ok: true, json: async () => ({ id: 42, title }) };
  });
  await settle();
  assert.equal(ui.title.textContent, title);
  assert.equal(ui.title.innerHTML, undefined);
  assert.equal(ui.id.textContent, "Event ID: 42");
  assert.equal(ui.event.hidden, false);
  assert.match(ui.status.textContent, /joined this event/);
  assert.equal(ui.button.hidden, true);
});

for (const search of ["", "?eventId=", "?eventId=0", "?eventId=-1", "?eventId=abc", "?eventId=1&eventId=2"]) {
  test(`rejects invalid link ${search || "without ID"} before calling the API`, async () => {
    const ui = setup(search, () => assert.fail("Invalid event requested"));
    await settle();
    assert.match(ui.status.textContent, /link is invalid/);
    assert.equal(ui.event.hidden, true);
    assert.equal(ui.button.hidden, true);
  });
}

for (const [status, message] of [[404, /could not be found/], [400, /link is invalid/]]) {
  test(`handles HTTP ${status} without showing an event`, async () => {
    const ui = setup("?eventId=42", async () => ({ ok: false, status }));
    await settle();
    assert.match(ui.status.textContent, message);
    assert.equal(ui.event.hidden, true);
    assert.equal(ui.button.hidden, true);
  });
}

test("network failure allows retry with the same event ID", async () => {
  let fail = true;
  const ui = setup("?eventId=42", async (url) => {
    assert.equal(url, "/api/events/42");
    if (fail) throw new Error("Offline");
    return { ok: true, json: async () => ({ id: 42, title: "Lecture" }) };
  });
  await settle();
  assert.match(ui.status.textContent, /Check your connection/);
  assert.equal(ui.button.hidden, false);
  assert.equal(ui.button.disabled, false);
  fail = false;
  await ui.retry();
  assert.equal(ui.event.hidden, false);
  assert.equal(ui.button.hidden, true);
});

for (const response of [
  { ok: false, status: 500 },
  { ok: true, json: async () => { throw new Error("Invalid JSON"); } },
  { ok: true, json: async () => ({ id: 99, title: "Wrong event" }) },
]) {
  test("invalid server response permits retry and does not join an incorrect event", async () => {
    const ui = setup("?eventId=42", async () => response);
    await settle();
    assert.equal(ui.event.hidden, true);
    assert.equal(ui.button.hidden, false);
    assert.match(ui.status.textContent, /Cannot load the event/);
  });
}
