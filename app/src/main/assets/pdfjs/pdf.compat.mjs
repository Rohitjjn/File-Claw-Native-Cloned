/*
 * File Claw: tiny feature shims for the bundled PDF.js 4.10 build.
 * That build assumes a recent Chromium (Promise.withResolvers, Array#toSorted,
 * AbortSignal.any ...). Older Android System WebView builds lack some of these,
 * which made the reader fail silently (endless loading). Every shim is guarded and
 * is a no-op on current WebViews. Imported by the page AND by the worker shim.
 */
const g = globalThis;

if (typeof Promise.withResolvers !== 'function') {
  Promise.withResolvers = function withResolvers() {
    let resolve, reject;
    const promise = new Promise(function (res, rej) { resolve = res; reject = rej; });
    return { promise: promise, resolve: resolve, reject: reject };
  };
}

if (typeof Object.hasOwn !== 'function') {
  Object.hasOwn = function hasOwn(object, key) {
    return Object.prototype.hasOwnProperty.call(object, key);
  };
}

function defineIfMissing(proto, name, fn) {
  if (proto && typeof proto[name] !== 'function') {
    Object.defineProperty(proto, name, { value: fn, writable: true, configurable: true });
  }
}

defineIfMissing(Array.prototype, 'at', function at(index) {
  const n = Math.trunc(index) || 0;
  const i = n < 0 ? this.length + n : n;
  return i < 0 || i >= this.length ? undefined : this[i];
});
defineIfMissing(Array.prototype, 'findLast', function findLast(fn, thisArg) {
  for (let i = this.length - 1; i >= 0; i--) if (fn.call(thisArg, this[i], i, this)) return this[i];
  return undefined;
});
defineIfMissing(Array.prototype, 'findLastIndex', function findLastIndex(fn, thisArg) {
  for (let i = this.length - 1; i >= 0; i--) if (fn.call(thisArg, this[i], i, this)) return i;
  return -1;
});
defineIfMissing(Array.prototype, 'toSorted', function toSorted(compare) {
  return Array.prototype.slice.call(this).sort(compare);
});
defineIfMissing(Array.prototype, 'toReversed', function toReversed() {
  return Array.prototype.slice.call(this).reverse();
});
if (g.Uint8Array) {
  defineIfMissing(Uint8Array.prototype, 'toSorted', function toSorted(compare) {
    return Uint8Array.prototype.slice.call(this).sort(compare);
  });
  defineIfMissing(Uint8Array.prototype, 'toReversed', function toReversed() {
    return Uint8Array.prototype.slice.call(this).reverse();
  });
}
defineIfMissing(String.prototype, 'replaceAll', function replaceAll(search, replacement) {
  if (search instanceof RegExp) {
    if (!search.global) throw new TypeError('replaceAll must be called with a global RegExp');
    return this.replace(search, replacement);
  }
  return this.split(String(search)).join(replacement);
});

if (g.AbortSignal && typeof AbortSignal.any !== 'function') {
  AbortSignal.any = function any(signals) {
    const controller = new AbortController();
    for (const signal of signals) {
      if (signal.aborted) { controller.abort(signal.reason); return controller.signal; }
      signal.addEventListener('abort', function () { controller.abort(signal.reason); }, { once: true });
    }
    return controller.signal;
  };
}

if (typeof g.structuredClone !== 'function') {
  // Enough for pdf.js' plain-data uses (no cycles, no transfer lists).
  g.structuredClone = function structuredClone(value) {
    if (value === undefined) return undefined;
    return JSON.parse(JSON.stringify(value));
  };
}
