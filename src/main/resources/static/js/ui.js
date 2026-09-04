const chatBox = document.getElementById('chatBox');
const welcomeScreen = document.getElementById('welcomeScreen');


// =========================================================
// WELCOME SCREEN
// =========================================================

export function hideWelcomeScreen() {

    if (welcomeScreen) {
        welcomeScreen.style.display = 'none';
    }
}


// =========================================================
// USER MESSAGE
// =========================================================

export function appendUserMessage(message) {

    const msgDiv = document.createElement('div');

    msgDiv.className =
        "flex justify-end chat-message";

    msgDiv.innerHTML = `
        <div class="
            bg-[#2B2E72]
            text-white
            rounded-2xl
            rounded-br-md
            px-4
            py-3
            max-w-[80%]
            shadow-sm
            text-sm
            md:text-base
            break-words
        ">
            ${escapeHtml(message)}
        </div>
    `;

    chatBox.appendChild(msgDiv);

    scrollToBottom();
}


// =========================================================
// AI MESSAGE
// =========================================================

export function appendAIMessage(message) {

    const msgDiv = document.createElement('div');

    msgDiv.className =
        "flex justify-start gap-3 md:gap-4 chat-message w-full";


    // =====================================================
    // MARKDOWN
    // =====================================================

    let formattedMessage;

    try {

        if (typeof marked !== "undefined") {

            formattedMessage =
                marked.parse(message);

        } else {

            formattedMessage =
                escapeHtml(message)
                    .replace(/\n/g, "<br>");

        }

    } catch (error) {

        console.error(
            "Markdown oluşturulamadı:",
            error
        );

        formattedMessage =
            escapeHtml(message)
                .replace(/\n/g, "<br>");
    }


    // =====================================================
    // AI MESSAGE HTML
    // =====================================================

    msgDiv.innerHTML = `

        <!-- AI Avatar -->
        <div class="
            w-8
            h-8
            rounded-full
            bg-[#2B2E72]
            flex
            items-center
            justify-center
            flex-shrink-0
            shadow-sm
        ">
            <img
                src="/assets/images/logo.png"
                class="w-5"
                alt="AI"
            >
        </div>


        <!-- AI Message -->
        <div class="
            bg-white
            border
            border-gray-200
            text-gray-800
            rounded-2xl
            rounded-tl-md
            px-4
            py-3
            max-w-[calc(100%-3rem)]
            md:max-w-[80%]
            shadow-sm
            ai-content
            overflow-x-auto
            min-w-0
        ">
            ${formattedMessage}
        </div>

    `;


    chatBox.appendChild(msgDiv);


    // =====================================================
    // TABLO SONRASI SCROLL
    // =====================================================

    scrollToBottom();

    /*
     * Markdown tablo oluşturulduktan sonra
     * browser'ın gerçek yüksekliği hesaplamasını bekle.
     *
     * Böylece özellikle uzun cevaplarda
     * scroll kesin olarak en alta gider.
     */

    requestAnimationFrame(() => {

        requestAnimationFrame(() => {

            scrollToBottom();

        });

    });
}


// =========================================================
// LOADING ANIMATION
// =========================================================

export function showLoadingAnimation(id) {

    const loadingDiv =
        document.createElement('div');

    loadingDiv.id = id;

    loadingDiv.className =
        "flex justify-start gap-3 md:gap-4 chat-message";


    loadingDiv.innerHTML = `

        <!-- AI Avatar -->
        <div class="
            w-8
            h-8
            rounded-full
            bg-[#2B2E72]
            flex
            items-center
            justify-center
            flex-shrink-0
            shadow-sm
        ">
            <img
                src="/assets/images/logo.png"
                class="w-5 opacity-60"
                alt="AI"
            >
        </div>


        <!-- Loading -->
        <div class="
            bg-white
            border
            border-gray-200
            text-gray-600
            rounded-2xl
            rounded-tl-md
            px-4
            py-3
            shadow-sm
            flex
            items-center
            gap-3
        ">

            <div class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
            </div>

            <span class="loading-status">
                Ürün araştırılıyor...
            </span>

        </div>

    `;


    chatBox.appendChild(loadingDiv);

    scrollToBottom();


    // =====================================================
    // DURUM MESAJLARI
    // =====================================================

    const statusMessages = [

        "Ürün araştırılıyor...",

        "Amazon taranıyor...",

        "Hepsiburada kontrol ediliyor...",

        "Trendyol kontrol ediliyor...",

        "PttAVM kontrol ediliyor...",

        "Fiyatlar karşılaştırılıyor...",

        "En uygun fiyat belirleniyor..."

    ];


    let currentIndex = 0;


    const statusElement =
        loadingDiv.querySelector(
            ".loading-status"
        );


    // =====================================================
    // STATUS INTERVAL
    // =====================================================

    const statusInterval =
        setInterval(() => {

            if (
                !document.body.contains(loadingDiv)
            ) {

                clearInterval(statusInterval);

                return;
            }


            currentIndex =
                (currentIndex + 1)
                % statusMessages.length;


            if (statusElement) {

                statusElement.style.opacity = "0";


                setTimeout(() => {

                    if (
                        document.body.contains(
                            loadingDiv
                        )
                    ) {

                        statusElement.textContent =
                            statusMessages[currentIndex];

                        statusElement.style.opacity =
                            "1";
                    }

                }, 200);

            }

        }, 1800);


    /*
     * Interval'i element üzerinde saklıyoruz.
     */

    loadingDiv._statusInterval =
        statusInterval;
}


// =========================================================
// REMOVE LOADING
// =========================================================

export function removeLoadingAnimation(id) {

    const loadingDiv =
        document.getElementById(id);


    if (loadingDiv) {

        if (loadingDiv._statusInterval) {

            clearInterval(
                loadingDiv._statusInterval
            );
        }


        loadingDiv.remove();

        scrollToBottom();
    }
}


// =========================================================
// ERROR
// =========================================================

export function appendErrorMessage() {

    const msgDiv =
        document.createElement('div');

    msgDiv.className =
        "flex justify-start gap-3 md:gap-4 chat-message";


    msgDiv.innerHTML = `

        <div class="
            w-8
            h-8
            rounded-full
            bg-red-50
            text-[#E30613]
            flex
            items-center
            justify-center
            flex-shrink-0
        ">
            <i class="fa-solid fa-triangle-exclamation"></i>
        </div>


        <div class="
            bg-red-50
            text-red-600
            rounded-2xl
            rounded-tl-md
            px-4
            py-3
            max-w-[80%]
            border
            border-red-100
            shadow-sm
        ">
            Bir hata oluştu.
            Lütfen backend bağlantınızı kontrol edin.
        </div>

    `;


    chatBox.appendChild(msgDiv);

    scrollToBottom();
}


// =========================================================
// SCROLL
// =========================================================

function scrollToBottom() {

    if (!chatBox) {
        return;
    }


    /*
     * Önce doğrudan scrollTop ile en alta git.
     *
     * scrollTo + smooth bazı durumlarda
     * uzun Markdown içeriklerinde hedef yüksekliği
     * yanlış hesaplayabiliyor.
     */

    chatBox.scrollTop =
        chatBox.scrollHeight;


    /*
     * Browser DOM'u güncelledikten sonra
     * tekrar en alta git.
     */

    requestAnimationFrame(() => {

        chatBox.scrollTop =
            chatBox.scrollHeight;

    });

}


// =========================================================
// HTML ESCAPE
// =========================================================

function escapeHtml(text) {

    const div =
        document.createElement('div');

    div.textContent =
        text;

    return div.innerHTML;
}