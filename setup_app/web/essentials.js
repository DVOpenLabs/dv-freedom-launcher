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
    title.textContent = kind === 'support' ? 'Check your setup.' : 'A few useful extras.';
    body.replaceChildren(element('p', 'Loading…'));
    if (!dialog.open) dialog.showModal();
    try {
      const response = await fetch('/api/' + kind, {headers: {'X-Setup-Token': document.querySelector('meta[name="setup-token"]').content}});
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
        const filterLabel = element('label', 'Show extras for '), filter = document.createElement('select');
        filter.setAttribute('aria-label', 'Filter extras by platform');
        for (const [value, text] of [['android', 'Android TV / Google TV'], ['lg', 'Native LG'], ['tizen', 'Native Samsung']]) {
          const option = element('option', text); option.value = value; filter.append(option);
        }
        filterLabel.append(filter); body.append(filterLabel);
        const list = element('div', '', 'extras-list'); body.append(list);
        function render() {
          list.replaceChildren();
          const items = data.items.filter(item => item.platforms.includes(filter.value));
          if (!items.length) list.append(element('p', 'Native platform companions are still under review. Android add-ons cannot be installed here.'));
          for (const item of items) {
            const row = element('section', '', 'essential-row');
            const link = element('a', item.name + ' ↗'); link.href = item.url; link.target = '_blank'; link.rel = 'noopener noreferrer';
            const heading = document.createElement('h3'); heading.append(link);
            row.append(heading, element('p', item.summary), element('small', item.access)); list.append(row);
          }
        }
        filter.onchange = render; render();
      }
    } catch (error) { if (generation === current) body.replaceChildren(element('p', error.message, 'error')); }
  }
  const mobile = element('button', 'Help & extras', 'help-link essentials-mobile');
  mobile.onclick = () => {
    title.textContent = 'What would help?'; body.replaceChildren();
    for (const [kind, label] of [['support', 'Setup health & support'], ['extras', 'Explore optional extras']]) {
      const button = element('button', label, 'help-link'); button.onclick = () => show(kind); body.append(button);
    }
    if (!dialog.open) dialog.showModal();
  };
  document.querySelector('.topbar').append(mobile);
  const host = document.querySelector('.sidebar-bottom');
  for (const [kind, label] of [['support', 'Setup health & support'], ['extras', 'Explore optional extras']]) {
    const button = element('button', label, 'help-link'); button.onclick = () => show(kind); host.append(button);
  }
})();
