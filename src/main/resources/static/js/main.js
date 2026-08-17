import { sendChatMessage } from './api.js';
import { hideWelcomeScreen, appendUserMessage, showLoadingAnimation, removeLoadingAnimation, appendAIMessage, appendErrorMessage } from './ui.js';

const messageInput = document.getElementById('promptInput');
const sendButton = document.getElementById('sendButton');

async function handleSendMessage() {

    const message = messageInput.value.trim();
    if (!message) return;

    // 1. Arayüzü güncelle (UI)
    hideWelcomeScreen();
    appendUserMessage(message);
    messageInput.value = ""; // Gönderdikten sonra input'u temizle

    const loadingId = "loading-" + Date.now();
    showLoadingAnimation(loadingId);

    // 2. Sunucuya İstek At (API)
    try {
        const responseText = await sendChatMessage(message);
        removeLoadingAnimation(loadingId);
        appendAIMessage(responseText);
    } catch (error) {
        removeLoadingAnimation(loadingId);
        appendErrorMessage();
    }
}

// Olay Dinleyicileri (Event Listeners)
sendButton.addEventListener("click", handleSendMessage);

messageInput.addEventListener("keypress", (event) => {
    if (event.key === "Enter") {
        event.preventDefault(); // Enter'a basınca sayfanın yenilenmesini engeller
        handleSendMessage();
    }
});