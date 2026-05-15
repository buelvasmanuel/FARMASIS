const chatId = "session-" + Math.random().toString(36).substring(2, 9);

function toggleChat() {
    const container = document.getElementById('farma-chat-container');
    const fab = document.getElementById('farma-fab');
    
    if (container.classList.contains('active')) {
        container.classList.remove('active');
        setTimeout(() => { container.style.display = 'none'; }, 300); // match transition
    } else {
        container.style.display = 'flex';
        // forced reflow
        void container.offsetWidth;
        container.classList.add('active');
        document.getElementById('farma-input').focus();
    }
}

function handleEnter(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

async function sendMessage() {
    const inputField = document.getElementById('farma-input');
    const message = inputField.value.trim();
    
    if (message === '') return;
    
    inputField.value = '';
    
    appendMessage(message, 'user');
    
    const typingId = appendTypingIndicator();

    try {
        const payload = {
            chatId: chatId,
            message: message
        };

        const response = await fetch('/api/chat/ask', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error('Error en la respuesta del servidor');
        }

        const data = await response.json();
        
        removeElement(typingId);
        
        appendMessage(data.response, 'ai');
        
    } catch (error) {
        console.error("Error:", error);
        removeElement(typingId);
        appendMessage("Disculpa, tuve un problema al procesar tu solicitud. Intenta de nuevo más tarde.", 'ai');
    }
}

function formatMarkdown(text) {
    // Basic Markdown formatting
    return text
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/\n/g, '<br>')
        .replace(/- (.*?)<br>/g, '<ul><li>$1</li></ul>') // Basic lists support
        .replace(/<\/ul><ul>/g, ''); // Fix adjacent lists
}

function appendMessage(text, sender) {
    const chatBody = document.getElementById('farma-chat-body');
    
    const bubble = document.createElement('div');
    bubble.className = `chat-bubble ${sender}`;
    
    const label = document.createElement('div');
    label.className = 'bubble-label';
    label.innerText = sender === 'user' ? 'Tú' : 'FarmaBot IA';
    
    const content = document.createElement('div');
    content.innerHTML = formatMarkdown(text);
    
    bubble.appendChild(label);
    bubble.appendChild(content);
    
    chatBody.appendChild(bubble);
    scrollToBottom();
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
    chatBody.scrollTo({
        top: chatBody.scrollHeight,
        behavior: 'smooth'
    });
}
