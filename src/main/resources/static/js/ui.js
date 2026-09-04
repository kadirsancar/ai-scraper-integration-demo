
const chatBox =
    document.getElementById("chatBox");

const welcomeScreen =
    document.getElementById("welcomeScreen");


// =========================================================
// WELCOME SCREEN
// =========================================================

export function hideWelcomeScreen() {

    if (welcomeScreen) {
        welcomeScreen.style.display = "none";
    }

}


// =========================================================
// USER MESSAGE
// =========================================================

export function appendUserMessage(message) {

    if (!chatBox) {
        return;
    }

    const msgDiv =
        document.createElement("div");

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

    if (!chatBox) {
        return;
    }

    const msgDiv =
        document.createElement("div");

    msgDiv.className =
        "flex justify-start chat-message w-full";


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
<div class="
ai-message-wrapper
max-w-[80%]
md:max-w-[80%]
min-w-0
">

<!-- AI MESSAGE -->

<div class="
bg-white
border
border-gray-200
text-gray-800
rounded-2xl
rounded-tl-md
px-4
py-3
shadow-sm
ai-content
overflow-x-auto
min-w-0
">
${formattedMessage}
</div>


<!-- ACTION BUTTONS -->

<div class="
                ai-actions
                flex
                items-center
                gap-2
                mt-2
            ">

    <!-- COPY -->

    <button
        type="button"
        class="ai-action-button copy-button"
        title="Cevabı kopyala"
    >
        <i class="fa-regular fa-copy"></i>
        <span>Kopyala</span>
    </button>


    <!-- PDF -->

    <button
        type="button"
        class="ai-action-button pdf-button"
        title="PDF olarak indir"
    >
        <i class="fa-regular fa-file-pdf"></i>
        <span>PDF</span>
    </button>


    <!-- EXCEL -->

    <button
        type="button"
        class="ai-action-button excel-button"
        title="Excel olarak indir"
    >
        <i class="fa-regular fa-file-excel"></i>
        <span>Excel</span>
    </button>

</div>

</div>
`;


    chatBox.appendChild(msgDiv);


    // =====================================================
    // MARKDOWN LINKLERİ
    // =====================================================

    activateMarkdownLinks(msgDiv);


    // =====================================================
    // ACTION BUTTONLARI
    // =====================================================

    setupAIMessageActions(
        msgDiv,
        message
    );


    // =====================================================
    // SCROLL
    // =====================================================

    scrollToBottom();

    requestAnimationFrame(() => {

        requestAnimationFrame(() => {

            scrollToBottom();

        });

    });

}


// =========================================================
// MARKDOWN LINKLERİNİ AKTİFLEŞTİR
// =========================================================

function activateMarkdownLinks(messageElement) {

    const links =
        messageElement.querySelectorAll(
            ".ai-content a"
        );


    links.forEach((link) => {

        const href =
            link.getAttribute("href");


        // -------------------------------------------------
        // Geçerli URL kontrolü
        // -------------------------------------------------

        if (
            !href ||
            href === "#" ||
            href.trim() === ""
        ) {

            link.removeAttribute("href");

            return;
        }


        // -------------------------------------------------
        // Sadece http / https bağlantılarına izin ver
        // -------------------------------------------------

        if (
            !href.startsWith("http://") &&
            !href.startsWith("https://")
        ) {

            link.removeAttribute("href");

            return;
        }


        // -------------------------------------------------
        // Yeni sekmede aç
        // -------------------------------------------------

        link.setAttribute(
            "target",
            "_blank"
        );


        // -------------------------------------------------
        // Güvenlik
        // -------------------------------------------------

        link.setAttribute(
            "rel",
            "noopener noreferrer"
        );


        // -------------------------------------------------
        // CSS class
        // -------------------------------------------------

        link.classList.add(
            "ai-product-link"
        );

    });

}


// =========================================================
// AI ACTION BUTTONS
// =========================================================

function setupAIMessageActions(
    messageElement,
    rawMessage
) {


    // =====================================================
    // COPY
    // =====================================================

    const copyButton =
        messageElement.querySelector(
            ".copy-button"
        );


    if (copyButton) {

        copyButton.addEventListener(
            "click",
            async () => {

                try {

                    await navigator.clipboard.writeText(
                        rawMessage
                    );


                    const originalHtml =
                        copyButton.innerHTML;


                    copyButton.innerHTML = `
<i class="fa-solid fa-check"></i>
<span>Kopyalandı</span>
    `;


                    copyButton.classList.add(
                        "success"
                    );


                    setTimeout(() => {

                        copyButton.innerHTML =
                            originalHtml;

                        copyButton.classList.remove(
                            "success"
                        );

                    }, 1500);


                } catch (error) {

                    console.error(
                        "Kopyalama hatası:",
                        error
                    );

                    alert(
                        "Cevap kopyalanamadı."
                    );

                }

            }
        );

    }


    // =====================================================
    // PDF
    // =====================================================

    const pdfButton =
        messageElement.querySelector(
            ".pdf-button"
        );


    if (pdfButton) {

        pdfButton.addEventListener(
            "click",
            () => {

                exportMessageToPDF(
                    messageElement
                );

            }
        );

    }


    // =====================================================
    // EXCEL
    // =====================================================

    const excelButton =
        messageElement.querySelector(
            ".excel-button"
        );


    if (excelButton) {

        excelButton.addEventListener(
            "click",
            () => {

                exportMessageToExcel(
                    messageElement
                );

            }
        );

    }

}


// =========================================================
// PDF EXPORT
// =========================================================

function exportMessageToPDF(
    messageElement
) {

    if (
        typeof html2pdf === "undefined"
    ) {

        alert(
            "PDF kütüphanesi yüklenemedi."
        );

        return;
    }


    const content =
        messageElement.querySelector(
            ".ai-content"
        );


    if (!content) {
        return;
    }


    // =====================================================
    // PDF CLONE
    // =====================================================

    const clone =
        content.cloneNode(true);


    clone.style.maxWidth =
        "100%";

    clone.style.width =
        "100%";

    clone.style.background =
        "#ffffff";

    clone.style.color =
        "#000000";

    clone.style.padding =
        "20px";

    clone.style.border =
        "none";

    clone.style.borderRadius =
        "0";


    const wrapper =
        document.createElement("div");


    wrapper.style.background =
        "#ffffff";

    wrapper.style.padding =
        "10px";

    wrapper.style.width =
        "100%";


    wrapper.appendChild(clone);


    // =====================================================
    // PDF OPTIONS
    // =====================================================

    const options = {

        margin: 10,

        filename:
            `aipage-cevap-${Date.now()}.pdf`,

        image: {

            type: "jpeg",

            quality: 0.98

        },

        html2canvas: {

            scale: 2,

            useCORS: true

        },

        jsPDF: {

            unit: "mm",

            format: "a4",

            orientation: "portrait"

        }

    };


    html2pdf()
        .set(options)
        .from(wrapper)
        .save();

}


// =========================================================
// EXCEL EXPORT
// =========================================================

function exportMessageToExcel(
    messageElement
) {

    if (
        typeof XLSX === "undefined"
    ) {

        alert(
            "Excel kütüphanesi yüklenemedi."
        );

        return;
    }


    const table =
        messageElement.querySelector(
            ".ai-content table"
        );


    if (!table) {

        alert(
            "Bu cevapta Excel'e aktarılabilecek bir tablo bulunamadı."
        );

        return;
    }


    try {

        const worksheet =
            XLSX.utils.table_to_sheet(
                table
            );


        // =================================================
        // SÜTUN GENİŞLİKLERİ
        // =================================================

        worksheet["!cols"] = [

            { wch: 20 },

            { wch: 55 },

            { wch: 20 },

            { wch: 20 },

            { wch: 35 }

        ];


        // =================================================
        // WORKBOOK
        // =================================================

        const workbook =
            XLSX.utils.book_new();


        XLSX.utils.book_append_sheet(
            workbook,
            worksheet,
            "Fiyat Karşılaştırma"
        );


        // =================================================
        // DOSYA
        // =================================================

        XLSX.writeFile(
            workbook,
            `aipage-fiyat-karsilastirma-${Date.now()}.xlsx`
        );


    } catch (error) {

        console.error(
            "Excel oluşturma hatası:",
            error
        );

        alert(
            "Excel dosyası oluşturulamadı."
        );

    }

}


// =========================================================
// LOADING ANIMATION
// =========================================================

export function showLoadingAnimation(id) {

    if (!chatBox) {
        return;
    }


    const loadingDiv =
        document.createElement("div");


    loadingDiv.id =
        id;


    loadingDiv.className =
        "flex justify-start chat-message";


    loadingDiv.innerHTML = `

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
loading-message
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


    chatBox.appendChild(
        loadingDiv
    );


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


    let currentIndex =
        0;


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
                !document.body.contains(
                    loadingDiv
                )
            ) {

                clearInterval(
                    statusInterval
                );

                return;
            }


            currentIndex =
                (
                    currentIndex + 1
                ) %
                statusMessages.length;


            if (statusElement) {

                statusElement.style.opacity =
                    "0";


                setTimeout(() => {

                    if (
                        document.body.contains(
                            loadingDiv
                        )
                    ) {

                        statusElement.textContent =
                            statusMessages[
                                currentIndex
                            ];


                        statusElement.style.opacity =
                            "1";

                    }

                }, 200);

            }

        }, 1800);


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

        if (
            loadingDiv._statusInterval
        ) {

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

    if (!chatBox) {
        return;
    }


    const msgDiv =
        document.createElement("div");


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

<i class="
fa-solid
fa-triangle-exclamation
"></i>

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


    chatBox.appendChild(
        msgDiv
    );


    scrollToBottom();

}


// =========================================================
// SCROLL
// =========================================================

function scrollToBottom() {

    if (!chatBox) {
        return;
    }


    chatBox.scrollTop =
        chatBox.scrollHeight;


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
        document.createElement("div");


    div.textContent =
        text;


    return div.innerHTML;

}

