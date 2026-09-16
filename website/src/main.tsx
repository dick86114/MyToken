// Ensure window.fetch has both getter and setter so external proxies/scripts do not throw TypeError
try {
  const globalObj = typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : null);
  if (globalObj) {
    let currentFetch = globalObj.fetch;
    const desc = Object.getOwnPropertyDescriptor(globalObj, 'fetch');
    if (!desc || !desc.set) {
      Object.defineProperty(globalObj, 'fetch', {
        get() {
          return currentFetch;
        },
        set(newFetch) {
          currentFetch = newFetch;
        },
        configurable: true,
        enumerable: true,
      });
    }
  }
} catch {
  // Ignore
}

import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
