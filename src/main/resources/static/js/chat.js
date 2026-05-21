const chatId = "session-" + Math.random().toString(36).substring(2, 9);

// 🎵 Efectos de sonido generados sintéticamente (sin archivos externos)
const playPop = (freq = 400, type = 'sine', duration = 0.1) => {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(freq, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(0.01, ctx.currentTime + duration);
        gain.gain.setValueAtTime(0.1, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + duration);
    } catch(e) {}
};

// 🌞 Saludo dinámico al iniciar la página
document.addEventListener('DOMContentLoaded', () => {
    const greetingEl = document.getElementById('dynamic-greeting');
    if (greetingEl) {
        const hour = new Date().getHours();
        if (hour < 12) greetingEl.innerText = 'Buenos días';
        else if (hour < 18) greetingEl.innerText = 'Buenas tardes';
        else greetingEl.innerText = 'Buenas noches';
    }
});

function toggleChat() {
    const container = document.getElementById('farma-chat-container');
    
    if (container.classList.contains('active')) {
        container.classList.remove('active');
        setTimeout(() => { container.style.display = 'none'; }, 300);
    } else {
        playPop(600, 'sine', 0.15); // Sonido al abrir
        container.style.display = 'flex';
        void container.offsetWidth;
        container.classList.add('active');
        document.getElementById('farma-input').focus();
    }
}

function handleEnter(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        sendMessage(event);
    }
}

function sendQuickReply(text) {
    const qr = document.getElementById('quick-replies');
    if (qr) qr.style.display = 'none';
    document.getElementById('farma-input').value = text;
    sendMessage(null);
}

function clearChat() {
    const chatBody = document.getElementById('farma-chat-body');
    const children = Array.from(chatBody.children);
    children.forEach((child, index) => {
        if (index > 0) child.remove(); 
    });
    const qr = document.getElementById('quick-replies');
    if (qr) {
        qr.style.display = 'flex';
        if (!chatBody.contains(qr)) chatBody.appendChild(qr);
    } else {
        const newQr = document.createElement('div');
        newQr.className = 'quick-replies';
        newQr.id = 'quick-replies';
        newQr.innerHTML = `
            <button class="quick-reply-btn" onclick="sendQuickReply('Ver inventario de productos')">Ver inventario</button>
            <button class="quick-reply-btn" onclick="sendQuickReply('Revisar las ventas del día')">Ventas de hoy</button>
            <button class="quick-reply-btn" onclick="sendQuickReply('Mostrar productos con bajo stock')">Bajo stock</button>
            <button class="quick-reply-btn" onclick="sendQuickReply('Lista de proveedores activos')">Proveedores</button>
            <button class="quick-reply-btn" onclick="sendQuickReply('Consultar ofertas y descuentos')">Ofertas activas</button>
        `;
        chatBody.appendChild(newQr);
    }
}

async function sendMessage(event) {
    if (event) event.preventDefault();
    
    const inputField = document.getElementById('farma-input');
    const message = inputField.value.trim();
    if (message === '') return;
    inputField.value = '';
    
    const qr = document.getElementById('quick-replies');
    if (qr) qr.style.display = 'none';
    
    appendMessage(message, 'user');
    const typingId = appendTypingIndicator();

    try {
        const response = await fetch('/api/chat/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ chatId: chatId, message: message })
        });

        removeElement(typingId);

        if (!response.ok) {
            appendMessage(response.status === 429 ? '⏳ El servicio está ocupado. Intenta de nuevo en unos segundos.' : '⚠️ Error del servidor. Intenta de nuevo más tarde.', 'ai');
            return;
        }

        const data = await response.json();
        // true = activar efecto máquina de escribir
        appendMessage(data.response, 'ai', true);
        
    } catch (error) {
        removeElement(typingId);
        appendMessage('⚠️ Error de conexión o red. Intenta de nuevo.', 'ai');
    }
}

function getTimeString() {
    const now = new Date();
    return now.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
}

// 📊 Procesamiento Avanzado de Markdown con Tablas
function formatMarkdown(text) {
    if (!text) return '';
    let html = text
        .replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/^### (.*$)/gm, '<strong style="font-size:1rem;">$1</strong>')
        .replace(/^## (.*$)/gm, '<strong style="font-size:1.05rem;">$1</strong>')
        .replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code style="background:#e2e8f0;padding:1px 5px;border-radius:4px;font-size:0.85em;">$1</code>')
        .replace(/\n/g, '<br>')
        .replace(/- (.*?)<br>/g, '<li style="margin-left:15px;list-style:disc;">$1</li>')
        .replace(/(\d+)\. (.*?)<br>/g, '<li style="margin-left:15px;list-style:decimal;">$2</li>');
        
    // Parseo de tablas rudimentario para Markdown
    if (html.includes('|')) {
        let inTable = false;
        let rows = html.split('<br>');
        for(let i=0; i<rows.length; i++) {
            let row = rows[i].trim();
            if(row.startsWith('|') && row.endsWith('|')) {
                if(!inTable) { rows[i] = '<table class="farma-table">' + row; inTable = true; }
                let cells = row.split('|').filter(c => c !== '');
                if(row.includes('---')) {
                    rows[i] = ''; // omitir fila divisora
                } else if(i > 0 && rows[i-1].includes('<table')) { // Header
                    rows[i] = '<tr>' + cells.map(c => `<th>${c.trim()}</th>`).join('') + '</tr>';
                } else {
                    rows[i] = '<tr>' + cells.map(c => `<td>${c.trim()}</td>`).join('') + '</tr>';
                }
            } else if(inTable) {
                rows[i-1] += '</table>';
                inTable = false;
            }
        }
        if(inTable) rows[rows.length-1] += '</table>';
        html = rows.join('<br>').replace(/<br><table/g, '<table').replace(/<\/table><br>/g, '</table>');
    }
    return html;
}

function appendMessage(text, sender, typewriter = false) {
    const chatBody = document.getElementById('farma-chat-body');
    
    // 🎵 Emitir sonido según emisor
    if(sender === 'ai') playPop(800, 'triangle', 0.1); 
    else playPop(400, 'sine', 0.05); 
    
    const bubble = document.createElement('div');
    bubble.className = `chat-bubble ${sender}`;
    
    const label = document.createElement('div');
    label.className = 'bubble-label';
    label.innerText = sender === 'user' ? 'Tú' : 'FarmaBot IA';
    
    const content = document.createElement('div');
    const formattedHtml = formatMarkdown(text);
    
    const timeEl = document.createElement('div');
    timeEl.className = 'msg-time';
    timeEl.innerText = getTimeString();
    
    bubble.appendChild(label);
    bubble.appendChild(content);
    bubble.appendChild(timeEl);
    chatBody.appendChild(bubble);
    
    // ✍️ Efecto de Máquina de Escribir
    if (typewriter && sender === 'ai') {
        typeWriterHTML(content, formattedHtml, () => scrollToBottom());
    } else {
        content.innerHTML = formattedHtml;
        scrollToBottom();
    }
}

// Lógica para escribir HTML progresivamente
function typeWriterHTML(element, htmlString, onTick) {
    element.innerHTML = '';
    const tokens = [];
    let currentToken = '';
    let inTag = false;
    for(let i=0; i<htmlString.length; i++) {
        const char = htmlString[i];
        if(char === '<') {
            if(currentToken && !inTag) tokens.push({type: 'text', val: currentToken});
            currentToken = '<';
            inTag = true;
        } else if(char === '>') {
            currentToken += '>';
            tokens.push({type: 'html', val: currentToken});
            currentToken = '';
            inTag = false;
        } else {
            if(inTag) currentToken += char;
            else tokens.push({type: 'text', val: char});
        }
    }
    if(currentToken && !inTag) tokens.push({type: 'text', val: currentToken});
    
    let index = 0;
    let buffer = '';
    function typeNext() {
        if(index < tokens.length) {
            const token = tokens[index];
            if(token.type === 'html') {
                buffer += token.val;
                index++;
                typeNext(); // saltar delays en tags html
            } else {
                buffer += token.val;
                element.innerHTML = buffer; // el navegador auto-cierra los tags pendientes!
                index++;
                if(onTick) onTick();
                setTimeout(typeNext, 12); // velocidad (12ms)
            }
        } else {
            element.innerHTML = htmlString; // asegurar el HTML final
        }
    }
    typeNext();
}

function appendTypingIndicator() {
    const chatBody = document.getElementById('farma-chat-body');
    const typingId = 'typing-' + Date.now();
    
    const bubble = document.createElement('div');
    bubble.id = typingId;
    bubble.className = 'chat-bubble ai';
    
    const label = document.createElement('div');
    label.className = 'bubble-label';
    label.innerText = 'FarmaBot IA';
    
    const indicator = document.createElement('div');
    indicator.className = 'typing-indicator';
    indicator.innerHTML = `
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
    `;
    
    bubble.appendChild(label);
    bubble.appendChild(indicator);
    
    chatBody.appendChild(bubble);
    scrollToBottom();
    return typingId;
}

function removeElement(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function scrollToBottom() {
    const chatBody = document.getElementById('farma-chat-body');
    chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: 'smooth' });
}
