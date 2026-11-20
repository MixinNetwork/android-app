const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    path.join(__dirname, '../../main/java/one/mixin/android/ui/web/BotSigningBridge.kt'),
    'utf8',
);
const script = source.match(/internal val SCRIPT = """([\s\S]*?)"""\.trimIndent\(\)/)[1];

function page() {
    const sent = [];
    const window = {
        MixinContext: { getContext: () => 'native-context' },
        _MixinBotSigning: { postMessage: value => sent.push(JSON.parse(value)) },
    };
    window.top = window;
    return { window, sent };
}

test('keeps the client signature API and boolean reload flags', () => {
    const { window, sent } = page();
    vm.runInNewContext(script, { window });
    const args = ['app-id', false, 'POST', '/transactions', '{}', 'callbacks.signed'];
    window.MixinContext.signBotSignature(args);
    window.MixinContext.signBotSignature(args.map((value, index) => index === 1 ? true : value));
    assert.deepEqual(sent, [args, ['app-id', true, 'POST', '/transactions', '{}', 'callbacks.signed']]);
    assert.equal(window.MixinContext.getContext(), 'native-context');
});

test('keeps string reload flags and additional client arguments', () => {
    const { window, sent } = page();
    vm.runInNewContext(script, { window });
    const args = ['app-id', 'false', 'GET', '/me', '', 'callbacks.signed', 'extra'];
    window.MixinContext.signBotSignature(args);
    assert.deepEqual(sent, [args]);
});

test('does not mutate the client request', () => {
    const { window, sent } = page();
    vm.runInNewContext(script, { window });
    const args = Object.freeze(['app-id', false, 'POST', '/transactions', '{}', 'signed']);
    window.MixinContext.signBotSignature(args);
    assert.deepEqual(sent, [args]);
});

test('does not install the signing shim in an iframe', () => {
    const { window, sent } = page();
    window.top = {};
    vm.runInNewContext(script, { window });
    assert.equal(window.MixinContext.signBotSignature, undefined);
    assert.deepEqual(sent, []);
});

test('does not expose a signing fallback when the native channel is unavailable', () => {
    const { window } = page();
    delete window._MixinBotSigning;
    vm.runInNewContext(script, { window });
    assert.equal(window.MixinContext.signBotSignature, undefined);
    assert.equal(window.MixinContext.getContext(), 'native-context');
});

test('does not install the signing shim without the native context', () => {
    const { window, sent } = page();
    delete window.MixinContext;
    vm.runInNewContext(script, { window });
    assert.equal(window.MixinContext, undefined);
    assert.deepEqual(sent, []);
});
