    import { sendChatMessage } from './api.js';
    
    import {
        hideWelcomeScreen,
        appendUserMessage,
        showLoadingAnimation,
        removeLoadingAnimation,
        appendAIMessage,
        appendErrorMessage
    } from './ui.js';
    
    const DARK_MODE_KEY = "aipage_dark_mode";
    
    
    const messageInput =
        document.getElementById('promptInput');
    
    const sendButton =
        document.getElementById('sendButton');
    
    
    // =========================================================
    // SON ARAMALAR
    // =========================================================
    
    const RECENT_SEARCH_KEY =
        "aipage_recent_searches";
    
    const MAX_RECENT_SEARCHES = 8;
    
    
    // =========================================================
    // SON ARAMALARI YÜKLE
    // =========================================================
    
    function loadRecentSearches() {
    
        try {
    
            return JSON.parse(
                localStorage.getItem(
                    RECENT_SEARCH_KEY
                )
            ) || [];
    
        } catch (error) {
    
            console.error(
                "Son aramalar yüklenemedi:",
                error
            );
    
            return [];
        }
    }
    
    
    // =========================================================
    // SON ARAMAYI KAYDET
    // =========================================================
    
    function saveRecentSearch(message) {
    
        let searches =
            loadRecentSearches();
    
    
        // Aynı arama varsa kaldır
        searches =
            searches.filter(
                item =>
                    item.toLowerCase() !==
                    message.toLowerCase()
            );
    
    
        // En başa ekle
        searches.unshift(message);
    
    
        // Maksimum kayıt
        searches =
            searches.slice(
                0,
                MAX_RECENT_SEARCHES
            );
    
    
        localStorage.setItem(
            RECENT_SEARCH_KEY,
            JSON.stringify(searches)
        );
    
    
        renderRecentSearches();
    }
    
    
    // =========================================================
    // SON ARAMALARI GÖSTER
    // =========================================================
    
    function renderRecentSearches() {
    
        const container =
            document.getElementById(
                "recentSearches"
            );
    
    
        if (!container) {
            return;
        }
    
    
        const searches =
            loadRecentSearches();
    
    
        container.innerHTML = "";
    
    
        if (searches.length === 0) {
    
            container.innerHTML = `
                <div class="
                    text-xs
                    text-gray-400
                    px-2.5
                    py-2
                ">
                    Henüz arama yok
                </div>
            `;
    
            return;
        }
    
    
        searches.forEach((search) => {
    
            const button =
                document.createElement("button");
    
    
            button.type = "button";
    
    
            button.className = `
                w-full
                text-left
                p-2.5
                rounded-lg
                text-xs
                text-gray-600
                hover:bg-white
                hover:text-[#2B2E72]
                flex
                items-center
                gap-2
                transition-all
                duration-200
                truncate
            `;
    
    
            button.innerHTML = `
                <i class="
                    fa-solid
                    fa-magnifying-glass
                    text-[10px]
                    text-gray-400
                    flex-shrink-0
                "></i>
    
                <span class="truncate"></span>
            `;
    
    
            button
                .querySelector("span")
                .textContent = search;
    
    
            button.addEventListener(
                "click",
                () => {
    
                    messageInput.value =
                        search;
    
                    handleSendMessage();
                }
            );
    
    
            container.appendChild(button);
    
        });
    }
    
    
    // =========================================================
    // PROMPT BAR
    // =========================================================
    
    const promptBar =
        document.querySelector(
            '.prompt-bar'
        );
    
    let thinkingTimer = null;
    
    
    // =========================================================
    // MESAJ GÖNDER
    // =========================================================
    
    async function handleSendMessage() {
    
        const message =
            messageInput.value.trim();
    
    
        if (
            !message ||
            sendButton.disabled
        ) {
            return;
        }
    
    
        if (thinkingTimer) {
    
            clearTimeout(
                thinkingTimer
            );
    
            thinkingTimer = null;
        }
    
    
        // Son aramaya ekle
        saveRecentSearch(message);
    
    
        // Kullanıcı mesajı
        hideWelcomeScreen();
    
        appendUserMessage(message);
    
    
        messageInput.value = "";
    
        sendButton.disabled = true;
    
    
        // =====================================================
        // PROMPT BAR
        // =====================================================
    
        if (promptBar) {
    
            promptBar.classList.remove(
                'sending',
                'thinking',
                'completed'
            );
    
            promptBar.classList.add(
                'sending'
            );
    
    
            thinkingTimer =
                setTimeout(() => {
    
                    promptBar.classList.remove(
                        'sending'
                    );
    
                    promptBar.classList.add(
                        'thinking'
                    );
    
                    thinkingTimer = null;
    
                }, 300);
        }
    
    
        // =====================================================
        // LOADING
        // =====================================================
    
        const loadingId =
            "loading-" + Date.now();
    
    
        showLoadingAnimation(
            loadingId
        );
    
    
        try {
    
            const responseText =
                await sendChatMessage(
                    message
                );
    
    
            // Timer temizle
            if (thinkingTimer) {
    
                clearTimeout(
                    thinkingTimer
                );
    
                thinkingTimer = null;
            }
    
    
            removeLoadingAnimation(
                loadingId
            );
    
    
            appendAIMessage(
                responseText
            );
    
    
            // =================================================
            // CEVAP GELDİ
            // =================================================
    
            if (promptBar) {
    
                promptBar.classList.remove(
                    'sending',
                    'thinking'
                );
    
                promptBar.classList.add(
                    'completed'
                );
    
    
                setTimeout(() => {
    
                    promptBar.classList.remove(
                        'completed'
                    );
    
                }, 500);
            }
    
    
        } catch (error) {
    
            console.error(
                "Chat hatası:",
                error
            );
    
    
            if (thinkingTimer) {
    
                clearTimeout(
                    thinkingTimer
                );
    
                thinkingTimer = null;
            }
    
    
            removeLoadingAnimation(
                loadingId
            );
    
    
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
    
    
    // =========================================================
    // SAYFA AÇILINCA SON ARAMALAR
    // =========================================================
    
    renderRecentSearches();
    
    // =========================================================
    // DARK MODE
    // =========================================================
    
    const darkModeToggle =
        document.getElementById("darkModeToggle");
    
    const darkModeIcon =
        document.getElementById("darkModeIcon");
    
    const darkModeText =
        document.getElementById("darkModeText");
    
    
    function updateDarkModeUI(isDark) {
    
        if (darkModeIcon) {
    
            darkModeIcon.className =
                isDark
                    ? "fa-solid fa-sun w-4"
                    : "fa-solid fa-moon w-4";
        }
    
    
        if (darkModeText) {
    
            darkModeText.textContent =
                isDark
                    ? "Aydınlık Mod"
                    : "Karanlık Mod";
        }
    }
    
    
    function setDarkMode(isDark) {
    
        document.documentElement.classList.toggle(
            "dark",
            isDark
        );
    
        localStorage.setItem(
            DARK_MODE_KEY,
            isDark ? "true" : "false"
        );
    
        updateDarkModeUI(isDark);
    }
    
    
    const savedDarkMode =
        localStorage.getItem(DARK_MODE_KEY);
    
    setDarkMode(
        savedDarkMode === "true"
    );
    
    
    if (darkModeToggle) {
    
        darkModeToggle.addEventListener(
            "click",
            () => {
    
                const isDark =
                    document.documentElement.classList.contains(
                        "dark"
                    );
    
                setDarkMode(!isDark);
    
            }
        );
    
    }