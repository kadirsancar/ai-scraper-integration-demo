const chatBox = document.getElementById('chatBox');
const welcomeScreen = document.getElementById('welcomeScreen');

export function hideWelcomeScreen() {
    if (welcomeScreen) welcomeScreen.style.display = 'none';
}

export function appendUserMessage(message) {
    const msgDiv = document.createElement('div');
    msgDiv.className = "flex justify-end";
    msgDiv.innerHTML = `<div class="bg-gray-100 text-gray-800 rounded-2xl p-4 max-w-[80%]">${message}</div>`;
    chatBox.appendChild(msgDiv);
    scrollToBottom();
}

export function appendAIMessage(message) {
    const msgDiv = document.createElement('div');
    msgDiv.className = "flex justify-start gap-4";
    msgDiv.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-[#2B2E72] flex items-center justify-center flex-shrink-0 shadow-sm">
            <img src="/assets/images/logo.png" class="w-5" alt="AI">
        </div>
        <!-- DİKKAT: En sona whitespace-pre-wrap eklendi -->
        <div class="bg-white border border-gray-200 text-gray-800 rounded-2xl p-4 max-w-[80%] whitespace-pre-wrap">
            ${message}
        </div>`;
    chatBox.appendChild(msgDiv);
    scrollToBottom();
}

export function showLoadingAnimation(id) {
    const loadingDiv = document.createElement('div');
    loadingDiv.id = id;
    loadingDiv.className = "flex justify-start gap-4";
    loadingDiv.innerHTML = `
       <div class="w-8 h-8 rounded-full bg-red-600 flex items-center justify-center flex-shrink-0">
             <img src="/assets/images/logo.png" class="w-5 opacity-50" alt="AI">
        </div>
        <div class="bg-white border border-gray-200 text-gray-800 rounded-2xl p-4 max-w-[80%] flex items-center gap-2">
            <i class="fa-solid fa-circle-notch fa-spin text-gray-400"></i> Düşünüyor...
        </div>`;
    chatBox.appendChild(loadingDiv);
    scrollToBottom();
}

export function removeLoadingAnimation(id) {
    const loadingDiv = document.getElementById(id);
    if (loadingDiv) loadingDiv.remove();
}

export function appendErrorMessage() {
    const msgDiv = document.createElement('div');
    msgDiv.className = "flex justify-start gap-4";
    msgDiv.innerHTML = `<div class="bg-red-50 text-red-600 rounded-2xl p-4 max-w-[80%] border border-red-100">Bir hata oluştu. Lütfen backend bağlantınızı kontrol edin.</div>`;
    chatBox.appendChild(msgDiv);
    scrollToBottom();
}

function scrollToBottom() {
    chatBox.scrollTop = chatBox.scrollHeight;
}