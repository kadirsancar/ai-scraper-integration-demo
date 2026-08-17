// Backend ile iletişim katmanı
const API_BASE_URL = "http://localhost:8081/api/chat";

async function sendChatMessage(message) {
    const response = await fetch(`${API_BASE_URL}/ask`, {
        method: "POST",
        headers: { "Content-Type": "text/plain",
            "X-API-KEY":"KORU-SECRET-KEY-2026"
        },
        body: message
    });

    if (!response.ok) {
        throw new Error("Sunucu hata döndü. Durum kodu:  " + response.status);
    }

    return await response.text();
}

export { sendChatMessage }; // Diğer dosyalardan erişilebilmesi için dışa aktarıyoruz