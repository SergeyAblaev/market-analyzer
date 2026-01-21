async function initAlertRules() {
    await loadAlertRules();
}

async function loadAlertRules() {
    const res = await fetch('/api/alertrules/all');
    const rules = await res.json();

    const tbody = document.getElementById('rulesTableBody');
    tbody.innerHTML = '';

    rules
        .filter(r => r.id.startsWith('PERCENT_CHANGE'))
        .forEach(rule => {
            const tr = document.createElement('tr');

            tr.innerHTML = `
                <td>${rule.symbol}</td>
                <td>
                    <input type="number"
                           min="1"
                           value="${rule.candles}"
                           id="candles-${rule.id}">
                </td>
                <td>
                    <input type="number"
                           step="0.01"
                           min="0.0001"
                           value="${rule.percent}"
                           id="percent-${rule.id}">
                </td>
                <td>
                    <button class="save" onclick="updateRule('${rule.id}')">Save</button>
                </td>
            `;

            tbody.appendChild(tr);
        });
}

async function updateRule(ruleId) {
    const candlesEl = document.getElementById(`candles-${ruleId}`);
    const percentEl = document.getElementById(`percent-${ruleId}`);
    const statusEl = document.getElementById('rulesStatus');

    const candles = Number(candlesEl.value);
    const percent = Number(percentEl.value);

    /* ========== VALIDATION ========== */
    if (!Number.isInteger(candles) || candles < 1) {
        showStatus('Candles must be integer ≥ 1', 'red');
        candlesEl.focus();
        return;
    }

    if (isNaN(percent) || percent <= 0) {
        showStatus('Percent must be > 0', 'red');
        percentEl.focus();
        return;
    }

    /* ========== UPDATE ========== */
    const url = `/api/alertrules/update?id=${ruleId}&candles=${candles}&percent=${percent}`;

    statusEl.innerText = 'Saving...';
    statusEl.style.color = 'black';

    try {
        const res = await fetch(url, { method: 'PATCH' });

        if (!res.ok) {
            throw new Error('Update failed');
        }

        showStatus(`Rule ${ruleId} updated`, 'green');

        /* ========== AUTO-REFRESH ========== */
        await loadAlertRules();

    } catch (e) {
        showStatus(`Error updating ${ruleId}`, 'red');
    }
}

function showStatus(text, color) {
    const statusEl = document.getElementById('rulesStatus');
    statusEl.innerText = text;
    statusEl.style.color = color;

    setTimeout(() => {
        statusEl.innerText = '';
    }, 3000);
}
