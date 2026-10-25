// Runs the compat shims BEFORE the PDF.js worker bundle (static imports evaluate in
// order) and re-exports the worker's message handler, so the very same file works
//  - as the Worker script, and
//  - as the module PDF.js imports for its main-thread "fake worker" fallback.
import './pdf.compat.mjs';
export { WorkerMessageHandler } from './pdf.worker.min.mjs';
