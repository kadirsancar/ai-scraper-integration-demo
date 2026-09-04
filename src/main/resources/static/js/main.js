import { sendChatMessage } from './api.js';

import {
    hideWelcomeScreen,
    appendUserMessage,
    showLoadingAnimation,
    removeLoadingAnimation,
    appendAIMessage,
    appendErrorMessage
} from './ui.js';


const messageInput = document.getElementById('promptInput');
const sendButton = document.getElementById('sendButton');
const promptBar = document.querySelector('.prompt-bar');

let thinkingTimer = null;


async function handleSendMessage() {

    const message = messageInput.value.trim();

    if (!message || sendButton.disabled) {
        return;
    }


    // Önceki timer varsa temizle
    if (thinkingTimer) {
        clearTimeout(thinkingTimer);
        thinkingTimer = null;
    }


    // Kullanıcı mesajını göster
    hideWelcomeScreen();
    appendUserMessage(message);

    messageInput.value = "";
    sendButton.disabled = true;


    // =========================================================
    // PROMPT BAR ANİMASYONU
    // =========================================================

    if (promptBar) {

        // Önce temizle
        promptBar.classList.remove(
            'sending',
            'thinking',
            'completed'
        );

        // Gönderme animasyonu
        promptBar.classList.add('sending');


        // 300ms sonra düşünme animasyonu
        thinkingTimer = setTimeout(() => {

            promptBar.classList.remove('sending');
            promptBar.classList.add('thinking');

            thinkingTimer = null;

        }, 300);
    }


    // Loading
    const loadingId = "loading-" + Date.now();

    showLoadingAnimation(loadingId);


    try {

        const responseText = await sendChatMessage(message);


        // Timer temizle
        if (thinkingTimer) {
            clearTimeout(thinkingTimer);
            thinkingTimer = null;
        }


        removeLoadingAnimation(loadingId);

        appendAIMessage(responseText);


        // =====================================================
        // CEVAP GELDİ
        // =====================================================

        if (promptBar) {

            promptBar.classList.remove(
                'sending',
                'thinking'
            );

            promptBar.classList.add('completed');


            setTimeout(() => {

                promptBar.classList.remove('completed');

            }, 500);
        }


    } catch (error) {

        console.error("Chat hatası:", error);


        if (thinkingTimer) {
            clearTimeout(thinkingTimer);
            thinkingTimer = null;
        }


        removeLoadingAnimation(loadingId);

        appendErrorMessage();


        if (promptBar) {

            promptBar.classList.remove(
                'sending',
                'thinking',
                'completed'
            );
        }


    } finally {

        sendButton.disabled = false;

        messageInput.focus();
    }
}


// =========================================================
// GÖNDER BUTONU
// =========================================================

sendButton.addEventListener(
    "click",
    handleSendMessage
);


// =========================================================
// ENTER
// =========================================================

messageInput.addEventListener(
    "keydown",
    (event) => {

        if (
            event.key === "Enter" &&
            !event.shiftKey
        ) {

            event.preventDefault();

            handleSendMessage();
        }
    }
);