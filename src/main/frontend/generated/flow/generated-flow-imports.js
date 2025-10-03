import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/tabs/src/vaadin-tabs.js';
import '@vaadin/tabs/src/vaadin-tab.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/password-field/src/vaadin-password-field.js';
import '@vaadin/notification/src/vaadin-notification.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'aef80b2739d5cfe3356e28808c341585ceb2cfae89e978f2f3a21f1f4695f8c4') {
    pending.push(import('./chunks/chunk-34762ff98b9f53ba33681f1772fe32fa8daca9b52b2e04f3a5f4a211e8f9137f.js'));
  }
  if (key === '7fa7511197990fae7f1df681a2cd9375abab52f5fa1c70b13d1ccadd962b609b') {
    pending.push(import('./chunks/chunk-34762ff98b9f53ba33681f1772fe32fa8daca9b52b2e04f3a5f4a211e8f9137f.js'));
  }
  if (key === 'fa9a2bff2f4e687782a226a4152ec0cde0b19f2fa4319b195c684c1d4343e0cc') {
    pending.push(import('./chunks/chunk-8abb1e0a0e865361bb7563bd5b091190f106deff21dec6c5b252c8851912c909.js'));
  }
  if (key === '61dba017d0905d4dd5326bc8b18be20640498459678f6cd51a7bd4e714e60dcc') {
    pending.push(import('./chunks/chunk-b6d42b4938ad16e8d7d52a92c17b8c32440123f9a88295056d04fd315a607ed8.js'));
  }
  if (key === '6168868a57ad73b42ce0537ce5386a671c515f56af91eabd27ef48a985edbf05') {
    pending.push(import('./chunks/chunk-34762ff98b9f53ba33681f1772fe32fa8daca9b52b2e04f3a5f4a211e8f9137f.js'));
  }
  if (key === '0a929027ab764d9d79fa081ad7eee9556704b9f69a19dd7d7629f68e906d2c47') {
    pending.push(import('./chunks/chunk-34762ff98b9f53ba33681f1772fe32fa8daca9b52b2e04f3a5f4a211e8f9137f.js'));
  }
  if (key === '15076dff012f500aded7e89ad03bdc106043c8b63d3db002f0bbf00a59f3d273') {
    pending.push(import('./chunks/chunk-180d70c7b32fe884fe2eb22c7f64e7347afcf8adeff6012eecda1f7c40bcc892.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}