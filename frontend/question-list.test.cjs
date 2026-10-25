const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const vm = require("node:vm");
const source = readFileSync(`${__dirname}/question-list.js`, "utf8");

function setup(fetch) {
  const handlers = {};
  const list = {
    children: [], setAttribute() {},
    replaceChildren(...items) { this.children = items; },
  };
  const status = {};
  const button = { addEventListener: (_, fn) => { handlers.refresh = fn; } };
  const form = { addEventListener: (_, fn) => { handlers.submitted = fn; } };
  const elements = { "question-list": list, "questions-status": status,
    "questions-refresh": button, "question-form": form };
  vm.runInNewContext(source, {
    document: { getElementById: (id) => elements[id], createElement: () => ({}) }, fetch,
  });
  return { list, status, button, handlers };
}

const settle = () => new Promise((resolve) => setImmediate(resolve));
const response = (questions) => ({ ok: true, json: async () => questions });

test("loads questions initially and renders untrusted text as plain text", async () => {
  const text = '<img src=x onerror="alert(1)">';
  const ui = setup(async (url, options) => {
    assert.equal(url, "/api/questions");
    assert.equal(options.cache, "no-store");
    return response([{ id: 1, text }]);
  });
  await settle();
  assert.equal(ui.list.children.length, 1);
  assert.equal(ui.list.children[0].textContent, text);
  assert.equal(ui.list.children[0].innerHTML, undefined);
  assert.equal(ui.button.disabled, false);
});

test("shows an empty-state message", async () => {
  const ui = setup(async () => response([]));
  await settle();
  assert.equal(ui.list.children.length, 0);
  assert.match(ui.status.textContent, /No questions yet/);
});

test("refresh and successful submission reload the list without duplicates", async () => {
  let questions = [];
  const ui = setup(async () => response(questions));
  await settle();
  questions = [{ text: "New question" }];
  await ui.handlers.submitted();
  assert.equal(ui.list.children[0].textContent, "New question");
  await ui.handlers.refresh();
  assert.equal(ui.list.children.length, 1);
});

test("failed refresh preserves existing questions and allows retry", async () => {
  let fail = false;
  const ui = setup(async () => {
    if (fail) throw new Error("Offline");
    return response([{ text: "Saved question" }]);
  });
  await settle();
  fail = true;
  await ui.handlers.refresh();
  assert.equal(ui.list.children[0].textContent, "Saved question");
  assert.match(ui.status.textContent, /could not be refreshed/);
  assert.equal(ui.button.disabled, false);
  fail = false;
  await ui.handlers.refresh();
  assert.equal(ui.status.textContent, "");
});

for (const bad of [{ ok: false }, response({}), { ok: true, json: async () => { throw new Error(); } }]) {
  test("handles an invalid server response", async () => {
    const ui = setup(async () => bad);
    await settle();
    assert.match(ui.status.textContent, /could not be refreshed/);
    assert.equal(ui.button.disabled, false);
  });
}

test("an older request cannot overwrite a newer list", async () => {
  let resolveOld;
  let calls = 0;
  const ui = setup(() => {
    if (calls++ === 0) return new Promise((resolve) => { resolveOld = resolve; });
    return Promise.resolve(response([{ text: "Latest" }]));
  });
  await ui.handlers.submitted();
  resolveOld(response([{ text: "Old" }]));
  await settle();
  assert.equal(ui.list.children[0].textContent, "Latest");
});
