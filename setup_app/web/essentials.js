'use strict';
(() => {
  const dialog = document.querySelector('#help-dialog'), body = document.querySelector('#help-body');
  const title = document.querySelector('#help-title');
  let generation = 0;
  function element(tag, text, className) {
    const node = document.createElement(tag); node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  async function show(kind) {
    const current = ++generation;
    title.textContent = kind === 'support' ? 'Check your setup.' : 'Add-on Center.';
    body.replaceChildren(element('p', 'Loading…'));
    if (!dialog.open) dialog.showModal();
    try {
      const response = await fetch('/api/' + (kind === 'extras' ? 'addons' : kind), {headers: {'X-Setup-Token': document.querySelector('meta[name="setup-token"]').content}});
      if (!response.ok) throw new Error('Could not load this view. Reopen Setup and try again.');
      const data = await response.json();
      if (generation !== current || !dialog.open) return;
      body.replaceChildren();
      if (kind === 'support') {
        body.append(element('p', 'These checks stay on this computer. The downloadable report omits private device details.'));
        for (const check of data.checks) {
          const row = element('section', '', 'essential-row');
          row.append(element('h3', check.name), element('span', check.status.replaceAll('-', ' '), 'essential-status'), element('p', check.next));
          body.append(row);
        }
        body.append(element('p', data.privacy));
        const button = element('button', 'Download shareable report', 'primary');
        button.onclick = () => {
          const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'}));
          const link = document.createElement('a'); link.href = url; link.download = 'dv-freedom-support.json'; link.click();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
        };
        body.append(button);
      } else {
        body.append(element('p', data.note));
        const list = element('div', '', 'addon-list'); body.append(list);
        const status = element('p', '', 'addon-status'); status.setAttribute('role', 'status'); body.append(status);
        async function action(path, value) {
          const r = await fetch('/api/addon-' + path, {method: 'POST', headers: {'Content-Type':'application/json', 'X-Setup-Token':document.querySelector('meta[name="setup-token"]').content}, body:JSON.stringify(value)});
          const result = await r.json(); if (!r.ok) throw new Error(result.error || 'This action could not finish.'); return result;
        }
        for (const item of data.items) {
          const row = element('section', '', 'essential-row addon-row'), info = element('div');
          info.append(element('h3', item.name), element('p', item.summary));
          const release = item.release;
          info.append(element('small', release.available ? release.versionName + ' · ' + Math.ceil(release.bytes/1024) + ' KB · signed offline app' : release.message));
          const review = element('button', 'Review installation', 'secondary'); review.disabled = !data.connected || !release.available;
          review.onclick = async () => {
            for (const button of list.querySelectorAll('button')) button.disabled = true;
            status.textContent = 'Reading this TV and saving a recovery record. No installation yet…';
            try {
              const reviewed = await action('review', {id:item.id});
              const detail = element('section', '', 'addon-review');
              detail.append(element('h3', reviewed.operation + ' ' + item.name), element('p', 'Home and wallpaper stay saved. This does not enable optional access, services or connections.'), element('p', reviewed.note));
              const access = element('ul'); for (const permission of reviewed.release.permissions) access.append(element('li', ({'android.permission.INTERNET':'Network access for optional connections', 'android.permission.FOREGROUND_SERVICE':'Keep an explicitly enabled feature running', 'android.permission.FOREGROUND_SERVICE_SPECIAL_USE':'Run the user-started remote or schedule service', 'android.permission.RECEIVE_BOOT_COMPLETED':'Resume a schedule you enabled after restart', 'android.permission.SCHEDULE_EXACT_ALARM':'Exact timers — separate Android approval', 'android.permission.SYSTEM_ALERT_WINDOW':'Warning overlays — separate Android approval'}[permission] || permission)));
              detail.append(element('h4', 'Declared Android permissions'), access, element('small', 'Private recovery record: ' + reviewed.recoveryPath));
              const install = element('button', reviewed.operation + ' reviewed app', 'primary'), cancel = element('button', 'Cancel', 'secondary');
              cancel.onclick = () => show('extras');
              install.onclick = async () => {
                install.disabled = true; cancel.disabled = true; status.textContent = 'Installing and checking the signed APK. Keep Setup open…';
                try { const result = await action('install', {id:reviewed.id, reviewToken:reviewed.reviewToken}); status.textContent = result.message; detail.replaceChildren(element('h3', item.name + ' is ready'), element('p', result.message));
                  const open = element('button', 'Open on TV to configure', 'secondary'); open.onclick = async () => {try {const result = await action('open', {id:item.id});status.textContent=result.message;}catch(e){status.textContent=e.message;}};
                  const back = element('button', 'Back to Add-on Center', 'secondary');back.onclick=()=>show('extras');detail.append(open,back);open.focus();
                } catch(error) {status.textContent=error.message;cancel.disabled=false;}
              };
              detail.append(install, cancel); list.replaceChildren(detail); status.textContent='Review the access above. Install is a separate action.'; install.focus();
            } catch(error) {status.textContent=error.message;for(const button of list.querySelectorAll('button'))button.disabled=false;}
          };
          row.append(info, review); list.append(row);
        }
        if (!data.connected) status.textContent='Connect to an Android TV or Google TV Streamer to review an installation. No apps can be installed in preview mode.';
      }
    } catch (error) { if (generation === current) body.replaceChildren(element('p', error.message, 'error')); }
  }
  const mobile = element('button', 'Help & extras', 'help-link essentials-mobile');
  mobile.onclick = () => {
    title.textContent = 'What would help?'; body.replaceChildren();
    for (const [kind, label] of [['support', 'Setup health & support'], ['extras', 'Add-on Center']]) {
      const button = element('button', label, 'help-link'); button.onclick = () => show(kind); body.append(button);
    }
    if (!dialog.open) dialog.showModal();
  };
  document.querySelector('.topbar').append(mobile);
  const host = document.querySelector('.sidebar-bottom');
  for (const [kind, label] of [['support', 'Setup health & support'], ['extras', 'Add-on Center']]) {
    const button = element('button', label, 'help-link'); button.onclick = () => show(kind); host.append(button);
  }
})();
