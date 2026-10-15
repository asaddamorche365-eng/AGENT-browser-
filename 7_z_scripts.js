// ===== Z. scripts: ekhane tomar extension-er code paste koro =====
// Ei file X tab-er proti page load-e auto inject hoy.
// Helpers:
//   ZBridge.http(method, url, headersJson, body) -> string  (CORS charai API call)
//   ZBridge.go(url)  -> X tab-e URL load (search er jonno)
(function () {
  if (window.__zLoaded) return;
  window.__zLoaded = true;

  window.Z = {
    sleep: (ms) => new Promise((r) => setTimeout(r, ms)),

    search(q) {
      ZBridge.go("https://x.com/search?q=" + encodeURIComponent(q) + "&f=live");
    },

    async like() {
      const b = document.querySelector('[data-testid="like"]');
      if (b) b.click();
    },

    async comment(text) {
      const r = document.querySelector('[data-testid="reply"]');
      if (r) r.click();
      await Z.sleep(1000);
      const box = document.querySelector('[data-testid="tweetTextarea_0"]');
      if (!box) return;
      box.focus();
      document.execCommand("insertText", false, text);
      await Z.sleep(500);
      const send = document.querySelector('[data-testid="tweetButton"]');
      if (send) send.click();
    },

    // Reply generate: tomar API url/key boshao
    async generateReply(postText) {
      const res = ZBridge.http(
        "POST",
        "https://YOUR_API_URL",
        JSON.stringify({ "Content-Type": "application/json", Authorization: "Bearer YOUR_KEY" }),
        JSON.stringify({ prompt: postText })
      );
      return res;
    },
  };

  console.log("Z. loaded");
})();
